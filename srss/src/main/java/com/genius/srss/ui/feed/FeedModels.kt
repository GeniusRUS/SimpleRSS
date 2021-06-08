package com.genius.srss.ui.feed

import androidx.annotation.DrawableRes
import com.genius.srss.R
import com.ub.utils.base.DiffComparable
import java.util.*

data class FeedStateModel(
    val isRefreshing: Boolean = false,
    val feedContent: List<FeedModels> = listOf(),
    val title: String? = null,
    val isInEditMode: Boolean = false,
    val isAvailableToSave: Boolean = false
)

sealed class FeedModels : DiffComparable {
    abstract val id: String?
    abstract val layoutId: Int
    override fun getItemId(): Int = id.hashCode()
}

data class FeedItemModel(
    override val id: String?,
    val url: String?,
    val pictureUrl: String?,
    val title: String?,
    val publicationDate: Date?,
    override val layoutId: Int = R.layout.rv_feed_item,
) : FeedModels()

data class FeedEmptyModel(
    override val id: String = "empty",
    @DrawableRes
    val icon: Int,
    val message: String? = null,
    val actionText: String? = null,
    override val layoutId: Int = R.layout.rv_feed_empty,
) : FeedModels()