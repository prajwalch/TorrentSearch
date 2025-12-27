package com.prajwalch.torrentsearch.ui.settings.defaultcategory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.utils.categoryStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultCategoryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DefaultCategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(connection = scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            DefaultCategoryScreenTopBar(
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = innerPadding,
        ) {
            items(items = Category.entries, contentType = { it }) {
                CategoryListItem(
                    onClick = { viewModel.setDefaultCategory(it) },
                    selected = it == uiState,
                    name = categoryStringResource(it),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultCategoryScreenTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.settings_default_category)) },
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun CategoryListItem(
    onClick: () -> Unit,
    selected: Boolean,
    name: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = Modifier
            .clickable(
                role = Role.RadioButton,
                onClick = onClick,
            )
            .then(modifier),
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        headlineContent = { Text(text = name) },
    )
}