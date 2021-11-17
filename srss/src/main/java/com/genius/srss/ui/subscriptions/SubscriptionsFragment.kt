package com.genius.srss.ui.subscriptions

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.genius.srss.R
import com.genius.srss.databinding.FragmentSubscriptionsBinding
import com.genius.srss.di.DIManager
import com.genius.srss.utils.bindings.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.base.BaseListAdapter
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
}