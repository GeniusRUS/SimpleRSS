package com.genius.srss.ui.folder

import com.genius.srss.ui.subscriptions.BaseSubscriptionModel

data class FolderStateModel(
    val feedList: List<BaseSubscriptionModel> = listOf(),
    val title: String? = null
)