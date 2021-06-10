package com.genius.srss.ui.folder

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.net.Uri
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentFolderBinding
import com.genius.srss.di.DIManager
import com.genius.srss.di.services.converters.IConverters
import com.genius.srss.ui.subscriptions.*
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.LogUtils
import com.ub.utils.base.BaseListAdapter
import com.ub.utils.openSoftKeyboard
import dev.chrisbanes.insetter.applyInsetter
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.OneExecution
import javax.inject.Inject
import javax.inject.Provider

@OneExecution
interface FolderView : MvpView {
    fun onStateChanged(state: FolderStateModel)
    fun onUpdateNameToEdit(nameToEdit: String?)
    fun onScreenClose()
    fun onShowLoadedFeedsCount(count: Int)
}

class FolderFragment : MvpAppCompatFragment(R.layout.fragment_folder), FolderView,
    BaseListAdapter.BaseListClickListener<BaseSubscriptionModel>,
    FolderTouchHelperCallback.TouchFolderListener, View.OnClickListener {

    @Inject
    lateinit var provider: FolderPresenterFactory

    @Inject
    lateinit var convertersProvider: Provider<IConverters>

    private val presenter: FolderPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        provider.create(arguments.folderId)
    }

    private val adapter: SubscriptionsListAdapter by lazy {
        DIManager.appComponent.inject(this)
        SubscriptionsListAdapter(convertersProvider.get())
    }

    private val binding: FragmentFolderBinding by viewBinding(FragmentFolderBinding::bind)

    private val arguments: FolderFragmentArgs by navArgs()

    private var menu: Menu? = null

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
            presenter.updateFolderFeed(isManual = true)
        }
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object: OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    val isInEditMode = presenter.isInEditMode
                    if (isInEditMode) {
                        presenter.changeEditMode(isEdit = false)
                    } else {
                        findNavController().popBackStack()
                    }
                }
            }
        )

        binding.navigationFab.setOnClickListener(this)
        binding.collapsingToolbar.isTitleEnabled = false
        binding.folderContent.adapter = adapter
        binding.folderContent.setHasFixedSize(true)
        adapter.listListener = this

        binding.folderContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (binding.folderContent.canScrollVertically(-1) && presenter.isInFeedListMode) {
                        binding.navigationFab.show()
                    } else {
                        binding.navigationFab.hide()
                    }
                }
            }
        })
        binding.updateNameField.setOnEditorActionListener { _, keyCode, _ ->
            return@setOnEditorActionListener if (keyCode == EditorInfo.IME_ACTION_DONE) {
                presenter.updateFolder(newFolderName = binding.updateNameField.text?.toString())
                true
            } else false
        }

        ItemTouchHelper(
            FolderTouchHelperCallback(
                view.context,
                this,
                R.drawable.ic_vector_link_off_24dp,
                Color.TRANSPARENT
            )
        ).attachToRecyclerView(binding.folderContent)

        binding.updateNameField.addTextChangedListener {
            presenter.checkSaveAvailability(it?.toString())
        }
        binding.folderContent.applyInsetter {
            type(navigationBars = true) {
                padding(bottom = true)
            }
        }
        binding.collapsingToolbar.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }
        binding.navigationFab.applyInsetter {
            type(navigationBars = true) {
                margin()
            }
        }

        presenter.updateFolderFeed()
    }

    override fun onDestroyView() {
        adapter.listListener = null
        binding.folderContent.adapter = null
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_options_button, menu)
        this.menu = menu
        menu.findItem(R.id.option_delete).isVisible = false
        menu.findItem(R.id.option_save).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                true
            }
            R.id.option_edit -> {
                presenter.changeEditMode(isEdit = true)
                true
            }
            R.id.option_save -> {
                presenter.updateFolder(newFolderName = binding.updateNameField.text?.toString())
                true
            }
            R.id.option_delete -> {
                presenter.deleteFolder()
                true
            }
            R.id.option_mode -> {
                presenter.changeMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStateChanged(state: FolderStateModel) {
        adapter.update(state.feedList)
        (activity as? AppCompatActivity)?.supportActionBar?.title = state.title ?: ""

        menu?.findItem(R.id.option_delete)?.isVisible = state.isInEditMode
        menu?.findItem(R.id.option_save)?.isVisible = state.isInEditMode
        menu?.findItem(R.id.option_edit)?.isVisible = !state.isInEditMode
        menu?.findItem(R.id.option_mode)?.isVisible = !state.isInEditMode
        menu?.findItem(R.id.option_mode)?.icon = if (state.isCombinedMode) {
            VectorDrawableCompat.create(resources, R.drawable.ic_vector_folder_24dp, context?.theme)
        } else {
            VectorDrawableCompat.create(resources, R.drawable.ic_vector_list_24dp, context?.theme)
        }
        binding.refresher.isEnabled = state.isCombinedMode
        binding.refresher.isRefreshing = state.isInFeedLoadingProgress
        binding.updateNameField.isGone = !state.isInEditMode
        binding.folderContent.isGone = state.isInEditMode
        if (state.isCombinedMode && binding.folderContent.canScrollVertically(-1)) {
            binding.navigationFab.show()
        } else {
            binding.navigationFab.hide()
        }

        if (state.isInEditMode) {
            menu?.findItem(R.id.option_save)?.isEnabled = state.isAvailableToSave
            openSoftKeyboard(binding.updateNameField.context, binding.updateNameField)
        } else {
            try {
                val inputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.updateNameField.windowToken, 0)
                binding.updateNameField.clearFocus()
            } catch (e: NullPointerException) {
                LogUtils.e("KeyBoard", "NULL point exception in input method service")
            }
        }
    }

    override fun onUpdateNameToEdit(nameToEdit: String?) {
        binding.updateNameField.setText(nameToEdit)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.navigation_fab -> {
                binding.appBar.setExpanded(true, true)
                binding.folderContent.scrollToPosition(0)
                binding.navigationFab.hide()
            }
        }
    }

    override fun onClick(view: View, item: BaseSubscriptionModel, position: Int) {
        when (item) {
            is SubscriptionItemModel -> {
                val direction = FolderFragmentDirections.actionFolderFragmentToFeedFragment(
                    item.urlToLoad ?: return
                )
                findNavController().navigate(direction)
            }
            is SubscriptionFolderEmptyModel -> {
                val direction = FolderFragmentDirections.actionFolderFragmentToAddFragment(
                    folderId = arguments.folderId
                )
                findNavController().navigate(direction)
            }
            is FeedItemModel -> {
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
    }

    override fun onFolderDismiss(position: Int) {
        presenter.unlinkFolderByPosition(position)
    }

    override fun onShowLoadedFeedsCount(count: Int) {
        Snackbar.make(
            binding.rootView,
            resources.getQuantityString(R.plurals.folder_feed_list_not_fully_loaded, count, count),
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onScreenClose() {
        findNavController().popBackStack()
    }
}