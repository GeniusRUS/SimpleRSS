package com.genius.srss.ui.feed

import com.genius.srss.ui.subscriptions.BaseSubscriptionModel

data class FeedStateModel(
    val feedContent: List<BaseSubscriptionModel> = listOf(),
    val title: String? = null,
    val isAvailableToSave: Boolean = false
)