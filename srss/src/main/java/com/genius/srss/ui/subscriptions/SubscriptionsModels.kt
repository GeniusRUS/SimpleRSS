package com.genius.srss.ui.subscriptions

import androidx.annotation.LayoutRes
import com.genius.srss.R
import com.ub.utils.base.DiffComparable

data class SubscriptionsStateModel(
    val feedList: List<BaseSubscriptionModel> = listOf()
)

sealed class BaseSubscriptionModel : DiffComparable {
    @LayoutRes
    abstract fun getLayoutId(): Int
}

data class SubscriptionItemModel(
    val title: String?,
    val urlToLoad: String?
) : BaseSubscriptionModel() {
    override fun getLayoutId(): Int = R.layout.rv_subscription_item
    override fun getItemId(): Int = urlToLoad.hashCode()
}

data class SubscriptionFolderItemModel(
    val id: String,
    val name: String,
    val countOtOfSources: Int
) : BaseSubscriptionModel() {
    override fun getLayoutId(): Int = R.layout.rv_subscription_folder_item
    override fun getItemId(): Int = id.hashCode()
}