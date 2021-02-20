package com.genius.srss.ui.add.subscription

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentAddSubscriptionBinding
import com.genius.srss.di.DIManager
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.hideSoftKeyboard
import dev.chrisbanes.insetter.applyInsetter
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.OneExecution
import javax.inject.Inject
import javax.inject.Provider

@OneExecution
interface AddSubscriptionView : MvpView {
    fun showErrorMessage(@StringRes messageId: Int)
    fun onAvailableToSave(isAvailableToSave: Boolean)
    fun onSourceAdded(feedUrl: String)
}

class AddSubscriptionFragment : MvpAppCompatFragment(R.layout.fragment_add_subscription), AddSubscriptionView, View.OnClickListener {

    @Inject
    lateinit var provider: Provider<AddSubscriptionPresenter>

    private val presenter: AddSubscriptionPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        provider.get()
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
        binding.textField.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.confirmButton.performClick()
                true
            } else false
        }

        if (!arguments.urlToAdd.isNullOrEmpty()) {
            binding.textField.setText(arguments.urlToAdd)
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
            R.id.confirm_button -> presenter.checkOrSave(binding.textField.text.toString())
        }
    }

    override fun onAvailableToSave(isAvailableToSave: Boolean) {
        binding.confirmButton.setText(
            if (isAvailableToSave) {
                R.string.add_new_subscription_save
            } else {
                R.string.add_new_subscription_check
            }
        )
    }

    override fun showErrorMessage(messageId: Int) {
        Snackbar.make(binding.rootView, messageId, Snackbar.LENGTH_LONG).show()
    }

    override fun onSourceAdded(feedUrl: String) {
        val direction = AddSubscriptionFragmentDirections.actionAddFragmentToFeedFragment(feedUrl)
        findNavController().navigate(direction)
    }
}