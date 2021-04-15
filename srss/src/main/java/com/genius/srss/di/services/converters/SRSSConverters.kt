package com.genius.srss.di.services.converters

import android.content.Context
import android.text.format.DateUtils
import com.genius.srss.R
import com.ub.utils.year
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class SRSSConverters @Inject constructor(
    private val context: Context
) : IConverters {

    private val imgRegex = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>"

    private val simpleDateFullFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    }

    private val imageInTextPattern: Pattern by lazy {
        Pattern.compile(imgRegex)
    }

    private val simpleDateShortFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("d MMMM", Locale.getDefault())
    }

    override fun formatDateToString(date: Date): String {
        val isCurrentYear = Calendar.getInstance().year == Calendar.getInstance().apply {
            time = date
        }.year
        return if (isCurrentYear) {
            when {
                DateUtils.isToday(date.time) -> context.getString(R.string.feed_date_today)
                DateUtils.isToday(date.time + TimeUnit.DAYS.toMillis(1)) -> context.getString(R.string.feed_date_yesterday)
                else -> simpleDateShortFormat.format(date)
            }
        } else {
            simpleDateFullFormat.format(date)
        }
    }

    override suspend fun extractImageUrlFromText(htmlTextWithImage: String?): String? = suspendCoroutine { continuation ->
        try {
            val imageUrl = htmlTextWithImage?.let { inputString ->
                val matcher = imageInTextPattern.matcher(inputString)
                val hasResult = matcher.find()
                if (hasResult) {
                    matcher.group(1)
                } else null
            }
            continuation.resumeWith(Result.success(imageUrl))
        } catch (e: Exception) {
            continuation.resumeWith(Result.failure(e))
        }
    }
}