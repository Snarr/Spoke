package com.jacobsnarr.spoke.ui.myrides

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.LoadingState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer

@Composable
fun UnpaidFeesScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: UnpaidFeesViewModel = viewModel(factory = UnpaidFeesViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = "Unpaid Fees",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
        )

        when {
            !state.isLoggedIn -> CenteredMessage("Log in to see unpaid fees.")
            !state.isSupported -> CenteredMessage("Unpaid fees aren't supported for this system.")
            state.isLoading -> LoadingState()
            state.error != null -> CenteredMessage(state.error!!)
            else -> {
                val fees = state.fees
                if (fees == null || fees.totalOwed <= 0.0) {
                    CenteredMessage("No unpaid fees.")
                } else {
                    CenteredMessage("Total owed: $${fees.totalOwed.toInt()}")
                }
            }
        }
    }
}
