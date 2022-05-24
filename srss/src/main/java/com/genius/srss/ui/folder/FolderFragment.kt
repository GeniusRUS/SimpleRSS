package com.genius.srss.ui.folder

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.genius.srss.R
import com.genius.srss.databinding.FragmentFolderBinding
import com.genius.srss.di.DIManager
import com.genius.srss.di.services.converters.IConverters
import com.genius.srss.ui.feed.FeedEmptyItem
import com.genius.srss.ui.feed.FeedItem
import com.genius.srss.ui.feed.collectAsEffect
import com.genius.srss.ui.subscriptions.BaseSubscriptionModel
import com.genius.srss.ui.subscriptions.FeedItemModel
import com.genius.srss.ui.subscriptions.SubscriptionFolderEmptyModel
import com.genius.srss.ui.subscriptions.SubscriptionItem
import com.genius.srss.ui.subscriptions.SubscriptionItemModel
import com.genius.srss.ui.subscriptions.SubscriptionsListAdapter
import com.genius.srss.ui.subscriptions.urlEncode
import com.genius.srss.ui.theme.SRSSTheme
import com.genius.srss.util.launchAndRepeatWithViewLifecycle
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.LogUtils
import com.ub.utils.base.BaseListAdapter
import com.ub.utils.openSoftKeyboard
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class FolderFragment : Fragment(),
    BaseListAdapter.BaseListClickListener<BaseSubscriptionModel>,
    FolderTouchHelperCallback.TouchFolderListener, View.OnClickListener {

    @Inject
    lateinit var provider: FolderViewModelFactory

    @Inject
    lateinit var convertersProvider: Provider<IConverters>

    private val viewModel: FolderViewModel by viewModels {
        DIManager.appComponent.inject(this)
        provider.create(arguments.folderId)
    }

    private val adapter: SubscriptionsListAdapter by lazy {
        DIManager.appComponent.inject(this)
        SubscriptionsListAdapter()
    }

    private lateinit var binding: FragmentFolderBinding

    private val arguments: FolderFragmentArgs by navArgs()

    private var menu: Menu? = null
    private var isInInteractionMode: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFolderBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@FolderFragment.viewModel
        }
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

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val isInEditMode = viewModel.isInEditMode
                    if (isInEditMode) {
                        viewModel.changeEditMode(isEdit = false)
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
                if (viewModel.isInFeedListMode && !isInInteractionMode) {
                    isInInteractionMode = true
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (binding.folderContent.canScrollVertically(-1) && viewModel.isInFeedListMode) {
                        binding.navigationFab.show()
                    } else {
                        binding.navigationFab.hide()
                    }
                }
            }
        })
        binding.updateNameField.setOnEditorActionListener { _, keyCode, _ ->
            return@setOnEditorActionListener if (keyCode == EditorInfo.IME_ACTION_DONE) {
                viewModel.updateFolder(newFolderName = binding.updateNameField.text?.toString())
                true
            } else false
        }

        ItemTouchHelper(
            FolderTouchHelperCallback(
                view.context,
                this,
                R.drawable.ic_vector_link_off,
                Color.TRANSPARENT
            )
        ).attachToRecyclerView(binding.folderContent)

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

        viewModel.updateFolderFeed()

        launchAndRepeatWithViewLifecycle {
            launch {
                viewModel.loadedFeedCountFlow.collect { count ->
                    Snackbar.make(
                        binding.rootView,
                        count,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            launch {
                viewModel.screenCloseFlow.collect {
                    findNavController().popBackStack()
                }
            }
            launch {
                viewModel.state.collect { state ->
                    adapter.update(state.feedList)

                    menu?.findItem(R.id.option_delete)?.isVisible = state.isInEditMode
                    menu?.findItem(R.id.option_save)?.isVisible = state.isInEditMode
                    menu?.findItem(R.id.option_edit)?.isVisible = !state.isInEditMode
                    menu?.findItem(R.id.option_mode)?.isVisible =
                        !state.isInEditMode && state.feedList.any { feed -> feed !is SubscriptionFolderEmptyModel }
                    menu?.findItem(R.id.option_mode)?.icon = if (state.isCombinedMode) {
                        VectorDrawableCompat.create(
                            resources,
                            R.drawable.ic_vector_folder,
                            context?.theme
                        )
                    } else {
                        VectorDrawableCompat.create(
                            resources,
                            R.drawable.ic_vector_list,
                            context?.theme
                        )
                    }
                    binding.refresher.isEnabled = state.isCombinedMode
                    if (state.isCombinedMode && binding.folderContent.canScrollVertically(-1)) {
                        if (!isInInteractionMode) {
                            binding.folderContent.scrollToPosition(0)
                        } else {
                            binding.navigationFab.show()
                        }
                    } else {
                        binding.navigationFab.hide()
                    }

                    if (state.isInEditMode) {
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
        }
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
                viewModel.changeEditMode(isEdit = true)
                true
            }
            R.id.option_save -> {
                viewModel.updateFolder(newFolderName = binding.updateNameField.text?.toString())
                true
            }
            R.id.option_delete -> {
                viewModel.deleteFolder()
                true
            }
            R.id.option_mode -> {
                viewModel.changeMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    override fun onClick(
        view: View,
        item: BaseSubscriptionModel,
        position: Int,
    ) {
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
                    .setToolbarColor(ContextCompat.getColor(view.context, R.color.primary_dark))
                    .build()
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(colorScheme)
                    .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                    .setUrlBarHidingEnabled(true)
                    .build()
                customTabsIntent.launchUrl(view.context, Uri.parse(item.url))
            }
            else -> {
                // no-op
            }
        }
    }

    override fun onFolderDismiss(position: Int) {
        viewModel.unlinkFolderByPosition(position)
    }
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun FolderScreen(
    folderId: String,
    navigateUp: () -> Unit,
    isCanNavigateUp: Boolean,
    navigateToFeed: (String) -> Unit,
    navigateToPost: (String?) -> Unit,
    navigateToAdd: (String) -> Unit,
    viewModel: FolderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = FolderModelFactory(
            folderId = folderId,
            context = DIManager.appComponent.context,
            subscriptionsDao = DIManager.appComponent.subscriptionDao,
            converters = DIManager.appComponent.converters,
            network = DIManager.appComponent.network
        )
    )
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var newFolderName by remember { mutableStateOf<String?>(null) }
    val state by viewModel.state.collectAsState()
    viewModel.nameToEditFlow.collectAsEffect { nameToEdit ->
        newFolderName = nameToEdit
    }
    viewModel.screenCloseFlow.collectAsEffect {
        navigateUp.invoke()
    }
    viewModel.loadedFeedCountFlow.collectAsEffect { count ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = count
            )
        }
    }
    SRSSTheme {
        Surface {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    MediumTopAppBar(
                        title = {
                            if (state.isInEditMode) {
                                BasicTextField(
                                    value = newFolderName ?: "",
                                    onValueChange = {
                                        newFolderName = it
                                        viewModel.checkSaveAvailability(
                                            SpannableStringBuilder.valueOf(
                                                it
                                            )
                                        )
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = MaterialTheme.typography.bodyLarge.color)
                                )
                            } else {
                                Text(
                                    text = state.title ?: ""
                                )
                            }
                        },
                        navigationIcon = {
                            if (isCanNavigateUp) {
                                IconButton(
                                    onClick = {
                                        if (state.isInEditMode) {
                                            viewModel.changeEditMode(isEdit = false)
                                        } else {
                                            navigateUp.invoke()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = stringResource(id = android.R.string.cancel)
                                    )
                                }
                            }
                        },
                        actions = {
                            if (!state.isInEditMode) {
                                IconButton(onClick = {
                                    viewModel.changeMode()
                                }) {
                                    Icon(
                                        painter = if (state.isCombinedMode) {
                                            painterResource(id = R.drawable.ic_vector_folder)
                                        } else {
                                            painterResource(id = R.drawable.ic_vector_list)
                                        },
                                        contentDescription = stringResource(id = R.string.folder_menu_mode)
                                    )
                                }
                            }
                            if (!state.isInEditMode) {
                                IconButton(onClick = {
                                    viewModel.changeEditMode(isEdit = true)
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_vector_mode),
                                        contentDescription = stringResource(id = R.string.folder_menu_edit_title)
                                    )
                                }
                            }
                            if (state.isInEditMode) {
                                IconButton(onClick = {
                                    viewModel.deleteFolder()
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete_outline),
                                        contentDescription = stringResource(id = R.string.option_menu_delete)
                                    )
                                }
                            }
                            if (state.isInEditMode) {
                                IconButton(onClick = {
                                    viewModel.updateFolder(newFolderName = newFolderName)
                                }, enabled = state.isAvailableToSave) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_vector_done),
                                        contentDescription = stringResource(id = R.string.option_menu_save)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
            ) { paddings ->
                LazyColumn(
                    contentPadding = WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues(),
                    content = {
                        state.feedList.forEachIndexed { index, model ->
                            when (model) {
                                is SubscriptionItemModel -> item(
                                    key = model.getItemId(),
                                ) {
                                    val dismissState = rememberDismissState()
                                    if (dismissState.isDismissed(DismissDirection.StartToEnd)) {
                                        viewModel.unlinkFolderByPosition(index)
                                    }
                                    SwipeToDismiss(
                                        directions = setOf(DismissDirection.StartToEnd),
                                        state = dismissState,
                                        dismissThresholds = { FractionalThreshold(0.25f) },
                                        background = {
                                            val scale by animateFloatAsState(
                                                if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f
                                            )
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_vector_link_off),
                                                    contentDescription = stringResource(id = R.string.option_menu_delete),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.scale(scale)
                                                )
                                            }
                                        }
                                    ) {
                                        SubscriptionItem(
                                            title = model.title ?: "",
                                            position = index,
                                            onClick = {
                                                navigateToFeed.invoke(
                                                    model.urlToLoad?.urlEncode()
                                                        ?: return@SubscriptionItem
                                                )
                                            }
                                        )
                                    }
                                }
                                is FeedItemModel -> item(
                                    key = model.getItemId(),
                                ) {
                                    FeedItem(
                                        title = model.title ?: "",
                                        date = model.timestamp?.stringRepresentation,
                                        pictureUrl = model.pictureUrl,
                                        onClick = {
                                            navigateToPost.invoke(model.url)
                                        }
                                    )
                                }
                                is SubscriptionFolderEmptyModel -> item(
                                    key = model.getItemId(),
                                ) {
                                    FeedEmptyItem(
                                        icon = model.icon,
                                        message = model.message,
                                        action = model.actionText,
                                        modifier = Modifier.fillParentMaxHeight(),
                                        onClick = {
                                            navigateToAdd.invoke(folderId)
                                        }
                                    )
                                }
                                else -> throw IllegalArgumentException("Unsupported feed model: ${model::class.java.simpleName}")
                            }
                        }
                    }
                )
            }
        }
    }
}