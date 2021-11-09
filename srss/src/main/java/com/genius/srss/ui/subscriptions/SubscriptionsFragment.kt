package com.genius.srss.ui.subscriptions

import android.animation.AnimatorSet
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
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
import com.genius.srss.di.services.converters.IConverters
import com.genius.srss.util.TutorialView
import com.genius.srss.util.launchAndRepeatWithViewLifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ub.utils.animator
import com.ub.utils.base.BaseListAdapter
import com.ub.utils.dpToPx
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class SubscriptionsFragment : Fragment(R.layout.fragment_subscriptions),
    BaseListAdapter.BaseListClickListener<BaseSubscriptionModel>,
    View.OnClickListener, SubscriptionsItemTouchCallback.TouchListener, View.OnTouchListener {

    @Inject
    lateinit var convertersProvider: Provider<IConverters>

    @Inject
    lateinit var viewModelProvider: Provider<SubscriptionsViewModelFactory>

    private val viewModel: SubscriptionsViewModel by viewModels {
        DIManager.appComponent.inject(this)
        viewModelProvider.get()
    }

    private val adapter: SubscriptionsListAdapter by lazy {
        DIManager.appComponent.inject(this)
        SubscriptionsListAdapter(convertersProvider.get())
    }

    private val bindingDelegate: ViewBindingProperty<SubscriptionsFragment, FragmentSubscriptionsBinding> = viewBinding(
        FragmentSubscriptionsBinding::bind
    )
    private val binding: FragmentSubscriptionsBinding by bindingDelegate

    private var activeAnimation: AnimatorSet? = null

    private var scaleDetector: ScaleGestureDetector? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tutorial.rootView = binding.contentView
        binding.tutorial.setSkipCallback {
            viewModel.skipTutorial()
        }
        binding.tutorial.setDisplayedTips(
            listOf(
                TutorialView.Tip(R.string.tutorial_pinch_zoom_in, R.drawable.ic_vector_pinch),
                TutorialView.Tip(R.string.tutorial_pinch_zoom_out, R.drawable.ic_vector_pinch),
                TutorialView.Tip(R.string.tutorial_assign_subscription_to_folder, R.drawable.ic_vector_touch_app),
                TutorialView.Tip(R.string.tutorial_create_folder_or_subscription, R.drawable.ic_vector_add_circle_outline),
                TutorialView.Tip(R.string.tutorial_remove_subscription, R.drawable.ic_vector_swipe_left),
                TutorialView.Tip(R.string.tutorial_unlink_subscription, R.drawable.ic_vector_swipe_right),
                TutorialView.Tip(R.string.tutorial_manual_folder_sorting, R.drawable.ic_vector_touch_app),
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
            val offsetFromBottom = binding.subscriptionsContent.height - binding.subscriptionsContent.paddingBottom - event.y

            if (offsetFromTop in 0F..SCROLL_SLOW_THRESHOLD && binding.subscriptionsContent.canScrollVertically(-1)) {
                when (offsetFromTop) {
                    in 0F..SCROLL_FAST_THRESHOLD -> binding.subscriptionsContent.scrollBy(0, -50)
                    in SCROLL_FAST_THRESHOLD..SCROLL_MEDIUM_THRESHOLD -> binding.subscriptionsContent.scrollBy(0, -25)
                    in SCROLL_MEDIUM_THRESHOLD..SCROLL_SLOW_THRESHOLD -> binding.subscriptionsContent.scrollBy(0, -10)
                }
            } else if (offsetFromBottom in 0F..SCROLL_SLOW_THRESHOLD && binding.subscriptionsContent.canScrollVertically(1)) {
                when (offsetFromBottom) {
                    in 0F..SCROLL_FAST_THRESHOLD -> binding.subscriptionsContent.scrollBy(0, 50)
                    in SCROLL_FAST_THRESHOLD..SCROLL_MEDIUM_THRESHOLD -> binding.subscriptionsContent.scrollBy(0, 25)
                    in SCROLL_MEDIUM_THRESHOLD..SCROLL_SLOW_THRESHOLD -> binding.subscriptionsContent.scrollBy(0, 10)
                }
            }

            return@setOnDragListener true
        }
        (binding.subscriptionsContent.layoutManager as GridLayoutManager).spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
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
        }
    }

    override fun onDestroyView() {
        adapter.listListener = null
        binding.subscriptionsContent.adapter = null
        bindingDelegate.clear()
        super.onDestroyView()
    }

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
                val direction = SubscriptionsFragmentDirections.actionSubscriptionsFragmentToFeedFragment(
                    item.urlToLoad ?: return
                )
                findNavController().navigate(direction)
            }
            is SubscriptionFolderItemModel -> {
                val direction = SubscriptionsFragmentDirections.actionSubscriptionsFragmentToFolderFragment(
                    item.id
                )
                findNavController().navigate(direction)
            }
            is SubscriptionFolderEmptyModel -> {
                onClick(binding.addSubscription)
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