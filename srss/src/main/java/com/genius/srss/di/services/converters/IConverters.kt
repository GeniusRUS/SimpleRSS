package com.genius.srss.di.services.converters

import java.util.*

interface IConverters {
    fun formatDateToString(date: Date): String
    suspend fun extractImageUrlFromText(htmlTextWithImage: String?): String?
}