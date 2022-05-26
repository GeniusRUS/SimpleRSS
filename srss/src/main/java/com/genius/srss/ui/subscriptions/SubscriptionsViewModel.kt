package com.genius.srss.ui.subscriptions

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.genius.srss.R
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionFolderCrossRefDatabaseModel
import com.genius.srss.util.TutorialView.Companion.IS_TUTORIAL_SHOW
import com.ub.utils.LogUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class SubscriptionsViewModelFactory @Inject constructor(
    private val context: Context,
    private val subscriptionDao: SubscriptionsDao,
    private val dataStore: DataStore<Preferences>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionsViewModel(
                context,
                subscriptionDao,
                dataStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@SuppressLint("StaticFieldLeak")
class SubscriptionsViewModel(
    private val context: Context,
    private val subscriptionDao: SubscriptionsDao,
    private val dataStore: DataStore<Preferences>
) : ViewModel(), SubscriptionViewModelDelegate {

    private val _errorFlow: MutableSharedFlow<String> = MutableSharedFlow()
    private val _state: MutableStateFlow<SubscriptionsStateModel> =
        MutableStateFlow(SubscriptionsStateModel())
    private val _tutorialState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _sortingState: MutableStateFlow<Pair<Int, Int>> = MutableStateFlow(Pair(-1, -1))

    private val _isFullListState: MutableStateFlow<Boolean> = MutableStateFlow(_state.value.isFullList)

    val errorFlow: SharedFlow<String> = _errorFlow
    val state: StateFlow<SubscriptionsStateModel> = _state
    val tutorialState: StateFlow<Boolean> = _tutorialState

    init {
        viewModelScope.launch {
            val subscriptions = combine(
                _isFullListState,
                subscriptionDao.loadSubscriptions(),
                subscriptionDao.loadSubscriptionsWithoutFolders()
            ) { isFullList, allSubscriptions, subscriptionsWithoutFolders ->
                if (isFullList) {
                    allSubscriptions
                } else {
                    subscriptionsWithoutFolders
                }
            }
            combine(
                subscriptionDao.loadAllFoldersWithAutoSortingIfNeeded(),
                subscriptions
            ) { folders, feeds ->
                folders.map {
                    SubscriptionFolderItemModel(
                        it.id,
                        it.name,
                        subscriptionDao.getCrossRefCountByFolderId(it.id)
                    )
                } + feeds.map {
                    SubscriptionItemModel(
                        it.title,
                        it.urlToLoad
                    )
                }
            }.collect { models ->
                _state.update { state ->
                    state.copy(
                        isFullList = _state.value.isFullList,
                        feedList = models.ifEmpty {
                            listOf(
                                SubscriptionFolderEmptyModel(
                                    icon = R.drawable.ic_vector_empty_folder,
                                    message = context.getString(R.string.subscription_empty),
                                    actionText = context.getString(R.string.subscription_empty_first)
                                )
                            )
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            _sortingState.collect { pair ->
                val fromPosition = pair.first
                val toPosition = pair.second
                subscriptionDao.changeFolderSort(fromPosition, toPosition)
            }
        }
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                preferences[booleanPreferencesKey(IS_TUTORIAL_SHOW)] ?: true
            }.collect { isTutorialShow ->
                _tutorialState.emit(isTutorialShow)
                _state.update { state ->
                    state.copy(
                        isTutorialShow = isTutorialShow
                    )
                }
            }
        }
    }

    override fun updateFeed(isFull: Boolean?) {
        val isFullNotNull = isFull ?: _state.value.isFullList
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    isFullList = isFullNotNull,
                )
            }
            _isFullListState.emit(isFullNotNull)
        }
    }

    override fun removeSubscriptionByPosition(position: Int) {
        viewModelScope.launch {
            try {
                (_state.value.feedList[position] as? SubscriptionItemModel)?.let { subscription ->
                    subscription.urlToLoad?.let { urlToRemove ->
                        subscriptionDao.complexRemoveSubscriptionByUrl(urlToRemove)
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    override fun onEndTutorial() {
        viewModelScope.launch {
            try {
                val tutorialNewState = dataStore.updateData { preferences ->
                    preferences.toMutablePreferences().apply {
                        set(booleanPreferencesKey(IS_TUTORIAL_SHOW), false)
                    }
                }[booleanPreferencesKey(IS_TUTORIAL_SHOW)] ?: _state.value.isTutorialShow
                _tutorialState.emit(tutorialNewState)
                _state.update { state ->
                    state.copy(
                        isTutorialShow = tutorialNewState
                    )
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    override fun handleHolderMove(url: String, folderId: String) {
        viewModelScope.launch {
            try {
                subscriptionDao.saveSubscriptionFolderCrossRef(
                    SubscriptionFolderCrossRefDatabaseModel(
                        url,
                        folderId
                    )
                )
            } catch (linkAlreadyExistedException: SQLiteConstraintException) {
                _errorFlow.emit(context.getString(R.string.error_link_to_folder_already_exist))
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun handleFolderSortingChange(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            try {
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) return@launch
                if (_state.value.feedList[fromPosition] !is SubscriptionFolderItemModel) return@launch
                if (_state.value.feedList[toPosition] !is SubscriptionFolderItemModel) return@launch
                _sortingState.emit(Pair(fromPosition, toPosition))
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "SubscriptionsPresenter"
    }
}