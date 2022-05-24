package com.genius.srss.ui.add.subscription

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentAddSubscriptionBinding
import com.genius.srss.di.DIManager
import com.genius.srss.ui.feed.collectAsEffect
import com.genius.srss.ui.subscriptions.urlEncode
import com.genius.srss.ui.theme.SRSSTheme
import com.genius.srss.util.launchAndRepeatWithViewLifecycle
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.snackbar.Snackbar
import com.ub.utils.hideSoftKeyboard
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddSubscriptionFragment : Fragment(R.layout.fragment_add_subscription), View.OnClickListener {

    @Inject
    lateinit var provider: AddSubscriptionViewModelProvider

    private val viewModel: AddSubscriptionViewModel by viewModels {
        DIManager.appComponent.inject(this)
        provider.create(arguments.folderId)
    }

    private val binding: FragmentAddSubscriptionBinding by viewBinding(
        FragmentAddSubscriptionBinding::bind
    )

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

        launchAndRepeatWithViewLifecycle {
            launch {
                viewModel.sourceAddedFlow.collect { feedUrl ->
                    val direction =
                        AddSubscriptionFragmentDirections.actionAddFragmentToFeedFragment(
                            feedUrl
                        )
                    findNavController().navigate(direction)
                }
            }
            launch {
                viewModel.errorFlow.collect { messageId ->
                    Snackbar.make(binding.rootView, messageId, Snackbar.LENGTH_LONG).show()
                }
            }
            launch {
                viewModel.loadingSourceInfoFlow.collect { loadingState ->
                    binding.confirmButton.isEnabled = !loadingState.isLoading

                    when {
                        loadingState.isLoading -> {
                            binding.textField.isEnabled = false
                            binding.confirmButton.showProgress {
                                buttonTextRes =
                                    R.string.add_new_subscription_address_checking_process
                                progressColor =
                                    ContextCompat.getColor(view.context, R.color.button_text_color)
                            }
                        }
                        loadingState.isAvailableToSave -> {
                            binding.textField.isEnabled = false
                            binding.confirmButton.hideProgress(R.string.add_new_subscription_save)
                            hideSoftKeyboard(requireContext())
                        }
                        else -> {
                            binding.textField.isEnabled = true
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

@ExperimentalMaterial3Api
@Composable
fun AddSubscriptionScreen(
    folderId: String?,
    isCanNavigateUp: Boolean,
    navigateOnAdd: (String) -> Unit,
    navigateUp: () -> Unit,
    viewModel: AddSubscriptionViewModel = viewModel(
        factory = AddSubscriptionViewModelFactory(
            folderId,
            DIManager.appComponent.context,
            DIManager.appComponent.network,
            DIManager.appComponent.subscriptionDao
        )
    )
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.loadingSourceInfoFlow.collectAsState()
    var feedUrl by remember { mutableStateOf<String?>(null) }
    viewModel.sourceAddedFlow.collectAsEffect { addedFeedUrl ->
        navigateOnAdd.invoke(addedFeedUrl.urlEncode())
    }
    viewModel.errorFlow.collectAsEffect { error ->
        coroutineScope.launch {
            val snackbarResult = snackbarHostState.showSnackbar(
                message = error
            )
            when (snackbarResult) {
                SnackbarResult.Dismissed -> {}
                SnackbarResult.ActionPerformed -> {}
            }
        }
    }

    SRSSTheme {
        Surface {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    MediumTopAppBar(
                        title = {},
                        navigationIcon = {
                            if (isCanNavigateUp) {
                                IconButton(onClick = { navigateUp.invoke() }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
                modifier = Modifier.navigationBarsPadding()
            ) { paddings ->
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .imePadding(),
                ) {
                    OutlinedTextField(
                        enabled = !state.isAvailableToSave,
                        value = feedUrl ?: "",
                        label = {
                            Text(text = stringResource(id = R.string.add_new_subscription_address_hint))
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Uri
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onValueChange = {
                            feedUrl = it
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                    Button(
                        enabled = !state.isLoading,
                        onClick = {
                            viewModel.checkOrSave(feedUrl ?: "")
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier
                            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.isAvailableToSave) {
                                stringResource(id = R.string.add_new_subscription_save)
                            } else {
                                stringResource(id = R.string.add_new_subscription_check)
                            }
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun AddSubscriptionPreview() {
    SRSSTheme {
        AddSubscriptionScreen(isCanNavigateUp = true, navigateOnAdd = {}, navigateUp = { /*TODO*/ }, folderId = null)
    }
}