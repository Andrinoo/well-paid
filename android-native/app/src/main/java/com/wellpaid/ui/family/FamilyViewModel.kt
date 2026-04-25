package com.wellpaid.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.core.model.family.FamilyInviteCreatedDto
import com.wellpaid.data.FamilyMeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(
    val familyMe: FamilyMeRepository,
) : ViewModel() {

    val family = familyMe.family
    val refreshing = familyMe.refreshing

    fun refresh() {
        viewModelScope.launch { familyMe.refresh() }
    }

    suspend fun createFamily(name: String?): Result<Unit> = familyMe.createFamily(name)

    suspend fun joinFamily(token: String): Result<Unit> = familyMe.joinFamily(token)

    suspend fun renameFamily(name: String): Result<Unit> = familyMe.renameFamily(name)

    suspend fun createInvite(inviteEmail: String? = null): Result<FamilyInviteCreatedDto> =
        familyMe.createInvite(inviteEmail)

    suspend fun removeMember(userId: String): Result<Unit> = familyMe.removeMember(userId)

    suspend fun leaveFamily(): Result<Unit> = familyMe.leaveFamily()
}
