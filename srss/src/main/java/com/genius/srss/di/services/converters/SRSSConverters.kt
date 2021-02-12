package com.genius.srss.di.services.converters

import com.ub.utils.year
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class SRSSConverters @Inject constructor() : IConverters {

    private val simpleDateFullFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    }

    private val simpleDateShortFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("dd MMMM", Locale.getDefault())
    }

    override fun formatDateToString(date: Date): String {
        val isCurrentYear = Calendar.getInstance().year == Calendar.getInstance().apply {
            time = date
        }.year
        return if (isCurrentYear) {
            simpleDateShortFormat.format(date)
        } else {
            simpleDateFullFormat.format(date)
        }
    }
}