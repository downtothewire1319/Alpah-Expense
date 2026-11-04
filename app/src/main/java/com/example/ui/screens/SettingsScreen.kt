package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.auth.performSignOut
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.UserAvatarView
import androidx.credentials.CredentialManager
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.util.CurrencyHelper
import com.example.ui.util.FinancialInvoiceHelper
import com.example.ui.util.ResponsiveModal
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    coroutineScope: CoroutineScope,
    onSignOutSuccess: () -> Unit,
    onUnlockVault: () -> Unit = {}
) {
    val context = LocalContext.current
    val user = Firebase.auth.currentUser
    val userProfile by viewModel.userProfile.collectAsState()
    val userSecurity by viewModel.userSecurity.collectAsState()
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    val isPrivateUnlocked by viewModel.isPrivateUnlocked.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val publicState by viewModel.publicExpensesState.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showDurationDropdown by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val effectiveName = userProfile?.name?.ifBlank { null } ?: user?.displayName ?: "Google User"
    val effectivePhoto = userProfile?.photoUrl?.ifBlank { null } ?: user?.photoUrl?.toString()

    val autoLockEnabled = userSecurity?.autoLockEnabled ?: true
    val autoLockMinutes = userSecurity?.autoLockMinutes ?: 5

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Card with Gallery Avatar & Edit Username
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditProfileDialog = true }
                        .testTag("user_profile_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatarView(
                            photoUrl = effectivePhoto,
                            displayName = effectiveName,
                            size = 60.dp,
                            onClick = { showEditProfileDialog = true }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = effectiveName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user?.email ?: "No email available",
                                style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                            )
                        }

                        Button(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue.copy(alpha = 0.15f),
                                contentColor = AccentBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Security & Storage
            item {
                Text(
                    text = "Security & Storage",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Discreet Vault Access Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isPrivateUnlocked) {
                                        viewModel.lockPrivateSection()
                                    } else {
                                        onUnlockVault()
                                    }
                                }
                                .padding(vertical = 8.dp)
                                .testTag("discreet_vault_access_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPrivateUnlocked) Icons.Default.LockOpen else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isPrivateUnlocked) AccentGold else AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isPrivateUnlocked) "Close Security Archive" else "Security Archive",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = if (isPrivateUnlocked) "Close active session" else "Secure storage verification",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedSlate)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Change PIN row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChangePinDialog = true }
                                .padding(vertical = 8.dp)
                                .testTag("change_pin_button"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isPinConfigured) "Security PIN" else "Configure PIN",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = if (isPinConfigured) "Change your 4-digit PIN" else "Set a PIN for verification",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedSlate)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Auto-lock switch row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Inactivity Auto-Lock",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Lock session after idle time",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                                    )
                                }
                            }
                            Switch(
                                checked = autoLockEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.updateAutoLockSettings(enabled, autoLockMinutes)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = AccentBlue)
                            )
                        }

                        // Auto-lock duration selector
                        if (autoLockEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDurationDropdown = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Auto-Lock Timeout",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$autoLockMinutes minutes",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedSlate)
                                }
                            }

                            DropdownMenu(
                                expanded = showDurationDropdown,
                                onDismissRequest = { showDurationDropdown = false }
                            ) {
                                listOf(1, 2, 5, 10, 15, 30).forEach { mins ->
                                    DropdownMenuItem(
                                        text = { Text("$mins minutes") },
                                        onClick = {
                                            viewModel.updateAutoLockSettings(autoLockEnabled, mins)
                                            showDurationDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }


            // Sign Out
            item {
                Button(
                    onClick = { showSignOutConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed.copy(alpha = 0.15f),
                        contentColor = AccentRed
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            isPinAlreadySet = isPinConfigured,
            onDismiss = { showChangePinDialog = false },
            onDone = { oldPin, newPin, callback ->
                if (!isPinConfigured) {
                    viewModel.setupNewPin(newPin) { success, err ->
                        callback(success, err)
                    }
                } else {
                    viewModel.changePin(oldPin, newPin) { success, err ->
                        callback(success, err)
                    }
                }
            }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            modifier = ResponsiveModal.modalModifier(isSmall = true),
            properties = ResponsiveModal.dialogProperties,
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out from your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        val credentialManager = CredentialManager.create(context)
                        performSignOut(context, credentialManager, coroutineScope, onSignOutSuccess)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = effectiveName,
            currentEmail = user?.email ?: "",
            currentPhotoUrl = effectivePhoto,
            userId = user?.uid ?: "",
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newPhotoPath ->
                viewModel.updateUserProfile(newName, newPhotoPath) { success ->
                    if (success) {
                        showEditProfileDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun ChangePinDialog(
    isPinAlreadySet: Boolean,
    onDismiss: () -> Unit,
    onDone: (oldPin: String, newPin: String, callback: (Boolean, String?) -> Unit) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = ResponsiveModal.modalModifier(),
        properties = ResponsiveModal.dialogProperties,
        title = { Text(if (isPinAlreadySet) "Change Security PIN" else "Set Security PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPinAlreadySet) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN (4-6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPinAlreadySet && oldPin.isBlank()) {
                        errorMessage = "Please enter current PIN"
                        return@Button
                    }
                    if (newPin.length < 4) {
                        errorMessage = "PIN must be at least 4 digits"
                        return@Button
                    }
                    if (newPin != confirmPin) {
                        errorMessage = "New PIN and confirmation do not match"
                        return@Button
                    }
                    isSubmitting = true
                    errorMessage = null
                    onDone(oldPin, newPin) { success, error ->
                        isSubmitting = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = error ?: "Failed to update PIN"
                        }
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isSubmitting) "Saving..." else "Save PIN")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
