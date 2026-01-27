package com.rrajath.hugowriter.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rrajath.hugowriter.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val frontmatterTemplate by viewModel.frontmatterTemplate.collectAsState()
    val githubPat by viewModel.githubPat.collectAsState()
    val githubRepoOwner by viewModel.githubRepoOwner.collectAsState()
    val githubRepoName by viewModel.githubRepoName.collectAsState()
    val githubBranch by viewModel.githubBranch.collectAsState()
    val githubTargetDir by viewModel.githubTargetDir.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isPatVisible by remember { mutableStateOf(false) }
    val frontmatterBringIntoViewRequester = remember { BringIntoViewRequester() }
    val patBringIntoViewRequester = remember { BringIntoViewRequester() }
    val repoOwnerBringIntoViewRequester = remember { BringIntoViewRequester() }
    val repoNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val branchBringIntoViewRequester = remember { BringIntoViewRequester() }
    val targetDirBringIntoViewRequester = remember { BringIntoViewRequester() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge
            )
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dark Mode",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
            }

            Divider()

            // Frontmatter Template Section
            Text(
                text = "Frontmatter Template",
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                value = frontmatterTemplate,
                onValueChange = { viewModel.updateFrontmatterTemplate(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .bringIntoViewRequester(frontmatterBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                frontmatterBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Template") },
                placeholder = { Text("Use {TITLE} and {DATE} as placeholders") },
                supportingText = { Text("Available placeholders: {TITLE}, {DATE}") }
            )

            Divider()

            // GitHub Configuration Section
            Text(
                text = "GitHub Configuration",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = githubPat,
                onValueChange = { viewModel.updateGitHubPat(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(patBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                patBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Personal Access Token") },
                placeholder = { Text("ghp_...") },
                visualTransformation = if (isPatVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { isPatVisible = !isPatVisible }) {
                        Text(
                            text = if (isPatVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                singleLine = true,
                supportingText = { Text("Required for publishing to GitHub") }
            )

            OutlinedTextField(
                value = githubRepoOwner,
                onValueChange = { viewModel.updateGitHubRepoOwner(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(repoOwnerBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                repoOwnerBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Repository Owner") },
                placeholder = { Text("username or organization") },
                singleLine = true
            )

            OutlinedTextField(
                value = githubRepoName,
                onValueChange = { viewModel.updateGitHubRepoName(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(repoNameBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                repoNameBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Repository Name") },
                placeholder = { Text("my-blog") },
                singleLine = true
            )

            OutlinedTextField(
                value = githubBranch,
                onValueChange = { viewModel.updateGitHubBranch(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(branchBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                branchBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Branch") },
                placeholder = { Text("main") },
                singleLine = true
            )

            OutlinedTextField(
                value = githubTargetDir,
                onValueChange = { viewModel.updateGitHubTargetDir(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(targetDirBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                targetDirBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Target Directory") },
                placeholder = { Text("content/posts/") },
                singleLine = true,
                supportingText = { Text("Directory where posts will be saved") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    viewModel.saveSettings()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Settings saved successfully",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "Saving..." else "Save Settings")
            }
        }
    }
}
