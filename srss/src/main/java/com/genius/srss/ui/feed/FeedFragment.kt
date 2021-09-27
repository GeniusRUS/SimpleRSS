package com.genius.srss.ui.feed

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeTransform
import com.genius.srss.R
import com.genius.srss.databinding.FragmentFeedBinding
import com.genius.srss.di.DIManager
import com.genius.srss.di.services.converters.IConverters
import com.genius.srss.ui.subscriptions.BaseSubscriptionModel
import com.genius.srss.ui.subscriptions.FeedItemModel
import com.genius.srss.ui.subscriptions.SubscriptionFolderEmptyModel
import com.genius.srss.ui.subscriptions.SubscriptionsListAdapter
import com.genius.srss.util.launchAndRepeatWithViewLifecycle
import com.ub.utils.LogUtils
import com.ub.utils.ViewHolderItemDecoration
import com.ub.utils.openSoftKeyboard
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class FeedFragment : Fragment(),
    SubscriptionsListAdapter.TransitionListClickListener<BaseSubscriptionModel> {

    @Inject
    lateinit var provider: FeedViewModelProvider

    @Inject
    lateinit var convertersProvider: Provider<IConverters>

    private val viewModel: FeedViewModel by viewModels {
        DIManager.appComponent.inject(this)
        provider.create(arguments.feedUrl)
    }

    private lateinit var binding: FragmentFeedBinding

    private val adapter: SubscriptionsListAdapter by lazy {
        DIManager.appComponent.inject(this)
        SubscriptionsListAdapter(convertersProvider.get())
    }

    private var menu: Menu? = null

    private val arguments: FeedFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = ChangeTransform()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@FeedFragment.viewModel
        }
        binding.rootView.transitionName = String.format(
            inflater.context.getString(R.string.transition_root_tag),
            arguments.transitionPosition
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.let { activity ->
            setHasOptionsMenu(true)
            activity.setSupportActionBar(binding.toolbar)
            activity.supportActionBar?.title = ""
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object: OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    val isInEditMode = viewModel.isInEditMode.value
                    if (isInEditMode) {
                        viewModel.changeEditMode(isEdit = false)
                    } else {
                        findNavController().popBackStack()
                    }
                }
            }
        )

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        binding.feedContent.addItemDecoration(ViewHolderItemDecoration())
        binding.collapsingToolbar.isTitleEnabled = false
        binding.feedContent.setHasFixedSize(true)
        binding.feedContent.adapter = adapter
        adapter.transitionClickListener = this

        binding.updateNameField.setOnEditorActionListener { _, keyCode, _ ->
            return@setOnEditorActionListener if (keyCode == EditorInfo.IME_ACTION_DONE) {
                viewModel.updateSubscription(newSubscriptionName = binding.updateNameField.text?.toString())
                true
            } else false
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

        viewModel.updateFeed()

        launchAndRepeatWithViewLifecycle {
            launch {
                viewModel.state.collect { state ->
                    adapter.update(state.feedContent)

                    if (viewModel.isInEditMode.value) {
                        menu?.findItem(R.id.option_save)?.isEnabled = state.isAvailableToSave
                        openSoftKeyboard(binding.updateNameField.context, binding.updateNameField)
                    } else {
                        try {
                            val inputMethodManager =
                                context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(
                                binding.updateNameField.windowToken,
                                0
                            )
                            binding.updateNameField.clearFocus()
                        } catch (e: NullPointerException) {
                            LogUtils.e("KeyBoard", "NULL point exception in input method service")
                        }
                    }
                }
            }
            launch {
                viewModel.isInEditMode.collect { isInEditMode ->
                    menu?.findItem(R.id.option_delete)?.isVisible = isInEditMode
                    menu?.findItem(R.id.option_save)?.isVisible = isInEditMode
                    menu?.findItem(R.id.option_edit)?.isVisible = !isInEditMode
                }
            }
            launch {
                viewModel.closeFlow.collect {
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                true
            }
            R.id.option_edit -> {
                viewModel.changeEditMode(isEdit = true)
                true
            }
            R.id.option_save -> {
                viewModel.updateSubscription(newSubscriptionName = binding.updateNameField.text?.toString())
                true
            }
            R.id.option_delete -> {
                viewModel.deleteFeed()
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
        menu.findItem(R.id.option_mode)?.isVisible = false
    }

    override fun onDestroyView() {
        adapter.listListener = null
        binding.feedContent.adapter = null
        super.onDestroyView()
    }

    override fun onClick(
        view: View,
        item: BaseSubscriptionModel,
        position: Int,
        vararg transitionView: View
    ) {
        if (item is FeedItemModel) {
            val colorScheme = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(view.context, R.color.red_dark))
                .build()
            val customTabsIntent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorScheme)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setUrlBarHidingEnabled(true)
                .build()
            customTabsIntent.launchUrl(view.context, Uri.parse(item.url))
        } else if (item is SubscriptionFolderEmptyModel) {
            viewModel.updateFeed()
        }
    }
}