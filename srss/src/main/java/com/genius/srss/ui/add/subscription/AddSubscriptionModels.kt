package com.genius.srss.ui.add.subscription

data class AddSubscriptionStateModel(
    val sourceUrl: String? = null,
    val title: String? = null,
    val timeOfAdd: Long? = null
)

data class LoadingSourceInfoState(
    val isLoading : Boolean = false,
    val isAvailableToSave: Boolean = false
)