package com.genius.srss.ui.folder

import android.text.Editable
import com.genius.srss.ui.subscriptions.BaseSubscriptionModel

interface FolderViewModelDelegate {
    fun checkSaveAvailability(newFolderName: Editable?)
    fun updateFolder(newFolderName: String?)
    fun changeMode()
    fun deleteFolder()
    fun changeEditMode(isEdit: Boolean)
    fun unlinkFolderByPosition(position: Int)
}

data class FolderStateModel(
    val folderId: String,
    val feedList: List<BaseSubscriptionModel> = listOf(),
    val title: String? = null,
    val isInEditMode: Boolean = false,
    val isAvailableToSave: Boolean = false,
    val isCombinedMode: Boolean = false,
    val isInFeedLoadingProgress: Boolean = false,
)