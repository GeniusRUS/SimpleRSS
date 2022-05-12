package com.genius.srss.ui

import android.content.Intent
import android.os.Bundle
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.MainGraphDirections
import com.genius.srss.R
import com.genius.srss.databinding.ActivityHostBinding
import com.google.android.material.snackbar.Snackbar

class HostActivity : AppCompatActivity(R.layout.activity_host) {

    private val binding: ActivityHostBinding by viewBinding(ActivityHostBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.applicationNavigationContainer.post {
            handleSharingIntent(intent)
        }

//        setContent {
//            val navController = rememberNavController()
//            NavHost(navController, startDestination = "subscription") {
//                composable("subscription") { SubscriptionScreen(navController) }
//                composable("addSubscription") { AddSubscriptionScreen(navController) }
//                composable("addFolder") { AddFolderScreen(navController) }
//                composable("feed") { FeedScreen(navController) }
//                composable("folder") { FolderScreen(navController) }
//            }
//        }
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