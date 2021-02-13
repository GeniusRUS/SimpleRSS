package com.genius.srss.ui.subscriptions

import android.animation.AnimatorSet
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.URLUtil
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.genius.srss.R
import com.genius.srss.databinding.FragmentSubscriptionsBinding
import com.genius.srss.di.DIManager
import com.genius.srss.utils.bindings.viewBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.animator
import com.ub.utils.base.BaseListAdapter
import com.ub.utils.dpToPx
import dev.chrisbanes.insetter.applySystemWindowInsetsToMargin
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
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
    BaseListAdapter.BaseListClickListener<SubscriptionItemModel>, View.OnClickListener,
    ItemTouchCallback.TouchListener {

    private val adapter: SubscriptionsListAdapter by lazy { SubscriptionsListAdapter() }

    @Inject
    lateinit var presenterProvider: Provider<SubscriptionsPresenter>

    private val presenter: SubscriptionsPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        presenterProvider.get()
    }

    private val binding: FragmentSubscriptionsBinding by viewBinding(FragmentSubscriptionsBinding::bind)

    private var activeAnimation: AnimatorSet? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val externalUrl = activity?.intent?.extras?.getString(Intent.EXTRA_TEXT)
            ?: activity?.intent?.clipData?.getItemAt(0)?.text?.toString()
        externalUrl?.let { url ->
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

            activity?.intent?.removeExtra(Intent.EXTRA_TEXT)
            activity?.intent?.clipData = null
        }

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_vector_delete_outline_24px,
            context?.theme
        )?.let { icon ->
            val callback = ItemTouchCallback(
                this,
                icon,
                Color.TRANSPARENT
            )
            ItemTouchHelper(callback).attachToRecyclerView(binding.subscriptionsContent)
        }

        ViewCompat.setTooltipText(binding.addFolder, getString(R.string.subscriptions_add_folder_text))
        ViewCompat.setTooltipText(binding.addSubscription, getString(R.string.subscriptions_add_subscription_text))
        ViewCompat.setTooltipText(binding.fab, getString(R.string.subscriptions_add_text_open))
        binding.addFolder.setOnClickListener(this)
        binding.addSubscription.setOnClickListener(this)
        binding.fab.setOnClickListener(this)
        binding.subscriptionsContent.adapter = adapter
        binding.subscriptionsContent.setHasFixedSize(true)
        adapter.listListener = this

        binding.fab.applySystemWindowInsetsToMargin(
            bottom = true,
            right = true
        )

        binding.subscriptionsContent.applySystemWindowInsetsToPadding(
            left = true,
            top = true,
            right = true,
            bottom = true
        )

        presenter.updateFeed()
    }

    override fun onDestroyView() {
        adapter.listListener = null
        binding.subscriptionsContent.adapter = null
        super.onDestroyView()
    }

    override fun onStateChanged(state: SubscriptionsStateModel) {
        adapter.update(state.feedList)
    }

    override fun onClick(view: View, item: SubscriptionItemModel, position: Int) {
        val direction = SubscriptionsFragmentDirections.actionSubscriptionsFragmentToFeedFragment(
            item.urlToLoad ?: return
        )
        findNavController().navigate(direction)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fab -> {
                if (activeAnimation?.isRunning != true) {
                    binding.fab.rotateImage(toClose = binding.addSubscription.isInvisible)
                    transformFab(toExtend = binding.addSubscription.isInvisible)
                }
            }
            R.id.add_folder -> {

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
                            binding.addSubscription.height + binding.addFolder.height + binding.fab.dpToPx(16)
                        } else {
                            0F
                        },
                        if (toExtend) {
                            0F
                        } else {
                            binding.addSubscription.height + binding.addFolder.height + binding.fab.dpToPx(16)
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
                        ViewCompat.setTooltipText(binding.fab, getString(R.string.subscriptions_add_text_close))
                    }
                }
            } else {
                doOnEnd {
                    if (view != null) {
                        binding.addFolder.isInvisible = true
                        binding.addSubscription.isInvisible = true
                        ViewCompat.setTooltipText(binding.fab, getString(R.string.subscriptions_add_folder_text))
                        activeAnimation = null
                    }
                }
            }
        }.start()
    }

    private fun FloatingActionButton.rotateImage(toClose: Boolean) {
        ViewCompat.animate(this)
            .rotation(if (toClose) 225F else 0F)
            .withLayer()
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
}