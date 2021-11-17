package com.genius.srss.ui.feed

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.genius.srss.R
import com.genius.srss.databinding.FragmentFeedBinding
import com.genius.srss.di.DIManager
import com.genius.srss.di.services.converters.IConverters
import com.genius.srss.utils.bindings.viewBinding
import com.ub.utils.base.BaseListAdapter
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.AddToEndSingle
import javax.inject.Inject
import javax.inject.Provider

@AddToEndSingle
interface FeedView : MvpView {
    fun onStateChanged(state: FeedStateModel)
}

class FeedFragment : MvpAppCompatFragment(R.layout.fragment_feed), FeedView, BaseListAdapter.BaseListClickListener<FeedItemModel> {

    @Inject
    lateinit var provider: Provider<FeedPresenter>

    @Inject
    lateinit var convertersProvider: Provider<IConverters>

    private val presenter: FeedPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        provider.get()
    }

    private val binding: FragmentFeedBinding by viewBinding(FragmentFeedBinding::bind)

    private val adapter: FeedListAdapter by lazy { FeedListAdapter(convertersProvider.get()) }

    private val arguments: FeedFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.let { activity ->
            setHasOptionsMenu(true)
            activity.setSupportActionBar(binding.toolbar)
            activity.supportActionBar?.title = ""
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        binding.refresher.setOnRefreshListener {
            presenter.updateFeed(arguments.feedUrl)
        }
        binding.collapsingToolbar.isTitleEnabled = false
        binding.feedContent.setHasFixedSize(true)
        binding.feedContent.adapter = adapter
        adapter.listListener = this

        binding.feedContent.applySystemWindowInsetsToPadding(
            left = true,
            right = true,
            bottom = true
        )
        binding.collapsingToolbar.applySystemWindowInsetsToPadding(
            left = true,
            top = true,
            right = true
        )

        presenter.updateFeed(arguments.feedUrl)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        adapter.listListener = null
        binding.feedContent.adapter = null
        super.onDestroyView()
    }

    override fun onStateChanged(state: FeedStateModel) {
        adapter.update(state.feedContent)
        binding.refresher.isRefreshing = state.isRefreshing
        (activity as? AppCompatActivity)?.supportActionBar?.title = state.title ?: ""
    }

    override fun onClick(view: View, item: FeedItemModel, position: Int) {
        val colorScheme = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(view.context, R.color.primary_dark_color))
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorScheme)
            .build()
        customTabsIntent.launchUrl(view.context, Uri.parse(item.url))
    }
}