package com.genius.srss.ui.subscriptions

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
    private val subscriptionDao: SubscriptionsDao,
    private val dataStore: DataStore<Preferences>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionsViewModel(
                subscriptionDao,
                dataStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SubscriptionsViewModel(
    private val subscriptionDao: SubscriptionsDao,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _errorFlow: MutableSharedFlow<Int> = MutableSharedFlow()
    private val _state: MutableStateFlow<SubscriptionsStateModel> =
        MutableStateFlow(SubscriptionsStateModel())
    private val _tutorialState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _sortingState: MutableStateFlow<Pair<Int, Int>> = MutableStateFlow(Pair(-1, -1))

    private val _updateTriggerState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val errorFlow: SharedFlow<Int> = _errorFlow
    val state: StateFlow<SubscriptionsStateModel> = _state
    val tutorialState: StateFlow<Boolean> = _tutorialState

    init {
        viewModelScope.launch {
            combine(
                subscriptionDao.loadAllFoldersWithAutoSortingIfNeeded(),
                subscriptionDao.loadSubscriptions(),
                _updateTriggerState
            ) { folders, feeds, trigger ->
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
                        isFullList = _updateTriggerState.value,
                        feedList = models.ifEmpty {
                            listOf(
                                SubscriptionFolderEmptyModel(
                                    icon = R.drawable.ic_vector_empty_folder,
                                    message = R.string.subscription_empty,
                                    action = R.string.subscription_empty_first
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

    fun init() {
        try {
            viewModelScope.launch {
                _sortingState.collect { pair ->
                    val fromPosition = pair.first
                    val toPosition = pair.second
                    subscriptionDao.changeFolderSort(fromPosition, toPosition)
                    updateFeed()
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, e.message, e)
        }
    }

    fun updateFeed(isFull: Boolean = _state.value.isFullList) {
        viewModelScope.launch {
            try {
                val folders = subscriptionDao.loadAllFoldersWithAutoSortingIfNeeded()
                val subscriptions = if (isFull) {
                    subscriptionDao.loadSubscriptions()
                } else {
                    subscriptionDao.loadSubscriptionsWithoutFolders()
                }
                _state.update { state ->
                    state.copy(
                        isFullList = isFull,
                        /*feedList = merge(
                            folders.map {
                                SubscriptionFolderItemModel(
                                    it.id,
                                    it.name,
                                    subscriptionDao.getCrossRefCountByFolderId(it.id)
                                )
                            },
                            subscriptions.map {
                                SubscriptionItemModel(
                                    it.title,
                                    it.urlToLoad
                                )
                            }
                        ).toList().ifEmpty {
                            listOf(
                                SubscriptionFolderEmptyModel(
                                    icon = R.drawable.ic_vector_empty_folder,
                                    message = R.string.subscription_empty,
                                    action = R.string.subscription_empty_first
                                )
                            )
                        }*/
                    )
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun removeSubscriptionByPosition(position: Int) {
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

    fun skipTutorial() {
        viewModelScope.launch {
            try {
                dataStore.updateData { preferences ->
                    preferences.toMutablePreferences().apply {
                        set(booleanPreferencesKey(IS_TUTORIAL_SHOW), false)
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun handleHolderMove(holderPosition: Int, targetPosition: Int) {
        viewModelScope.launch {
            try {
                if (holderPosition == RecyclerView.NO_POSITION || targetPosition == RecyclerView.NO_POSITION) return@launch
                val holderToMove =
                    _state.value.feedList[holderPosition] as? SubscriptionItemModel ?: return@launch
                val targetOfMove =
                    _state.value.feedList[targetPosition] as? SubscriptionFolderItemModel
                        ?: return@launch
                subscriptionDao.saveSubscriptionFolderCrossRef(
                    SubscriptionFolderCrossRefDatabaseModel(
                        holderToMove.urlToLoad ?: return@launch,
                        targetOfMove.id
                    )
                )
            } catch (linkAlreadyExistedException: SQLiteConstraintException) {
                _errorFlow.emit(R.string.error_link_to_folder_already_exist)
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