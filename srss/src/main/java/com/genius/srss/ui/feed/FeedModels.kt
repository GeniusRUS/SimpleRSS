package com.genius.srss.ui.feed

import com.genius.srss.ui.subscriptions.BaseSubscriptionModel

data class FeedStateModel(
    val isRefreshing: Boolean = false,
    val feedContent: List<BaseSubscriptionModel> = listOf(),
    val title: String? = null,
    val isInEditMode: Boolean = false,
    val isAvailableToSave: Boolean = false
)