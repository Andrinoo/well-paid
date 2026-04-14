package com.wellpaid.ui.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.family.FamilyInviteCreatedDto
import com.wellpaid.core.model.family.FamilyMemberDto
import com.wellpaid.ui.theme.wellPaidCenterTopAppBarColors
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import kotlinx.coroutines.launch

private const val MAX_FAMILY_MEMBERS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyViewModel = hiltViewModel(),
) {
    val family by viewModel.family.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val familyFullMessage = stringResource(R.string.family_error_full)

    var createName by remember { mutableStateOf("") }
    var joinToken by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var inviteResult by remember { mutableStateOf<FamilyInviteCreatedDto?>(null) }
    var memberToRemove by remember { mutableStateOf<FamilyMemberDto?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(family?.id, family?.name) {
        renameText = family?.name.orEmpty()
    }

    val self = family?.members?.find { it.isSelf }
    val isOwner = self?.role == "owner"
    val memberCount = family?.members?.size ?: 0
    val roomForInvite = memberCount < MAX_FAMILY_MEMBERS

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidCenterTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.family_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !busy) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !busy && !refreshing,
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.home_refresh),
                            tint = Color.White,
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .wellPaidScreenHorizontalPadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            if (refreshing && family == null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            errorText?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (family == null) {
                Text(
                    text = stringResource(R.string.family_intro_solo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.family_section_create),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    label = { Text(stringResource(R.string.family_name_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            errorText = null
                            val r = viewModel.createFamily(createName.trim().takeIf { it.isNotEmpty() })
                            r.onFailure { errorText = it.message }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.family_create_action))
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.family_section_join),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = joinToken,
                    onValueChange = { joinToken = it },
                    label = { Text(stringResource(R.string.family_invite_token_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    minLines = 2,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            errorText = null
                            val r = viewModel.joinFamily(joinToken)
                            r.onSuccess { joinToken = "" }
                            r.onFailure { errorText = it.message }
                            busy = false
                        }
                    },
                    enabled = !busy && joinToken.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.family_join_action))
                }
            } else {
                val fam = family!!
                Text(
                    text = fam.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.family_member_count, memberCount, MAX_FAMILY_MEMBERS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                if (isOwner) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.family_rename_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val name = renameText.trim()
                            if (name.isEmpty()) return@OutlinedButton
                            scope.launch {
                                busy = true
                                errorText = null
                                val r = viewModel.renameFamily(name)
                                r.onFailure { errorText = it.message }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.family_rename_save))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!roomForInvite) {
                                errorText = familyFullMessage
                                return@Button
                            }
                            scope.launch {
                                busy = true
                                errorText = null
                                val r = viewModel.createInvite()
                                r.onSuccess { inviteResult = it }
                                r.onFailure { errorText = it.message }
                                busy = false
                            }
                        },
                        enabled = !busy && roomForInvite,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.family_invite_create))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.family_members_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                fam.members.forEach { m ->
                    MemberRow(
                        member = m,
                        isOwner = isOwner,
                        onRemove = { memberToRemove = m },
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showLeaveConfirm = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.family_leave_action))
                }
            }

            if (busy) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
            }
        }
    }

    inviteResult?.let { inv ->
        AlertDialog(
            onDismissRequest = { inviteResult = null },
            title = { Text(stringResource(R.string.family_invite_created_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.family_invite_expires, inv.expiresAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = inv.inviteUrl,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(inv.token))
                        },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                            Text(stringResource(R.string.family_copy_token))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inviteResult = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }

    memberToRemove?.let { m ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(stringResource(R.string.family_remove_title)) },
            text = { Text(stringResource(R.string.family_remove_body, m.email)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = m.userId
                        memberToRemove = null
                        scope.launch {
                            busy = true
                            errorText = null
                            val r = viewModel.removeMember(id)
                            r.onFailure { errorText = it.message }
                            busy = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.family_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(stringResource(R.string.family_leave_title)) },
            text = { Text(stringResource(R.string.family_leave_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirm = false
                        scope.launch {
                            busy = true
                            errorText = null
                            val r = viewModel.leaveFamily()
                            r.onFailure { errorText = it.message }
                            busy = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.family_leave_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun MemberRow(
    member: FamilyMemberDto,
    isOwner: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = member.fullName?.takeIf { it.isNotBlank() } ?: member.email,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (member.role == "owner") {
                    stringResource(R.string.family_role_owner)
                } else {
                    stringResource(R.string.family_role_member)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (isOwner && !member.isSelf) {
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.family_remove_short))
            }
        }
    }
}
