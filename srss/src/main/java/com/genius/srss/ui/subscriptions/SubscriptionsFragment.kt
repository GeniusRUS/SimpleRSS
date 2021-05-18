package com.genius.srss.ui.subscriptions

import android.animation.AnimatorSet
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.URLUtil
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.animator
import com.ub.utils.base.BaseListAdapter
import com.ub.utils.dpToPx
import dev.chrisbanes.insetter.applyInsetter
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.AddToEndSingle
import javax.inject.Inject
import javax.inject.Provider

@AddToEndSingle
interface SubscriptionsView : MvpView {
    fun onStateChanged(state: SubscriptionsStateModel)
}

class SubscriptionsFragment : MvpAppCompatFragment(R.layout.fragment_subscriptions),
    SubscriptionsView,
    BaseListAdapter.BaseListClickListener<BaseSubscriptionModel>, View.OnClickListener,
    SubscriptionsItemTouchCallback.TouchListener, View.OnTouchListener {

    private val adapter: SubscriptionsListAdapter by lazy { SubscriptionsListAdapter() }

    @Inject
    lateinit var presenterProvider: Provider<SubscriptionsPresenter>

    private val presenter: SubscriptionsPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        presenterProvider.get()
    }

    private val bindingDelegate: ViewBindingProperty<SubscriptionsFragment, FragmentSubscriptionsBinding> = viewBinding(
        FragmentSubscriptionsBinding::bind
    )
    private val binding: FragmentSubscriptionsBinding by bindingDelegate

    private var activeAnimation: AnimatorSet? = null

    private var scaleDetector: ScaleGestureDetector? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val externalUrl = activity?.intent?.extras?.getString(Intent.EXTRA_TEXT)
            ?: activity?.intent?.clipData?.getItemAt(0)?.text?.toString()
        externalUrl?.let { url ->
            handleUrlFromExternalSource(url)
            activity?.intent?.removeExtra(Intent.EXTRA_TEXT)
            activity?.intent?.clipData = null
        }

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
                        presenter.updateFeed(isFull = true)
                    } else if (scaleFactor < 1) {
                        presenter.updateFeed(isFull = false)
                    }
                    super.onScaleEnd(detector)
                }
            }
        )

        binding.subscriptionsContent.setOnTouchListener(this)

        ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_vector_delete_outline_24px,
            context?.theme
        )?.let { icon ->
            val callback = SubscriptionsItemTouchCallback(
                this,
                icon,
                Color.TRANSPARENT,
                listOf(SubscriptionsListAdapter.SubscriptionFolderViewHolder::class)
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
        adapter.touchListener = this
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

        presenter.updateFeed()
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

    override fun onStateChanged(state: SubscriptionsStateModel) {
        adapter.update(state.feedList)
    }

    override fun onClick(view: View, item: BaseSubscriptionModel, position: Int) {
        if (item is SubscriptionItemModel) {
            val direction = SubscriptionsFragmentDirections.actionSubscriptionsFragmentToFeedFragment(
                item.urlToLoad ?: return
            )
            findNavController().navigate(direction)
        } else if (item is SubscriptionFolderItemModel) {
            val direction = SubscriptionsFragmentDirections.actionSubscriptionsFragmentToFolderFragment(
                item.id
            )
            findNavController().navigate(direction)
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
                    SubscriptionsFragmentDirections.actionSubscriptionsFragmentToAddFragment(
                        null
                    )
                findNavController().navigate(direction)
            }
        }
    }

    override fun onItemDismiss(position: Int) {
        presenter.removeSubscriptionByPosition(position)
    }

    override fun onDragHolderToPosition(holderPosition: Int, targetPosition: Int) {
        presenter.handleHolderMove(holderPosition, targetPosition)
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

    private fun handleUrlFromExternalSource(url: String) {
        if (URLUtil.isValidUrl(url)) {
            val direction =
                SubscriptionsFragmentDirections.actionSubscriptionsFragmentToAddFragment(url)
            findNavController().navigate(direction)
        } else {
            Snackbar.make(
                binding.rootView,
                R.string.error_illegal_argument_url,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun FloatingActionButton.start() {
        (drawable as? Animatable2Compat)?.start()
    }

    private fun FloatingActionButton.isRunning(): Boolean =
        (drawable as? Animatable2Compat)?.isRunning ?: false

    private fun FloatingActionButton.stop() {
        (drawable as? Animatable2Compat)?.stop()
    }
}