package com.genius.srss.ui.folder

import android.os.Bundle
import android.graphics.Color
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentFolderBinding
import com.genius.srss.di.DIManager
import com.genius.srss.ui.subscriptions.*
import com.ub.utils.base.BaseListAdapter
import com.ub.utils.hideSoftKeyboard
import com.ub.utils.openSoftKeyboard
import dev.chrisbanes.insetter.applyInsetter
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.AddToEndSingle
import javax.inject.Inject

@AddToEndSingle
interface FolderView : MvpView {
    fun onStateChanged(state: FolderStateModel)
    fun onUpdateNameToEdit(nameToEdit: String?)
    fun onScreenClose()
}

class FolderFragment : MvpAppCompatFragment(R.layout.fragment_folder), FolderView,
    BaseListAdapter.BaseListClickListener<BaseSubscriptionModel>, FolderTouchHelperCallback.TouchFolderListener {

    @Inject
    lateinit var provider: FolderPresenterFactory

    private val presenter: FolderPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        provider.create(arguments.folderId)
    }

    private val adapter: SubscriptionsListAdapter by lazy { SubscriptionsListAdapter() }

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

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        binding.collapsingToolbar.isTitleEnabled = false
        binding.folderContent.adapter = adapter
        binding.folderContent.setHasFixedSize(true)
        adapter.listListener = this

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
                if (presenter.isInEditMode) {
                    presenter.changeEditMode(isEdit = false)
                } else {
                    findNavController().popBackStack()
                }
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStateChanged(state: FolderStateModel) {
        adapter.update(state.feedList)
        (activity as? AppCompatActivity)?.supportActionBar?.title = state.title ?: ""

        menu?.findItem(R.id.option_delete)?.isVisible = state.isInEditMode
        menu?.findItem(R.id.option_save)?.isVisible = state.isInEditMode
        menu?.findItem(R.id.option_edit)?.isVisible = !state.isInEditMode
        binding.updateNameField.isGone = !state.isInEditMode
        binding.folderContent.isGone = state.isInEditMode

        if (state.isInEditMode) {
            menu?.findItem(R.id.option_save)?.isEnabled = state.isAvailableToSave
            openSoftKeyboard(context ?: return, binding.updateNameField)
        } else {
            hideSoftKeyboard(context ?: return)
        }
    }

    override fun onUpdateNameToEdit(nameToEdit: String?) {
        binding.updateNameField.setText(nameToEdit)
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

    override fun onFolderDismiss(position: Int) {
        presenter.unlinkFolderByPosition(position)
    }

    override fun onScreenClose() {
        findNavController().popBackStack()
    }
}