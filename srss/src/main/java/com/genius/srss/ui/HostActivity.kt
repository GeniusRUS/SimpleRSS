package com.genius.srss.ui

import android.content.Intent
import android.os.Bundle
import android.webkit.URLUtil
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
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
import com.genius.srss.di.DIManager
import com.genius.srss.ui.add.folder.AddFolderScreen
import com.genius.srss.ui.add.subscription.AddSubscriptionScreen
import com.genius.srss.ui.feed.FeedScreen
import com.genius.srss.ui.feed.openFeed
import com.genius.srss.ui.folder.FolderScreen
import com.genius.srss.ui.subscriptions.SubscriptionScreen
import com.genius.srss.ui.subscriptions.SubscriptionsViewModel
import com.genius.srss.ui.subscriptions.SubscriptionsViewModelFactory
import com.genius.srss.ui.subscriptions.urlDecode
import com.google.android.material.snackbar.Snackbar

@ExperimentalMaterialApi
class HostActivity : AppCompatActivity(/*R.layout.activity_host*/) {

    private val binding: ActivityHostBinding by viewBinding(ActivityHostBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*binding.applicationNavigationContainer.post {
            handleSharingIntent(intent)
        }*/

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "subscription"
            ) {
                composable("subscription") {
                    val viewModel: SubscriptionsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = SubscriptionsViewModelFactory(
                            DIManager.appComponent.context,
                            DIManager.appComponent.subscriptionDao,
                            DIManager.appComponent.dataStore
                        )
                    )
                    val state by viewModel.state.collectAsState()
                    SubscriptionScreen(
                        navigateToFolder = { folderId ->
                            navController.navigate("folder/${folderId}")
                        },
                        navigateToFeed = { feedUrl ->
                            navController.navigate("feed/$feedUrl")
                        },
                        navigateToAddFolder = {
                            navController.navigate("addFolder")
                        },
                        navigateToAddSubscription = {
                            navController.navigate("addSubscription")
                        },
                        viewModelInterface = viewModel,
                        state = state
                    )
                }
                composable(
                    route = "addSubscription?folderId={folderId}",
                    arguments = listOf(navArgument("folderId") {
                        type = NavType.StringType
                        defaultValue = null
                        nullable = true
                    })
                ) { backStackEntry ->
                    val folderId = backStackEntry.arguments?.getString("folderId")
                    AddSubscriptionScreen(
                        folderId = folderId,
                        isCanNavigateUp = navController.previousBackStackEntry != null,
                        navigateOnAdd = { addedFeedUrl ->
                            navController.navigate("feed/${addedFeedUrl}") {
                                popUpTo("addSubscription?folderId={folderId}") {
                                    inclusive = true
                                }
                            }
                        },
                        navigateUp = {
                            navController.navigateUp()
                        }
                    )
                }
                composable("addFolder") {
                    AddFolderScreen(
                        isCanNavigateUp = navController.previousBackStackEntry != null,
                        navigateUp = { navController.navigateUp() },
                        navigateOnAdd = { navController.navigateUp() }
                    )
                }
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
                        folderId = folderId ?: return@composable,
                        navigateUp = { navController.navigateUp() },
                        isCanNavigateUp = navController.previousBackStackEntry != null,
                        navigateToFeed = { feedUrl ->
                            navController.navigate("feed/${feedUrl}")
                        },
                        navigateToPost = { postUrl ->
                            openFeed(context = this@HostActivity, postUrl)
                        },
                        navigateToAdd = { folderIdToAdd ->
                            navController.navigate("addSubscription?folderId=$folderIdToAdd")
                        }
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