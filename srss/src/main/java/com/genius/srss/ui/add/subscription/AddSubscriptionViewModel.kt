package com.genius.srss.ui.add.subscription

import android.database.sqlite.SQLiteConstraintException
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParserException
import java.util.zip.DataFormatException

@AssistedFactory
interface AddSubscriptionViewModelProvider {
    fun create(folderId: String?): AddSubscriptionViewModelFactory
}

class AddSubscriptionViewModelFactory @AssistedInject constructor(
    @Assisted private val folderId: String?,
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
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

    private val _errorFlow: MutableSharedFlow<Int> = MutableSharedFlow()
    private val _loadingSourceInfoFlow: MutableStateFlow<LoadingSourceInfoState> = MutableStateFlow(LoadingSourceInfoState())
    private val _sourceAddedFlow: MutableSharedFlow<String> = MutableSharedFlow()

    val errorFlow: SharedFlow<Int> = _errorFlow
    val loadingSourceInfoFlow: StateFlow<LoadingSourceInfoState> = _loadingSourceInfoFlow
    val sourceAddedFlow: SharedFlow<String> = _sourceAddedFlow

    private var state: AddSubscriptionStateModel = AddSubscriptionStateModel()

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
                _loadingSourceInfoFlow.value = _loadingSourceInfoFlow.value.copy(
                    isLoading = true
                )
                val feed = networkSource.loadFeed(sourceUrl)
                val subscriptions = subscriptionsDao.loadSubscriptions()
                state = state.copy(
                    sourceUrl = sourceUrl,
                    title = feed?.title,
                    timeOfAdd = System.currentTimeMillis()
                )
                _loadingSourceInfoFlow.value = _loadingSourceInfoFlow.value.copy(
                    isLoading = false,
                    isAvailableToSave = subscriptions.firstOrNull { it.urlToLoad == feed?.link } == null
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                _loadingSourceInfoFlow.value = _loadingSourceInfoFlow.value.copy(
                    isLoading = false
                )
                when (e) {
                    is XmlPullParserException -> _errorFlow.emit(R.string.error_data_format_exception)
                    is DataFormatException -> _errorFlow.emit(R.string.error_data_format_exception)
                    is IllegalArgumentException -> _errorFlow.emit(R.string.error_illegal_argument_url)
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
                    _sourceAddedFlow.emit(state.sourceUrl!!)
                }
            } catch (linkAlreadyExistedException: SQLiteConstraintException)  {
                _errorFlow.emit(R.string.error_link_to_folder_already_exist)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "AddSubscriptionPresenter"
    }
}