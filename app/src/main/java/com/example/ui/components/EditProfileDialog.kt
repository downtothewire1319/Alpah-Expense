package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.util.ResponsiveModal
import java.io.File

/**
 * Reusable User Avatar component that loads either custom local avatar, Google Photo, or initials
 */
@Composable
fun UserAvatarView(
    photoUrl: String?,
    displayName: String,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        val initials = displayName.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { "U" }

        Surface(
            modifier = modifier,
            color = AccentBlue.copy(alpha = 0.2f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        fontSize = (size.value * 0.4f).sp
                    )
                )
            }
        }
    }
}

/**
 * Dialog allowing user to edit their username and choose an avatar from gallery using Photo Picker
 */
@Composable
fun EditProfileDialog(
    currentName: String,
    currentEmail: String,
    currentPhotoUrl: String?,
    userId: String,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPhotoPath: String?) -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(currentName) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Android Zero-Permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = ResponsiveModal.modalModifier(),
        properties = ResponsiveModal.dialogProperties,
        title = {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar with Camera / Gallery Picker badge
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .testTag("avatar_edit_container"),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    val previewModel: Any? = selectedPhotoUri ?: currentPhotoUrl
                    if (previewModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(previewModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.dp, AccentGold, CircleShape)
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.dp, AccentBlue, CircleShape),
                            color = AccentBlue.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    // Gallery / Photo Picker tap button
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("select_avatar_from_gallery_button"),
                        shape = CircleShape,
                        color = AccentGold,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Pick Avatar from Gallery",
                                tint = DeepNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Tap the gold icon to choose avatar from Gallery",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedSlate)
                )

                // Username / Display Name field
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Username / Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentBlue)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_edit_field")
                )

                // Read-only email info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MutedSlate,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Connected Google Account",
                                style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 11.sp)
                            )
                            Text(
                                text = currentEmail.ifBlank { "No email associated" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank() && !isSaving) {
                        isSaving = true
                        val newPath = if (selectedPhotoUri != null) {
                            saveAvatarFile(context, selectedPhotoUri!!, userId)
                        } else {
                            currentPhotoUrl
                        }
                        onSave(nameInput.trim(), newPath)
                    }
                },
                enabled = nameInput.isNotBlank() && !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Profile")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Copies picked image from gallery to app's internal files directory for permanent local access
 */
private fun saveAvatarFile(context: Context, sourceUri: Uri, userId: String): String? {
    return try {
        val avatarDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
        val targetFile = File(avatarDir, "avatar_${userId.take(16)}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        targetFile.absolutePath
    } catch (e: Exception) {
        sourceUri.toString()
    }
}
