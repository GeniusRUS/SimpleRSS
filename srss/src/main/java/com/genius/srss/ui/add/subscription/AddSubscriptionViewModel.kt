package com.genius.srss.ui.add.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.genius.srss.R
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionDatabaseModel
import com.genius.srss.di.services.database.models.SubscriptionFolderCrossRefDatabaseModel
import com.genius.srss.di.services.network.INetworkSource
import com.ub.utils.LogUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParserException
import java.lang.IllegalArgumentException
import java.net.UnknownServiceException
import java.util.zip.DataFormatException
import kotlin.properties.Delegates

@AssistedFactory
interface AddSubscriptionViewModelProvider {
    fun create(folderId: String?): AddSubscriptionViewModelFactory
}

class AddSubscriptionViewModelFactory @AssistedInject constructor(
    @Assisted private val folderId: String?,
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddSubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddSubscriptionViewModel(
                folderId,
                networkSource,
                subscriptionsDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AddSubscriptionViewModel @AssistedInject constructor(
    @Assisted private val folderId: String?,
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao
) : ViewModel() {

    private val innerErrorFlow: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val innerLoadingSourceInfoFlow: MutableStateFlow<LoadingSourceInfoState> = MutableStateFlow(LoadingSourceInfoState())
    private val innerSourceAddedFlow: MutableStateFlow<String?> = MutableStateFlow(null)

    val errorFlow: StateFlow<Int?> = innerErrorFlow
    val loadingSourceInfoFlow: StateFlow<LoadingSourceInfoState> = innerLoadingSourceInfoFlow
    val sourceAddedFlow: StateFlow<String?> = innerSourceAddedFlow

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
        viewModelScope.launch {
            try {
                innerLoadingSourceInfoFlow.value = innerLoadingSourceInfoFlow.value.copy(
                    isLoading = true
                )
                val feed = networkSource.loadFeed(sourceUrl)
                val subscriptions = subscriptionsDao.loadSubscriptions()
                state = state.copy(
                    sourceUrl = sourceUrl,
                    title = feed?.title,
                    timeOfAdd = System.currentTimeMillis()
                )
                innerLoadingSourceInfoFlow.value = innerLoadingSourceInfoFlow.value.copy(
                    isLoading = false,
                    isAvailableToSave = subscriptions.firstOrNull { it.urlToLoad == feed?.link } == null
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                innerLoadingSourceInfoFlow.value = innerLoadingSourceInfoFlow.value.copy(
                    isLoading = false
                )
                when (e) {
                    is XmlPullParserException -> innerErrorFlow.value = R.string.error_data_format_exception
                    is DataFormatException -> innerErrorFlow.value = R.string.error_data_format_exception
                    is IllegalArgumentException -> innerErrorFlow.value = R.string.error_illegal_argument_url
                    is UnknownServiceException -> innerErrorFlow.value = R.string.error_http_insecure_format
                }
            }
        }
    }

    private fun saveSource() {
        viewModelScope.launch {
            try {
                if (state.sourceUrl != null && state.title != null && state.timeOfAdd != null) {
                    subscriptionsDao.saveSubscription(
                        SubscriptionDatabaseModel(
                            state.sourceUrl!!, state.title!!, state.timeOfAdd!!
                        )
                    )
                    folderId?.let { folderIdToLink ->
                        subscriptionsDao.saveSubscriptionFolderCrossRef(
                            SubscriptionFolderCrossRefDatabaseModel(
                                state.sourceUrl!!,
                                folderIdToLink
                            )
                        )
                    }
                    innerSourceAddedFlow.value = state.sourceUrl
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