package com.genius.srss.ui.subscriptions

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.genius.srss.R
import com.genius.srss.ui.theme.SRSSTheme

@ExperimentalMaterialApi
@Preview(
    name = "Standard screen state",
    showBackground = true,
    locale = "ru"
)
@Composable
fun StandardSubscriptionScreenPreview() {
    SRSSTheme {
        SubscriptionScreen(
            navigateToFolder = {},
            navigateToFeed = {},
            navigateToAddFolder = {},
            navigateToAddSubscription = {},
            viewModelInterface = object : ISubscriptionViewModel {
                override fun handleHolderMove(holderPosition: Int, targetPosition: Int) {
                }

                override fun removeSubscriptionByPosition(position: Int) {
                }

                override fun updateFeed(isFull: Boolean?) {
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
@Preview(
    name = "Empty screen state",
    showBackground = true,
    locale = "ru"
)
@Composable
fun EmptySubscriptionScreenPreview() {
    SRSSTheme {
        SubscriptionScreen(
            navigateToFolder = {},
            navigateToFeed = {},
            navigateToAddFolder = {},
            navigateToAddSubscription = {},
            viewModelInterface = object : ISubscriptionViewModel {
                override fun handleHolderMove(holderPosition: Int, targetPosition: Int) {
                }

                override fun removeSubscriptionByPosition(position: Int) {
                }

                override fun updateFeed(isFull: Boolean?) {
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