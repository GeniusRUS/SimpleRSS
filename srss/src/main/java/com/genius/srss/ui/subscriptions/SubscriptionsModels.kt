package com.genius.srss.ui.subscriptions

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.genius.srss.R
import com.ub.utils.base.DiffComparable

data class SubscriptionsStateModel(
    val feedList: List<BaseSubscriptionModel> = listOf(),
    val isFullList: Boolean = false
)

sealed class BaseSubscriptionModel : DiffComparable {
    @get:LayoutRes
    abstract val layoutId: Int
}

data class SubscriptionItemModel(
    val title: String?,
    val urlToLoad: String?,
    override val layoutId: Int = R.layout.rv_subscription_item
) : BaseSubscriptionModel() {
    override fun getItemId(): Int = urlToLoad.hashCode()
}

data class SubscriptionFolderItemModel(
    val id: String,
    val name: String,
    val countOtOfSources: Int,
    override val layoutId: Int = R.layout.rv_subscription_folder_item
) : BaseSubscriptionModel() {
    override fun getItemId(): Int = id.hashCode()
}

data class SubscriptionFolderEmptyModel(
    val id: String = "empty",
    @DrawableRes
    val icon: Int,
    val message: String,
    val action: String?,
    override val layoutId: Int = R.layout.rv_feed_empty
) : BaseSubscriptionModel() {
    override fun getItemId(): Int = id.hashCode()
}