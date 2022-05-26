package com.genius.srss.ui.subscriptions

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.genius.srss.R
import com.ub.utils.base.DiffComparable
import java.util.Date

interface SubscriptionViewModelDelegate {
    fun removeSubscriptionByPosition(position: Int)
    fun updateFeed(isFull: Boolean? = null)
    fun handleHolderMove(url: String, folderId: String)
    fun onEndTutorial()
}

data class SubscriptionsStateModel(
    val feedList: List<BaseSubscriptionModel> = listOf(),
    val isFullList: Boolean = false,
    val isTutorialShow: Boolean = false
)

sealed class BaseSubscriptionModel : DiffComparable {
    @get:LayoutRes
    abstract val layoutId: Int
}

data class SubscriptionItemModel(
    val title: String?,
    val urlToLoad: String?,
    override val layoutId: Int = R.layout.rv_subscription_item,
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
    @DrawableRes
    val icon: Int,
    val message: String,
    val actionText: String? = null,
    private val id: String = "empty",
    override val layoutId: Int = R.layout.rv_feed_empty
) : BaseSubscriptionModel() {
    override fun getItemId(): Int = id.hashCode()
}

data class FeedItemModel(
    private val id: String?,
    val url: String?,
    val pictureUrl: String?,
    val title: String?,
    val timestamp: Timestamp?,
    override val layoutId: Int = R.layout.rv_feed_item,
) : BaseSubscriptionModel() {
    override fun getItemId(): Int = id?.hashCode() ?: url.hashCode()

    data class Timestamp(
        val date: Date,
        val stringRepresentation: String
    )
}

data class FeedEmptyModel(
    @DrawableRes
    val icon: Int,
    val message: String,
    val actionText: String? = null,
    private val id: String = "empty",
    override val layoutId: Int = R.layout.rv_feed_empty,
) : BaseSubscriptionModel() {
    override fun getItemId(): Int = id.hashCode()
}