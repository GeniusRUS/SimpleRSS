package com.genius.srss.ui.add

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.genius.srss.R
import com.genius.srss.databinding.FragmentAddBinding
import com.genius.srss.di.DIManager
import com.genius.srss.utils.bindings.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.hideSoftKeyboard
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.Side
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.OneExecution
import javax.inject.Inject
import javax.inject.Provider

@OneExecution
interface AddView : MvpView {
    fun showErrorMessage(@StringRes messageId: Int)
    fun onAvailableToSave(isAvailableToSave: Boolean)
    fun onSourceAdded(feedUrl: String)
}

class AddFragment : MvpAppCompatFragment(R.layout.fragment_add), AddView, View.OnClickListener {

    @Inject
    lateinit var provider: Provider<AddPresenter>

    private val presenter: AddPresenter by moxyPresenter {
        DIManager.appComponent.inject(this)
        provider.get()
    }

    private val binding: FragmentAddBinding by viewBinding(FragmentAddBinding::bind)

    private val arguments: AddFragmentArgs by navArgs()

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

        Insetter.builder()
            .applySystemWindowInsetsToPadding(Side.BOTTOM or Side.TOP)
            .consumeSystemWindowInsets(Insetter.CONSUME_AUTO)
            .applyToView(binding.rootView)

        binding.confirmButton.setOnClickListener(this)

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
        val direction = AddFragmentDirections.actionAddFragmentToFeedFragment(feedUrl)
        findNavController().navigate(direction)
    }
}