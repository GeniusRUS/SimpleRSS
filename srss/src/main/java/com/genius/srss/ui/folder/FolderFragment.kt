package com.genius.srss.ui.folder

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.genius.srss.R
import com.genius.srss.databinding.FragmentFolderBinding
import com.genius.srss.di.DIManager
import com.genius.srss.ui.subscriptions.*
import com.genius.srss.utils.bindings.viewBinding
import com.ub.utils.base.BaseListAdapter
import dev.chrisbanes.insetter.applyInsetter
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.AddToEndSingle
import javax.inject.Inject
import javax.inject.Provider

@AddToEndSingle
interface FolderView : MvpView {
    fun onStateChanged(state: FolderStateModel)
}

class FolderFragment : MvpAppCompatFragment(R.layout.fragment_folder), FolderView,
    BaseListAdapter.BaseListClickListener<BaseSubscriptionModel> {

    @Inject
    lateinit var provider: Provider<FolderPresenter>

    private val presenter: FolderPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        provider.get()
    }

    private val adapter: SubscriptionsListAdapter by lazy { SubscriptionsListAdapter() }

    private val binding: FragmentFolderBinding by viewBinding(FragmentFolderBinding::bind)

    private val arguments: FolderFragmentArgs by navArgs()

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

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        binding.collapsingToolbar.isTitleEnabled = false
        binding.folderContent.adapter = adapter
        binding.folderContent.setHasFixedSize(true)
        adapter.listListener = this

        binding.folderContent.applyInsetter {
            type(ime = true, statusBars = true, navigationBars = true) {
                padding(
                    left = true,
                    right = true,
                    bottom = true
                )
            }
            consume(false)
        }
        binding.collapsingToolbar.applyInsetter {
            type(ime = true, statusBars = true, navigationBars = true) {
                padding(
                    left = true,
                    top = true,
                    right = true
                )
            }
            consume(false)
        }

        presenter.updateFolderFeed(arguments.folderId)
    }

    override fun onDestroyView() {
        adapter.listListener = null
        binding.folderContent.adapter = null
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStateChanged(state: FolderStateModel) {
        adapter.update(state.feedList)
        (activity as? AppCompatActivity)?.supportActionBar?.title = state.title ?: ""
    }

    override fun onClick(view: View, item: BaseSubscriptionModel, position: Int) {
        if (item is SubscriptionItemModel) {
            val direction = FolderFragmentDirections.actionFolderFragmentToFeedFragment(
                item.urlToLoad ?: return
            )
            findNavController().navigate(direction)
        } else if (item is SubscriptionFolderItemModel) {

        }
    }
}