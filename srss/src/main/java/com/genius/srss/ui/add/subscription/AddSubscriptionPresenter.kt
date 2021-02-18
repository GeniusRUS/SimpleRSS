package com.genius.srss.ui.add.subscription

import com.genius.srss.R
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionDatabaseModel
import com.genius.srss.di.services.network.INetworkSource
import com.ub.utils.LogUtils
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import org.xmlpull.v1.XmlPullParserException
import java.lang.IllegalArgumentException
import java.net.UnknownServiceException
import java.util.zip.DataFormatException
import javax.inject.Inject
import kotlin.properties.Delegates

class AddSubscriptionPresenter @Inject constructor(
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao
) : MvpPresenter<AddSubscriptionView>() {

    private var state: AddSubscriptionStateModel by Delegates.observable(AddSubscriptionStateModel()) { _, _, _ ->

    }

    fun checkOrSave(sourceUrl: String) {
        if (!state.sourceUrl.isNullOrEmpty()) {
            saveSource()
        } else {
            checkSource(sourceUrl)
        }
    }

    private fun checkSource(sourceUrl: String) {
        presenterScope.launch {
            try {
                val feed = networkSource.loadFeed(sourceUrl)
                val subscriptions = subscriptionsDao.loadSubscriptions()
                state = state.copy(
                    sourceUrl = sourceUrl,
                    title = feed?.title,
                    timeOfAdd = System.currentTimeMillis()
                )
                viewState.onAvailableToSave(
                    isAvailableToSave = subscriptions.firstOrNull { it.urlToLoad == feed?.link } == null
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                when (e) {
                    is XmlPullParserException -> viewState.showErrorMessage(R.string.error_data_format_exception)
                    is DataFormatException -> viewState.showErrorMessage(R.string.error_data_format_exception)
                    is IllegalArgumentException -> viewState.showErrorMessage(R.string.error_illegal_argument_url)
                    is UnknownServiceException -> viewState.showErrorMessage(R.string.error_http_insecure_format)
                }
            }
        }
    }

    private fun saveSource() {
        presenterScope.launch {
            try {
                if (state.sourceUrl != null && state.title != null && state.timeOfAdd != null) {
                    subscriptionsDao.saveSubscription(
                        SubscriptionDatabaseModel(
                            state.sourceUrl!!, state.title!!, state.timeOfAdd!!
                        )
                    )
                    viewState.onSourceAdded(state.sourceUrl!!)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "AddSubscriptionPresenter"
    }
}