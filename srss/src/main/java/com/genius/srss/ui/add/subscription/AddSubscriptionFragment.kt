package com.genius.srss.ui.add.subscription

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentAddSubscriptionBinding
import com.genius.srss.di.DIManager
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.hideSoftKeyboard
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddSubscriptionFragment : Fragment(R.layout.fragment_add_subscription), View.OnClickListener {

    @Inject
    lateinit var provider: AddSubscriptionViewModelProvider

    private val viewModel: AddSubscriptionViewModel by viewModels {
        DIManager.appComponent.inject(this)
        provider.create(arguments.folderId)
    }

    private val binding: FragmentAddSubscriptionBinding by viewBinding(FragmentAddSubscriptionBinding::bind)

    private val arguments: AddSubscriptionFragmentArgs by navArgs()

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

        binding.rootView.applyInsetter {
            type(ime = true, statusBars = true, navigationBars = true) {
                margin(
                    top = true,
                    bottom = true
                )
            }
            consume(true)
        }

        binding.confirmButton.setOnClickListener(this)
        bindProgressButton(binding.confirmButton)
        binding.confirmButton.attachTextChangeAnimator()
        binding.textField.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.confirmButton.performClick()
                true
            } else false
        }

        if (!arguments.urlToAdd.isNullOrEmpty()) {
            binding.textField.setText(arguments.urlToAdd)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sourceAddedFlow.collect { feedUrl ->
                    feedUrl?.let {
                        val direction = AddSubscriptionFragmentDirections.actionAddFragmentToFeedFragment(it)
                        findNavController().navigate(direction)
                    }
                }
                viewModel.errorFlow.collect { messageId ->
                    messageId?.let {
                        Snackbar.make(binding.rootView, it, Snackbar.LENGTH_LONG).show()
                    }
                }
                viewModel.loadingSourceInfoFlow.collect { loadingState ->
                    binding.confirmButton.isEnabled = !loadingState.isLoading

                    when {
                        loadingState.isLoading -> {
                            binding.confirmButton.showProgress {
                                buttonTextRes = R.string.add_new_subscription_address_checking_process
                                progressColor = Color.WHITE
                            }
                        }
                        loadingState.isAvailableToSave -> {
                            binding.confirmButton.hideProgress(R.string.add_new_subscription_save)
                            hideSoftKeyboard(requireContext())
                        }
                        else -> {
                            binding.confirmButton.hideProgress(R.string.add_new_subscription_check)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        context?.let { hideSoftKeyboard(it) }
        super.onDestroy()
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.confirm_button -> viewModel.checkOrSave(binding.textField.text.toString())
        }
    }
}