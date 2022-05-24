package com.genius.srss.ui.feed

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.genius.srss.R
import com.genius.srss.databinding.FragmentFeedBinding
import com.genius.srss.di.DIManager
import com.genius.srss.ui.subscriptions.BaseSubscriptionModel
import com.genius.srss.ui.subscriptions.FeedEmptyModel
import com.genius.srss.ui.subscriptions.FeedItemModel
import com.genius.srss.ui.subscriptions.SubscriptionFolderEmptyModel
import com.genius.srss.ui.subscriptions.SubscriptionsListAdapter
import com.genius.srss.ui.theme.SRSSTheme
import com.ub.utils.base.BaseListAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
class FeedFragment : Fragment(),
    BaseListAdapter.BaseListClickListener<BaseSubscriptionModel> {

    @Inject
    lateinit var provider: FeedViewModelProvider

    private val viewModel: FeedViewModel by viewModels {
        DIManager.appComponent.inject(this)
        provider.create(arguments.feedUrl)
    }

    private lateinit var binding: FragmentFeedBinding

    private val adapter: SubscriptionsListAdapter by lazy {
        DIManager.appComponent.inject(this)
        SubscriptionsListAdapter()
    }

    private var menu: Menu? = null

    private val arguments: FeedFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SRSSTheme {
                    FeedScreen(
                        feedUrl = arguments.feedUrl,
                        navigateToUp = { findNavController().navigateUp() },
                        isCanNavigateUp = findNavController().previousBackStackEntry != null
                    )
                }
            }
        }
    }

    /*override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@FeedFragment.viewModel
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
        adapter.listListener = this

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
                    menu?.findItem(R.id.option_save)?.isEnabled = viewModel.state.value.isAvailableToSave
                }
            }
            launch {
                viewModel.closeFlow.collect {
                    findNavController().popBackStack()
                }
            }
        }
    }*/

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
//        binding.feedContent.adapter = null
        super.onDestroyView()
    }

    override fun onClick(
        view: View,
        item: BaseSubscriptionModel,
        position: Int
    ) {
        if (item is FeedItemModel) {
            val colorScheme = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(view.context, R.color.primary_dark))
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

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun FeedScreen(
    feedUrl: String,
    navigateToUp: () -> Unit,
    isCanNavigateUp: Boolean,
    viewModel: FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = FeedViewModelFactory(
            feedUrl = feedUrl,
            networkSource = DIManager.appComponent.network,
            subscriptionsDao = DIManager.appComponent.subscriptionDao,
            converters = DIManager.appComponent.converters,
            context = DIManager.appComponent.context
        )
    )
) {
    val state by viewModel.state.collectAsState()
    val isInEditMode by viewModel.isInEditMode.collectAsState()
    var newFeedName by remember { mutableStateOf<String?>(null) }
    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }
    viewModel.closeFlow.collectAsEffect(block = {
        navigateToUp.invoke()
    })
    viewModel.nameToEditFlow.collectAsEffect { nameToEdit ->
        newFeedName = nameToEdit
    }
    val context = LocalContext.current
    val scrollState = rememberLazyListState()
    SRSSTheme {
        Surface {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    SmallTopAppBar(
                        title = {
                            if (isInEditMode) {
                                BasicTextField(
                                    value = newFeedName ?: "",
                                    onValueChange = {
                                        newFeedName = it
                                        viewModel.checkSaveAvailability(
                                            SpannableStringBuilder.valueOf(
                                                it
                                            )
                                        )
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = MaterialTheme.typography.bodyLarge.color),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = state.title ?: "",
                                    maxLines = 1
                                )
                            }
                        },
                        navigationIcon = {
                            if (isCanNavigateUp) {
                                IconButton(onClick = {
                                    if (isInEditMode) {
                                        viewModel.changeEditMode(isEdit = false)
                                    } else {
                                        navigateToUp.invoke()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = stringResource(id = android.R.string.cancel)
                                    )
                                }
                            }
                        },
                        actions = {
                            if (!isInEditMode) {
                                IconButton(onClick = {
                                    viewModel.changeEditMode(isEdit = true)
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_vector_mode),
                                        contentDescription = stringResource(id = R.string.folder_menu_edit_title)
                                    )
                                }
                            }
                            if (isInEditMode) {
                                IconButton(onClick = {
                                    viewModel.deleteFeed()
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete_outline),
                                        contentDescription = stringResource(id = R.string.option_menu_delete)
                                    )
                                }
                            }
                            if (isInEditMode) {
                                IconButton(onClick = {
                                    viewModel.updateSubscription(newSubscriptionName = newFeedName)
                                }, enabled = state.isAvailableToSave) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_vector_done),
                                        contentDescription = stringResource(id = R.string.option_menu_save)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .background(
                                TopAppBarDefaults.smallTopAppBarColors().containerColor(
                                    scrollFraction = scrollBehavior.scrollFraction
                                ).value
                            )
                            .statusBarsPadding(),
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { padding ->
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LazyColumn(
                        state = scrollState,
                        contentPadding = WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .add(WindowInsets(top = padding.calculateTopPadding()))
                            .asPaddingValues(),
                        content = {
                            items(
                                key = { index -> state.feedContent[index].getItemId() },
                                count = state.feedContent.size,
                            ) { index ->
                                when (val item = state.feedContent[index]) {
                                    is FeedItemModel -> FeedItem(
                                        title = item.title ?: "",
                                        date = item.timestamp?.stringRepresentation,
                                        pictureUrl = item.pictureUrl,
                                        modifier = Modifier.animateItemPlacement(),
                                        onClick = {
                                            openFeed(context, item.url)
                                        }
                                    )
                                    is FeedEmptyModel -> FeedEmptyItem(
                                        icon = item.icon,
                                        message = item.message,
                                        action = item.actionText,
                                        modifier = Modifier.fillParentMaxHeight(),
                                        onClick = {
                                            viewModel.updateFeed()
                                        }
                                    )
                                    is SubscriptionFolderEmptyModel -> FeedEmptyItem(
                                        icon = item.icon,
                                        message = item.message,
                                        action = item.actionText,
                                        modifier = Modifier.fillParentMaxHeight(),
                                        onClick = {
                                            viewModel.updateFeed()
                                        }
                                    )
                                    else -> throw IllegalArgumentException("Unsupported feed model: ${item::class.java.simpleName}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeedItem(
    title: String?,
    date: String?,
    pictureUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    SRSSTheme {
        Box(
            modifier = modifier
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                )
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .clickable {
                    onClick?.invoke()
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(pictureUrl)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_vector_placeholder),
                error = painterResource(R.drawable.ic_vector_placeholder),
                fallback = painterResource(R.drawable.ic_vector_placeholder),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .sizeIn(minHeight = 180.dp, maxHeight = 180.dp)
                    .fillMaxWidth(),
                contentDescription = stringResource(id = R.string.content_description_image)
            )
            if (title != null) {
                Text(
                    text = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
                        .background(
                            color = colorResource(id = R.color.feed_background),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                        .fillMaxWidth()
                )
            }
            if (date != null) {
                Text(
                    text = date,
                    fontSize = 8.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 8.dp)
                        .background(
                            color = colorResource(id = R.color.feed_background),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun FeedEmptyItem(
    icon: Int,
    message: String,
    action: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    SRSSTheme {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(24.dp)
                )
                Text(
                    text = message,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium
                )
                if (action != null) {
                    TextButton(
                        modifier = Modifier
                            .padding(24.dp),
                        onClick = {
                            onClick?.invoke()
                        }
                    ) {
                        Text(
                            text = action,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> Flow<T>.collectAsEffect(
    context: CoroutineContext = EmptyCoroutineContext,
    block: (T) -> Unit
) {
    LaunchedEffect(key1 = Unit) {
        onEach(block).flowOn(context).launchIn(this)
    }
}

fun openFeed(context: Context, url: String?) {
    val colorScheme = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(ContextCompat.getColor(context, R.color.primary_dark))
        .build()
    val customTabsIntent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colorScheme)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .setUrlBarHidingEnabled(true)
        .build()
    customTabsIntent.launchUrl(context, Uri.parse(url))
}