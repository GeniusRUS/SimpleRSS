package com.genius.srss.ui.add.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.genius.srss.R
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionFolderDatabaseModel
import com.genius.srss.di.services.generator.IGenerator
import com.ub.utils.LogUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddFolderModelFactory @Inject constructor(
    private val subscriptionsDao: SubscriptionsDao,
    private val generator: IGenerator
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddFolderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddFolderViewModel(
                subscriptionsDao,
                generator
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AddFolderViewModel(
    private val subscriptionsDao: SubscriptionsDao,
    private val generator: IGenerator
) : ViewModel() {

    private val _errorFlow: MutableSharedFlow<Int> = MutableSharedFlow()
    private val _folderCreatedFlow: MutableSharedFlow<Unit> = MutableSharedFlow()

    val errorFlow: SharedFlow<Int> = _errorFlow.apply {
        viewModelScope.launch {
            this@apply.collect {
                println("Error is $it")
            }
        }
    }
    val folderCreatedFlow: SharedFlow<Unit> = _folderCreatedFlow

    fun saveFolder(folderName: String) {
        viewModelScope.launch {
            try {
                if (folderName.isEmpty()) {
                    _errorFlow.emit(R.string.add_new_subscription_folder_must_be_not_empty)
                    return@launch
                }
                val lastIndex = subscriptionsDao.getLastFolderSortIndex() ?: 0
                subscriptionsDao.saveFolder(
                    SubscriptionFolderDatabaseModel(
                        generator.generateRandomId(),
                        lastIndex + 1,
                        folderName,
                        System.currentTimeMillis()
                    )
                )
                _folderCreatedFlow.emit(Unit)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "AddFolderPresenter"
    }
}