package com.genius.srss.ui.add.folder

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import com.genius.srss.R
import com.genius.srss.databinding.FragmentAddFolderBinding
import com.genius.srss.di.DIManager
import com.genius.srss.utils.bindings.viewBinding
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.Side
import moxy.MvpAppCompatFragment
import moxy.MvpView
import moxy.ktx.moxyPresenter
import moxy.viewstate.strategy.alias.OneExecution
import javax.inject.Inject
import javax.inject.Provider

@OneExecution
interface AddFolderView : MvpView {

}

class AddFolderFragment : MvpAppCompatFragment(R.layout.fragment_add_folder), AddFolderView {

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

        Insetter.builder()
            .applySystemWindowInsetsToPadding(Side.BOTTOM or Side.TOP)
            .consumeSystemWindowInsets(Insetter.CONSUME_AUTO)
            .applyToView(binding.rootView)
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