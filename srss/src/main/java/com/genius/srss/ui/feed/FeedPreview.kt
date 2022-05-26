package com.genius.srss.ui.feed

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.genius.srss.R
import com.genius.srss.ui.theme.SRSSTheme

@Preview(
    name = "Regular",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Composable
fun FeedItemPreview() {
    SRSSTheme {
        FeedItem(
            title = "Название элемента",
            date = "Сегодня в 22:15",
            pictureUrl = null,
            onClick = {}
        )
    }
}

@Preview(
    name = "Night",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun FeedItemNightPreview() {
    SRSSTheme {
        FeedItem(
            title = "Название элемента",
            date = "Сегодня в 22:15",
            pictureUrl = null,
            onClick = {}
        )
    }
}

@Preview(
    name = "Regular empty",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Composable
fun FeedEmptyItemPreview() {
    SRSSTheme {
        FeedEmptyItem(
            icon = R.drawable.ic_vector_empty_folder,
            message = "Пустая папка",
            action = "Добавить ленту",
            onClick = {}
        )
    }
}

@Preview(
    name = "Night empty",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun FeedEmptyItemNightPreview() {
    SRSSTheme {
        FeedEmptyItem(
            icon = R.drawable.ic_vector_empty_folder,
            message = "Пустая папка",
            action = "Добавить ленту",
            onClick = {}
        )
    }
}