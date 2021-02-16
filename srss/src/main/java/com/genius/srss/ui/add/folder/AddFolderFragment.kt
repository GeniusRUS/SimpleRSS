package com.genius.srss.ui.add.folder

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import com.genius.srss.R
import com.genius.srss.databinding.FragmentAddFolderBinding
import com.genius.srss.di.DIManager
import com.genius.srss.utils.bindings.viewBinding
import com.google.android.material.snackbar.Snackbar
import dev.chrisbanes.insetter.applyInsetter
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.OneExecution
import javax.inject.Inject
import javax.inject.Provider

@OneExecution
interface AddFolderView : MvpView {
    fun onFolderCreated()
    fun showErrorMessage(@StringRes messageId: Int)
}

class AddFolderFragment : MvpAppCompatFragment(R.layout.fragment_add_folder), AddFolderView, View.OnClickListener {

    @Inject
    lateinit var provider: Provider<AddFolderPresenter>

    private val presenter: AddFolderPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        provider.get()
    }

    private val binding: FragmentAddFolderBinding by viewBinding(FragmentAddFolderBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.let { activity ->
            setHasOptionsMenu(true)
            activity.setSupportActionBar(binding.toolbar)
            activity.supportActionBar?.setDisplayShowTitleEnabled(false)
            activity.supportActionBar?.setDisplayShowHomeEnabled(true)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        binding.confirmButton.setOnClickListener(this)
        binding.textField.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.confirmButton.performClick()
                true
            } else false
        }

        binding.rootView.applyInsetter {
            type(ime = true, statusBars = true, navigationBars = true) {
                margin(
                    top = true,
                    bottom = true
                )
            }
            consume(true)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.confirm_button -> presenter.saveFolder(binding.textField.text.toString())
        }
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

    override fun showErrorMessage(messageId: Int) {
        Snackbar.make(binding.rootView, messageId, Snackbar.LENGTH_LONG).show()
    }

    override fun onFolderCreated() {
        findNavController().popBackStack()
    }
}