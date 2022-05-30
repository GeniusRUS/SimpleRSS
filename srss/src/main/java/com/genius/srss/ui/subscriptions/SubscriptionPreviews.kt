package com.genius.srss.ui.subscriptions

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.genius.srss.R
import com.genius.srss.ui.theme.SRSSTheme

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
internal fun PreviewSubscriptionNotEmptyScreen() {
    SRSSTheme {
        SubscriptionScreen(
            navigateToFolder = {},
            navigateToFeed = {},
            navigateToAddFolder = {},
            navigateToAddSubscription = {},
            viewModelDelegate = object : SubscriptionViewModelDelegate {
                override fun handleHolderMove(url: String, folderId: String) {
                }

                override fun removeSubscriptionByPosition(position: Int) {
                }

                override fun updateFeed(isFull: Boolean?) {
                }

                override fun onEndTutorial() {
                }
            },
            state = SubscriptionsStateModel(
                feedList = listOf(
                    SubscriptionFolderItemModel(id = "folder1", name = "Name 1", countOtOfSources = 0),
                    SubscriptionFolderItemModel(id = "folder2", name = "Name 2", countOtOfSources = 10),
                    SubscriptionItemModel(title = "Title 1", urlToLoad = "https://google.com/rss/1"),
                    SubscriptionItemModel(title = "Title 2", urlToLoad = "https://google.com/rss/2"),
                    SubscriptionItemModel(title = "Title 3", urlToLoad = "https://google.com/rss/3"),
                    SubscriptionItemModel(title = "Title 4", urlToLoad = "https://google.com/rss/4"),
                ),
                isFullList = false,
                isTutorialShow = false
            )
        )
    }
}

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
internal fun PreviewSubscriptionEmptyScreen() {
    SRSSTheme {
        SubscriptionScreen(
            navigateToFolder = {},
            navigateToFeed = {},
            navigateToAddFolder = {},
            navigateToAddSubscription = {},
            viewModelDelegate = object : SubscriptionViewModelDelegate {
                override fun handleHolderMove(url: String, folderId: String) {
                }

                override fun removeSubscriptionByPosition(position: Int) {
                }

                override fun updateFeed(isFull: Boolean?) {
                }

                override fun onEndTutorial() {
                }
            },
            state = SubscriptionsStateModel(
                feedList = listOf(
                    SubscriptionFolderEmptyModel(
                        id = "folder1",
                        icon = R.drawable.ic_vector_empty_folder,
                        message = "n/a",
                        actionText = "update"
                    ),
                ),
                isFullList = false,
                isTutorialShow = false
            )
        )
    }
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Preview(
    name = "Standard dynamic",
    showBackground = true,
    locale = "ru"
)
@Composable
fun StandardDynamicSubscriptionScreenPreview() {
    PreviewSubscriptionNotEmptyScreen()
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Preview(
    name = "Empty dynamic",
    showBackground = true,
    locale = "ru"
)
@Composable
fun EmptyDynamicSubscriptionScreenPreview() {
    PreviewSubscriptionEmptyScreen()
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Preview(
    name = "Standard default dark",
    showBackground = true,
    locale = "ru",
    apiLevel = Build.VERSION_CODES.P,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun StandardDefaultSubscriptionScreenPreview() {
    PreviewSubscriptionNotEmptyScreen()
}