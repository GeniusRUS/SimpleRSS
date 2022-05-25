package com.genius.srss.ui.subscriptions

import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import by.kirich1409.viewbindingdelegate.ViewBindingProperty
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentSubscriptionsBinding
import com.genius.srss.di.DIManager
import com.genius.srss.ui.feed.FeedEmptyItem
import com.genius.srss.ui.theme.SRSSTheme
import com.genius.srss.util.DragTarget
import com.genius.srss.util.DropTarget
import com.genius.srss.util.LongPressDraggable
import com.genius.srss.util.MultiFabItem
import com.genius.srss.util.MultiFabState
import com.genius.srss.util.MultiFloatingActionButton
import com.genius.srss.util.Tutorial
import com.genius.srss.util.TutorialView
import com.genius.srss.util.launchAndRepeatWithViewLifecycle
import com.genius.srss.util.pointerInputDetectTransformGestures
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.animator
import com.ub.utils.base.BaseListAdapter
import com.ub.utils.dpToPx
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Provider

class SubscriptionsFragment : Fragment(R.layout.fragment_subscriptions),
    BaseListAdapter.BaseListClickListener<BaseSubscriptionModel>,
    View.OnClickListener, SubscriptionsItemTouchCallback.TouchListener, View.OnTouchListener {

    @Inject
    lateinit var viewModelProvider: Provider<SubscriptionsViewModelFactory>

    private val viewModel: SubscriptionsViewModel by viewModels {
        DIManager.appComponent.inject(this)
        viewModelProvider.get()
    }

    private val adapter: SubscriptionsListAdapter by lazy {
        DIManager.appComponent.inject(this)
        SubscriptionsListAdapter()
    }

    private val bindingDelegate: ViewBindingProperty<SubscriptionsFragment, FragmentSubscriptionsBinding> =
        viewBinding(
            FragmentSubscriptionsBinding::bind
        )
    private val binding: FragmentSubscriptionsBinding by bindingDelegate

    private var activeAnimation: AnimatorSet? = null

    private var scaleDetector: ScaleGestureDetector? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tutorial.rootView = binding.innerContent
        binding.tutorial.setSkipCallback {
            viewModel.onEndTutorial()
        }
        binding.tutorial.setDisplayedTips(
            listOf(
                TutorialView.Tip(R.string.tutorial_pinch_zoom_in, R.drawable.ic_vector_pinch),
                TutorialView.Tip(R.string.tutorial_pinch_zoom_out, R.drawable.ic_vector_pinch),
                TutorialView.Tip(
                    R.string.tutorial_assign_subscription_to_folder,
                    R.drawable.ic_vector_touch_app
                ),
                TutorialView.Tip(
                    R.string.tutorial_create_folder_or_subscription,
                    R.drawable.ic_vector_add_circle_outline
                ),
                TutorialView.Tip(
                    R.string.tutorial_remove_subscription,
                    R.drawable.ic_vector_swipe_left
                ),
                TutorialView.Tip(
                    R.string.tutorial_unlink_subscription,
                    R.drawable.ic_vector_swipe_right
                ),
                TutorialView.Tip(
                    R.string.tutorial_manual_folder_sorting,
                    R.drawable.ic_vector_touch_app
                ),
                TutorialView.Tip(R.string.tutorial_manual_folder_mode, R.drawable.ic_vector_list)
            )
        )

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        scaleDetector = ScaleGestureDetector(
            context,
            object : SimpleOnScaleGestureListener() {
                private var scaleFactor = 1f
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor = detector.scaleFactor
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    if (scaleFactor > 1) {
                        viewModel.updateFeed(isFull = true)
                    } else if (scaleFactor < 1) {
                        viewModel.updateFeed(isFull = false)
                    }
                    super.onScaleEnd(detector)
                }
            }
        )

        binding.subscriptionsContent.setOnTouchListener(this)

        ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_vector_delete_outline,
            context?.theme
        )?.let { icon ->
            val callback = SubscriptionsItemTouchCallback(
                this,
                icon,
                Color.TRANSPARENT
            )
            ItemTouchHelper(callback).attachToRecyclerView(binding.subscriptionsContent)
        }

        ViewCompat.setTooltipText(
            binding.addFolder,
            getString(R.string.subscriptions_add_folder_text)
        )
        ViewCompat.setTooltipText(
            binding.addSubscription,
            getString(R.string.subscriptions_add_subscription_text)
        )
        ViewCompat.setTooltipText(binding.fab, getString(R.string.subscriptions_add_text_open))
        binding.addFolder.setOnClickListener(this)
        binding.addSubscription.setOnClickListener(this)
        binding.fab.setOnClickListener(this)
        binding.subscriptionsContent.adapter = adapter
        binding.subscriptionsContent.setHasFixedSize(true)
        adapter.listListener = this
        adapter.longTouchListener = this
        binding.subscriptionsContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (activeAnimation?.isRunning != true && binding.addSubscription.isVisible) {
                        binding.fab.run {
                            isActivated = false
                            if (isRunning()) {
                                stop()
                            } else {
                                start()
                            }
                        }
                        transformFab(toExtend = false)
                    }
                }
            }
        })
        binding.subscriptionsContent.setOnDragListener { _, event ->
            val offsetFromTop = event.y - binding.subscriptionsContent.paddingTop
            val offsetFromBottom =
                binding.subscriptionsContent.height - binding.subscriptionsContent.paddingBottom - event.y

            if (offsetFromTop in 0F..SCROLL_SLOW_THRESHOLD && binding.subscriptionsContent.canScrollVertically(
                    -1
                )
            ) {
                when (offsetFromTop) {
                    in 0F..SCROLL_FAST_THRESHOLD -> binding.subscriptionsContent.scrollBy(0, -50)
                    in SCROLL_FAST_THRESHOLD..SCROLL_MEDIUM_THRESHOLD -> binding.subscriptionsContent.scrollBy(
                        0,
                        -25
                    )
                    in SCROLL_MEDIUM_THRESHOLD..SCROLL_SLOW_THRESHOLD -> binding.subscriptionsContent.scrollBy(
                        0,
                        -10
                    )
                }
            } else if (offsetFromBottom in 0F..SCROLL_SLOW_THRESHOLD && binding.subscriptionsContent.canScrollVertically(
                    1
                )
            ) {
                when (offsetFromBottom) {
                    in 0F..SCROLL_FAST_THRESHOLD -> binding.subscriptionsContent.scrollBy(0, 50)
                    in SCROLL_FAST_THRESHOLD..SCROLL_MEDIUM_THRESHOLD -> binding.subscriptionsContent.scrollBy(
                        0,
                        25
                    )
                    in SCROLL_MEDIUM_THRESHOLD..SCROLL_SLOW_THRESHOLD -> binding.subscriptionsContent.scrollBy(
                        0,
                        10
                    )
                }
            }

            return@setOnDragListener true
        }
        (binding.subscriptionsContent.layoutManager as GridLayoutManager).spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (adapter.currentList[position] is SubscriptionFolderItemModel) 1 else 2
                }
            }

        binding.fab.applyInsetter {
            type(ime = true, statusBars = true, navigationBars = true) {
                margin(
                    right = true,
                    bottom = true
                )
            }
            consume(false)
        }

        binding.subscriptionsContent.applyInsetter {
            type(ime = true, statusBars = true, navigationBars = true) {
                padding(
                    left = true,
                    top = true,
                    right = true,
                    bottom = true
                )
            }
            consume(false)
        }

        viewModel.updateFeed()

        launchAndRepeatWithViewLifecycle {
            launch {
                viewModel.state.collect { state ->
                    adapter.update(state.feedList)
                }
            }
            launch {
                viewModel.tutorialState.collect { isShowTutorial ->
                    binding.tutorial.isVisible = isShowTutorial
                }
            }
            launch {
                viewModel.errorFlow.collect { errorResId ->
                    Snackbar.make(binding.innerContent, errorResId, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        adapter.listListener = null
        binding.subscriptionsContent.adapter = null
        bindingDelegate.clear()
        super.onDestroyView()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return if (event?.pointerCount?.compareTo(1) == 1) {
            scaleDetector?.onTouchEvent(event) ?: false
        } else v?.onTouchEvent(event) ?: false
    }

    override fun onClick(
        view: View,
        item: BaseSubscriptionModel,
        position: Int,
    ) {
        when (item) {
            is SubscriptionItemModel -> {
                val direction =
                    SubscriptionsFragmentDirections.actionSubscriptionsFragmentToFeedFragment(
                        item.urlToLoad ?: return
                    )
                findNavController().navigate(direction)
            }
            is SubscriptionFolderItemModel -> {
                val direction =
                    SubscriptionsFragmentDirections.actionSubscriptionsFragmentToFolderFragment(
                        item.id
                    )
                findNavController().navigate(direction)
            }
            is SubscriptionFolderEmptyModel -> {
                onClick(binding.addSubscription)
            }
            else -> {
                // no-op
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fab -> {
                if (activeAnimation?.isRunning != true) {
                    binding.fab.run {
                        isActivated = binding.addSubscription.isInvisible
                        if (isRunning()) {
                            stop()
                        } else {
                            start()
                        }
                    }
                    transformFab(toExtend = binding.addSubscription.isInvisible)
                }
            }
            R.id.add_folder -> {
                val direction =
                    SubscriptionsFragmentDirections.actionSubscriptionsFragmentToAddFolderFragment()
                findNavController().navigate(direction)
            }
            R.id.add_subscription -> {
                val direction =
                    SubscriptionsFragmentDirections.actionSubscriptionsFragmentToAddFragment()
                findNavController().navigate(direction)
            }
        }
    }

    override fun onItemDismiss(position: Int) {
        viewModel.removeSubscriptionByPosition(position)
    }

    override fun onDragHolderToPosition(holderPosition: Int, targetPosition: Int) {
        viewModel.handleHolderMove(holderPosition, targetPosition)
    }

    override fun onChangeFolderSort(fromPosition: Int, toPosition: Int) {
        viewModel.handleFolderSortingChange(fromPosition, toPosition)
    }

    private fun transformFab(toExtend: Boolean) {
        activeAnimation?.end()
        val animationList = listOf(
            AnimatorSet().apply {
                interpolator = AccelerateDecelerateInterpolator()
                playTogether(
                    binding.addFolder.animator(
                        View.TRANSLATION_Y,
                        if (toExtend) {
                            binding.addFolder.height + binding.fab.dpToPx(8)
                        } else {
                            0F
                        },
                        if (toExtend) {
                            0F
                        } else {
                            binding.addFolder.height + binding.fab.dpToPx(8)
                        },
                    ),
                    binding.addFolder.animator(
                        View.ALPHA,
                        if (toExtend) 0F else 1F,
                        if (toExtend) 1F else 0F
                    ),
                    binding.addSubscription.animator(
                        View.TRANSLATION_Y,
                        if (toExtend) {
                            binding.addSubscription.height + binding.addFolder.height + binding.fab.dpToPx(
                                16
                            )
                        } else {
                            0F
                        },
                        if (toExtend) {
                            0F
                        } else {
                            binding.addSubscription.height + binding.addFolder.height + binding.fab.dpToPx(
                                16
                            )
                        }
                    ),
                    binding.addSubscription.animator(
                        View.ALPHA,
                        if (toExtend) 0F else 1F,
                        if (toExtend) 1F else 0F
                    )
                )
                duration = 275
            }
        ).run {
            if (!toExtend) {
                asReversed()
            } else this
        }
        AnimatorSet().apply {
            activeAnimation = this
            interpolator = AccelerateDecelerateInterpolator()
            playSequentially(animationList)
            if (toExtend) {
                doOnStart {
                    if (view != null) {
                        binding.addFolder.isVisible = true
                        binding.addSubscription.isVisible = true
                        ViewCompat.setTooltipText(
                            binding.fab,
                            getString(R.string.subscriptions_add_text_close)
                        )
                    }
                }
            } else {
                doOnEnd {
                    if (view != null) {
                        binding.addFolder.isInvisible = true
                        binding.addSubscription.isInvisible = true
                        ViewCompat.setTooltipText(
                            binding.fab,
                            getString(R.string.subscriptions_add_text_open)
                        )
                        activeAnimation = null
                    }
                }
            }
        }.start()
    }

    private fun FloatingActionButton.start() {
        (drawable as? Animatable2Compat)?.start()
    }

    private fun FloatingActionButton.isRunning(): Boolean =
        (drawable as? Animatable2Compat)?.isRunning ?: false

    private fun FloatingActionButton.stop() {
        (drawable as? Animatable2Compat)?.stop()
    }

    private companion object {
        private const val SCROLL_SLOW_THRESHOLD = 250F
        private const val SCROLL_MEDIUM_THRESHOLD = 150F
        private const val SCROLL_FAST_THRESHOLD = 50F
    }
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun SubscriptionScreen(
    navigateToFolder: (String) -> Unit,
    navigateToFeed: (String) -> Unit,
    navigateToAddFolder: () -> Unit,
    navigateToAddSubscription: () -> Unit,
    viewModelInterface: ISubscriptionViewModel,
    state: SubscriptionsStateModel
) {
    var toState by remember { mutableStateOf(MultiFabState.COLLAPSED) }
    var zoom by remember { mutableStateOf(1f) }
    SRSSTheme {
        Surface {
            Scaffold(
                floatingActionButton = {
                    MultiFloatingActionButton(
                        fabIcon = painterResource(id = R.drawable.ic_vector_add),
                        contentDescription = stringResource(id = R.string.subscriptions_add_text_open),
                        items = listOf(
                            MultiFabItem(
                                identifier = "folder",
                                icon = painterResource(id = R.drawable.ic_vector_create_new_folder_black),
                                label = stringResource(id = R.string.subscriptions_add_folder_text)
                            ),
                            MultiFabItem(
                                identifier = "subscription",
                                icon = painterResource(id = R.drawable.ic_vector_rss_feed_black),
                                label = stringResource(id = R.string.subscriptions_add_subscription_text)
                            )
                        ),
                        modifier = Modifier.navigationBarsPadding(),
                        toState = toState,
                        stateChanged = { multiState ->
                            toState = multiState
                        },
                        onFabItemClicked = { fabItem ->
                            when (fabItem.identifier) {
                                "folder" -> {
                                    navigateToAddFolder.invoke()
                                }
                                "subscription" -> {
                                    navigateToAddSubscription.invoke()
                                }
                            }
                        }
                    )
                },
                modifier = Modifier.run {
                    if (state.isTutorialShow) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            blur(10.dp)
                        } else {
                            background(MaterialTheme.colorScheme.background.copy(alpha = 0.5F))
                        }
                    } else this
                }
            ) { padding ->
                Box {
                    LongPressDraggable(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = WindowInsets.systemBars
                                .add(WindowInsets(bottom = 8.dp, top = 8.dp, left = 8.dp, right = 8.dp))
                                .add(WindowInsets(bottom = padding.calculateBottomPadding()))
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxHeight()
                                .pointerInputDetectTransformGestures(
                                    isTransformInProgressChanged = { isInProgress ->
                                        if (!isInProgress) {
                                            if (zoom > 1F) {
                                                viewModelInterface.updateFeed(isFull = true)
                                            } else if (zoom < 1F) {
                                                viewModelInterface.updateFeed(isFull = false)
                                            }
                                            zoom = 1F
                                        }
                                    },
                                    onGesture = { _, _, scale, _ ->
                                        zoom *= scale
                                    }
                                )
                        ) {
                            state.feedList.forEachIndexed { index, model ->
                                when (model) {
                                    is SubscriptionItemModel -> item(
                                        key = model.getItemId(),
                                        span = {
                                            GridItemSpan(maxLineSpan)
                                        }
                                    ) {
                                        val dismissState = rememberDismissState()
                                        if (dismissState.isDismissed(DismissDirection.EndToStart)) {
                                            viewModelInterface.removeSubscriptionByPosition(index)
                                        }
                                        SwipeToDismiss(
                                            directions = setOf(DismissDirection.EndToStart),
                                            state = dismissState,
                                            dismissThresholds = { FractionalThreshold(0.25f) },
                                            modifier = Modifier.animateItemPlacement(),
                                            background = {
                                                val scale by animateFloatAsState(
                                                    if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f
                                                )
                                                Box(
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 20.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_vector_delete_outline),
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
                                                modifier = Modifier.animateItemPlacement(),
                                                onClick = {
                                                    navigateToFeed.invoke(
                                                        model.urlToLoad?.urlEncode()
                                                            ?: return@SubscriptionItem
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    is SubscriptionFolderItemModel -> item(
                                        key = model.getItemId(),
                                    ) {
                                        FolderItem(
                                            id = model.id,
                                            name = model.name,
                                            count = model.countOtOfSources,
                                            modifier = Modifier.animateItemPlacement(),
                                            onClick = { folderId ->
                                                navigateToFolder.invoke(folderId)
                                            },
                                            onAddFeed = { position ->
                                                viewModelInterface.handleHolderMove(
                                                    holderPosition = position,
                                                    targetPosition = index
                                                )
                                            }
                                        )
                                    }
                                    is SubscriptionFolderEmptyModel -> item(
                                        key = model.getItemId(),
                                        span = {
                                            GridItemSpan(maxLineSpan)
                                        }
                                    ) {
                                        FeedEmptyItem(
                                            icon = model.icon,
                                            message = model.message,
                                            action = model.actionText,
                                            onClick = {
                                                navigateToAddSubscription.invoke()
                                            }
                                        )
                                    }
                                    else -> throw IllegalArgumentException("Unsupported feed model: ${model::class.java.simpleName}")
                                }
                            }
                        }
                    }
                }
            }
            if (state.isTutorialShow) {
                Tutorial(
                    toClose = {
                        viewModelInterface.onEndTutorial()
                    },
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun FolderItem(
    id: String,
    name: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
    onAddFeed: (Int) -> Unit
) {
    SRSSTheme {
        DropTarget<Int>(
            modifier = modifier,
        ) { isInBound, position ->
            Card(
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(8.dp),
                border = if (isInBound) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                } else null,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .clickable {
                        onClick.invoke(id)
                    }
            ) {
                position?.let { position ->
                    if (isInBound) {
                        onAddFeed.invoke(position)
                    }
                }
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = name
                    )
                    Text(
                        text = LocalContext.current.resources.getQuantityString(
                            R.plurals.subscriptions_folder_count_template,
                            count,
                            count
                        ),
                        style = TextStyle(
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun SubscriptionItem(
    @PreviewParameter(SubscriptionItemProvider::class) title: String,
    modifier: Modifier = Modifier,
    position: Int? = null,
    onClick: (() -> Unit)? = null
) {
    SRSSTheme {
        DragTarget(
            modifier = modifier,
            dataToDrop = position
        ) {
            Card(
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .clickable {
                        onClick?.invoke()
                    }
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

class SubscriptionItemProvider : PreviewParameterProvider<String> {
    override val values = listOf("First feed", "Second feed").asSequence()
}

fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}

fun String.urlDecode(): String {
    return URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
}