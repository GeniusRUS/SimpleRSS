package com.genius.srss.ui

import android.content.Intent
import android.os.Bundle
import android.webkit.URLUtil
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import androidx.navigation.navArgument
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.MainGraphDirections
import com.genius.srss.R
import com.genius.srss.databinding.ActivityHostBinding
import com.genius.srss.ui.add.folder.AddFolderScreen
import com.genius.srss.ui.add.subscription.AddSubscriptionFragmentDirections
import com.genius.srss.ui.add.subscription.AddSubscriptionScreen
import com.genius.srss.ui.feed.FeedScreen
import com.genius.srss.ui.folder.FolderScreen
import com.genius.srss.ui.subscriptions.SubscriptionScreen
import com.genius.srss.ui.subscriptions.urlDecode
import com.google.android.material.snackbar.Snackbar

@ExperimentalFoundationApi
@ExperimentalMaterialApi
class HostActivity : AppCompatActivity(/*R.layout.activity_host*/) {

    private val binding: ActivityHostBinding by viewBinding(ActivityHostBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*binding.applicationNavigationContainer.post {
            handleSharingIntent(intent)
        }*/

        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "subscription") {
                composable("subscription") {
                    SubscriptionScreen(
                        navigateToFolder = { folderId ->
                            navController.navigate("folder/${folderId}")
                        },
                        navigateToFeed = { feedUrl ->
                            navController.navigate("feed/$feedUrl")
                        },
                        navigateToAddSubscription = {
                            navController.navigate("addSubscription")
                        }
                    )
                }
                composable("addSubscription") {
                    AddSubscriptionScreen(
                        isCanNavigateUp = navController.previousBackStackEntry != null,
                        navigateOnAdd = { addedFeedUrl ->
                            navController.navigate("feed/${addedFeedUrl}") {
                                popUpTo("addSubscription") {
                                    inclusive = true
                                }
                            }
                        },
                        navigateUp = {
                            navController.navigateUp()
                        }
                    )
                }
                composable("addFolder") { AddFolderScreen(navController) }
                composable(
                    route = "feed/{url}",
                    arguments = listOf(navArgument("url") { type = NavType.StringType })
                ) { backStackEntry ->
                    val feedUrl = backStackEntry.arguments?.getString("url")?.urlDecode()
                    FeedScreen(
                        feedUrl = feedUrl ?: return@composable,
                        navigateToUp = { navController.navigateUp() },
                        isCanNavigateUp = navController.previousBackStackEntry != null
                    )
                }
                composable(
                    route = "folder/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStackEntry ->
                    val folderId = backStackEntry.arguments?.getString("id")
                    FolderScreen(
                        navController
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleSharingIntent(intent)
    }

    private fun handleSharingIntent(intent: Intent?) {
        val externalUrl = intent?.extras?.getString(Intent.EXTRA_TEXT)
            ?: intent?.clipData?.getItemAt(0)?.text?.toString()
        externalUrl?.let { url ->
            handleUrlFromExternalSource(url)
            intent?.removeExtra(Intent.EXTRA_TEXT)
            intent?.clipData = null
        }
    }

    private fun handleUrlFromExternalSource(url: String) {
        if (URLUtil.isValidUrl(url)) {
            val direction =
                MainGraphDirections.actionGlobalAddFragment(url)
            findNavController(R.id.application_navigation_container).navigate(direction)
        } else {
            Snackbar.make(
                binding.rootView,
                R.string.error_illegal_argument_url,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}