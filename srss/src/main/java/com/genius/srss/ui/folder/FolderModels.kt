package com.genius.srss.ui.folder

import com.genius.srss.ui.subscriptions.BaseSubscriptionModel

data class FolderStateModel(
    val folderId: String,
    val feedList: List<BaseSubscriptionModel> = listOf(),
    val title: String? = null,
    val isInEditMode: Boolean = false,
    val isAvailableToSave: Boolean = false
)