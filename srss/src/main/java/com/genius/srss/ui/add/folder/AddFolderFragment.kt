package com.genius.srss.ui.add.folder

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.genius.srss.R
import com.genius.srss.databinding.FragmentAddFolderBinding
import com.genius.srss.di.DIManager
import com.genius.srss.ui.feed.collectAsEffect
import com.genius.srss.ui.theme.SRSSTheme
import com.genius.srss.util.launchAndRepeatWithViewLifecycle
import com.google.android.material.snackbar.Snackbar
import dev.chrisbanes.insetter.applyInsetter
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

@Composable
fun AddFolderScreen(
    isCanNavigateUp: Boolean,
    navigateOnAdd: () -> Unit,
    navigateUp: () -> Unit,
    viewModel: AddFolderViewModel = viewModel(
        factory = AddFolderModelFactory(
            DIManager.appComponent.subscriptionDao,
            DIManager.appComponent.generator
        )
    )
) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    var folderName by remember { mutableStateOf<String?>(null) }
    viewModel.folderCreatedFlow.collectAsEffect {
        navigateOnAdd.invoke()
    }
    viewModel.errorFlow.collectAsEffect { error ->
        coroutineScope.launch {
            val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                message = error.toString()
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
                scaffoldState = scaffoldState,
                topBar = {
                    TopAppBar(
                        backgroundColor = MaterialTheme.colors.background,
                        title = {},
                        navigationIcon = if (isCanNavigateUp) {
                            {
                                IconButton(onClick = { navigateUp.invoke() }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        elevation = 0.dp,
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            ) { padding ->
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .imePadding()
                ) {
                    OutlinedTextField(
                        value = folderName ?: "",
                        label = {
                            Text(text = stringResource(id = R.string.add_new_subscription_folder_hint))
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onValueChange = {
                            folderName = it
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.saveFolder(folderName ?: "")
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier
                            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.add_new_subscription_folder_save))
                    }
                }
            }
        }
    }
}