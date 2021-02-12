package com.genius.srss.ui.subscriptions

import com.ub.utils.base.DiffComparable

data class SubscriptionsStateModel(
    val feedList: List<SubscriptionItemModel> = listOf()
)

data class SubscriptionItemModel(
    val title: String?,
    val urlToLoad: String?
) : DiffComparable {
    override fun getItemId(): Int = urlToLoad.hashCode()
}