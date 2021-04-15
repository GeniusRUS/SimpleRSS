package com.genius.srss.ui.feed

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentFeedBinding
import com.genius.srss.di.DIManager
import com.genius.srss.di.services.converters.IConverters
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.ViewHolderItemDecoration
import com.ub.utils.base.BaseListAdapter
import com.ub.utils.hideSoftKeyboard
import com.ub.utils.openSoftKeyboard
import dev.chrisbanes.insetter.applyInsetter
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.AddToEndSingle
import javax.inject.Inject
import javax.inject.Provider

@AddToEndSingle
interface FeedView : MvpView {
    fun onStateChanged(state: FeedStateModel)
    fun onUpdateNameToEdit(nameToEdit: String?)
    fun onScreenClose()
    fun onShowError(@StringRes resId: Int)
}

class FeedFragment : MvpAppCompatFragment(R.layout.fragment_feed), FeedView,
    BaseListAdapter.BaseListClickListener<FeedItemModel> {

    @Inject
    lateinit var provider: FeedPresenterProvider

    @Inject
    lateinit var convertersProvider: Provider<IConverters>

    private val presenter: FeedPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        provider.create(arguments.feedUrl)
    }

    private val binding: FragmentFeedBinding by viewBinding(FragmentFeedBinding::bind)

    private val adapter: FeedListAdapter by lazy {
        DIManager.appComponent.inject(this)
        FeedListAdapter(convertersProvider.get())
    }

    private var menu: Menu? = null

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
            presenter.updateFeed()
        }
        binding.feedContent.addItemDecoration(ViewHolderItemDecoration())
        binding.collapsingToolbar.isTitleEnabled = false
        binding.feedContent.setHasFixedSize(true)
        binding.feedContent.adapter = adapter
        adapter.listListener = this

        binding.updateNameField.addTextChangedListener {
            presenter.checkSaveAvailability(it?.toString())
        }
        binding.feedContent.applyInsetter {
            type(navigationBars = true) {
                padding(bottom = true)
            }
        }
        binding.collapsingToolbar.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }

        presenter.updateFeed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (presenter.isInEditMode) {
                    presenter.changeEditMode(isEdit = false)
                } else {
                    findNavController().popBackStack()
                }
                true
            }
            R.id.option_edit -> {
                presenter.changeEditMode(isEdit = true)
                true
            }
            R.id.option_save -> {
                presenter.updateSubscription(newSubscriptionName = binding.updateNameField.text?.toString())
                true
            }
            R.id.option_delete -> {
                presenter.deleteFeed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_options_button, menu)
        this.menu = menu
        menu.findItem(R.id.option_delete).isVisible = false
        menu.findItem(R.id.option_save).isVisible = false
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

        menu?.findItem(R.id.option_delete)?.isVisible = state.isInEditMode
        menu?.findItem(R.id.option_save)?.isVisible = state.isInEditMode
        menu?.findItem(R.id.option_edit)?.isVisible = !state.isInEditMode
        binding.updateNameField.isGone = !state.isInEditMode
        binding.refresher.isGone = state.isInEditMode

        if (state.isInEditMode) {
            menu?.findItem(R.id.option_save)?.isEnabled = state.isAvailableToSave
            openSoftKeyboard(context ?: return, binding.updateNameField)
        } else {
            hideSoftKeyboard(context ?: return)
        }
    }

    override fun onUpdateNameToEdit(nameToEdit: String?) {
        binding.updateNameField.setText(nameToEdit)
    }

    override fun onShowError(resId: Int) {
        Snackbar.make(binding.rootView, resId, Snackbar.LENGTH_LONG)
            .setAction(R.string.subscription_feed_error_action) {
                presenter.updateFeed()
            }
            .show()
    }

    override fun onScreenClose() {
        findNavController().popBackStack()
    }

    override fun onClick(view: View, item: FeedItemModel, position: Int) {
        val colorScheme = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(view.context, R.color.red_dark))
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorScheme)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .setUrlBarHidingEnabled(true)
            .build()
        customTabsIntent.launchUrl(view.context, Uri.parse(item.url))
    }
}