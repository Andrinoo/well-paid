package com.wellpaid.data

import android.content.Context
import com.wellpaid.core.model.family.FamilyCreateDto
import com.wellpaid.core.model.family.FamilyInviteCreateRequestDto
import com.wellpaid.core.model.family.FamilyInviteCreatedDto
import com.wellpaid.core.model.family.FamilyJoinRequestDto
import com.wellpaid.core.model.family.FamilyMemberDto
import com.wellpaid.core.model.family.FamilyOutDto
import com.wellpaid.core.model.family.FamilyPendingInviteDto
import com.wellpaid.core.model.family.FamilyUpdateDto
import com.wellpaid.core.network.FamiliesApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyMeRepository @Inject constructor(
    private val familiesApi: FamiliesApi,
    @ApplicationContext private val appContext: Context,
) {

    private val _family = MutableStateFlow<FamilyOutDto?>(null)
    val family: StateFlow<FamilyOutDto?> = _family.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun clear() {
        _family.value = null
    }

    suspend fun refresh() {
        _refreshing.value = true
        runCatching { familiesApi.getMe() }
            .onSuccess { resp -> _family.value = resp.family }
            .onFailure { _family.value = null }
        _refreshing.value = false
    }

    fun isFamilyOwnerOrSolo(): Boolean {
        val f = _family.value ?: return true
        val self = f.members.find { it.isSelf } ?: return false
        return self.role == "owner"
    }

    fun canShareExpense(): Boolean {
        val f = _family.value ?: return false
        return f.members.size >= 2
    }

    fun peerMembersExcludingSelf(): List<FamilyMemberDto> {
        val f = _family.value ?: return emptyList()
        return f.members.filter { !it.isSelf }
    }

    suspend fun createFamily(name: String?): Result<Unit> =
        apiCall {
            familiesApi.createFamily(FamilyCreateDto(name = name?.trim()?.takeIf { it.isNotEmpty() }))
            refresh()
        }

    suspend fun joinFamily(token: String): Result<Unit> =
        apiCall {
            familiesApi.joinFamily(FamilyJoinRequestDto(token = token.trim()))
            refresh()
        }

    suspend fun renameFamily(name: String): Result<Unit> =
        apiCall {
            familiesApi.updateFamily(FamilyUpdateDto(name = name.trim()))
            refresh()
        }

    suspend fun createInvite(inviteEmail: String? = null): Result<FamilyInviteCreatedDto> =
        apiCallData {
            familiesApi.createInvite(FamilyInviteCreateRequestDto(inviteEmail?.trim()?.takeIf { it.isNotEmpty() }))
        }

    suspend fun listPendingInvites(): Result<List<FamilyPendingInviteDto>> =
        apiCallData { familiesApi.listPendingInvites() }

    suspend fun removeMember(userId: String): Result<Unit> =
        apiCall {
            val r = familiesApi.removeMember(userId)
            if (!r.isSuccessful) error("HTTP ${r.code()}")
            refresh()
        }

    suspend fun leaveFamily(): Result<Unit> =
        apiCall {
            val r = familiesApi.leaveFamily()
            if (!r.isSuccessful) error("HTTP ${r.code()}")
            refresh()
        }

    private suspend fun apiCall(block: suspend () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(Exception(FastApiErrorMapper.message(appContext, t)))
        }

    private suspend fun <T> apiCallData(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (t: Throwable) {
            Result.failure(Exception(FastApiErrorMapper.message(appContext, t)))
        }
}
