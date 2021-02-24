package com.genius.srss.ui.feed

import com.ub.utils.base.DiffComparable
import java.util.*

data class FeedStateModel(
    val isRefreshing: Boolean = false,
    val feedContent: List<FeedItemModel> = listOf(),
    val title: String? = null,
    val isInEditMode: Boolean = false,
    val isAvailableToSave: Boolean = false
)

data class FeedItemModel(
    val id: String?,
    val url: String?,
    val pictureUrl: String?,
    val title: String?,
    val publicationDate: Date?
) : DiffComparable {
    override fun getItemId(): Int = id.hashCode()
}