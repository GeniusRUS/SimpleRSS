package com.genius.srss.di.services.converters

import com.einmalfel.earl.Item
import com.genius.srss.ui.subscriptions.FeedItemModel
import java.util.*

interface IConverters {
    fun formatDateToString(date: Date): String
    suspend fun convertNetworkFeedToLocal(item: Item): FeedItemModel
    suspend fun extractImageUrlFromText(htmlTextWithImage: String?): String?
}