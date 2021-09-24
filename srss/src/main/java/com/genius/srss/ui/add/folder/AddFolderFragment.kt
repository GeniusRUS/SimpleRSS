package com.genius.srss.ui.add.folder

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentAddFolderBinding
import com.genius.srss.di.DIManager
import com.genius.srss.util.launchAndRepeatWithViewLifecycle
import com.google.android.material.snackbar.Snackbar
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class AddFolderFragment : Fragment(R.layout.fragment_add_folder), View.OnClickListener {

    @Inject
    lateinit var provider: Provider<AddFolderModelFactory>

    private val viewModel: AddFolderViewModel by viewModels {
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

        launchAndRepeatWithViewLifecycle {
            launch {
                viewModel.folderCreatedFlow.collect {
                    findNavController().popBackStack()
                }
            }
            launch {
                viewModel.errorFlow.collect { messageId ->
                    Snackbar.make(binding.rootView, messageId, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.confirm_button -> viewModel.saveFolder(binding.textField.text.toString())
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
}