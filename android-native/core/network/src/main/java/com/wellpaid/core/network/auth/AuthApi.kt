package com.wellpaid.core.network.auth

import com.wellpaid.core.model.auth.ForgotPasswordRequestDto
import com.wellpaid.core.model.auth.ForgotPasswordResponseDto
import com.wellpaid.core.model.auth.LoginRequestDto
import com.wellpaid.core.model.auth.LogoutRequestDto
import com.wellpaid.core.model.auth.ResetPasswordRequestDto
import com.wellpaid.core.model.auth.MessageResponseDto
import com.wellpaid.core.model.auth.RefreshRequestDto
import com.wellpaid.core.model.auth.RegisterRequestDto
import com.wellpaid.core.model.auth.RegisterResponseDto
import com.wellpaid.core.model.auth.ResendVerificationRequestDto
import com.wellpaid.core.model.auth.ResendVerificationResponseDto
import com.wellpaid.core.model.auth.TokenPairDto
import com.wellpaid.core.model.auth.VerifyEmailRequestDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): RegisterResponseDto

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequestDto): TokenPairDto

    @POST("auth/resend-verification")
    suspend fun resendVerification(
        @Body body: ResendVerificationRequestDto,
    ): ResendVerificationResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenPairDto

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): ForgotPasswordResponseDto

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): MessageResponseDto

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto): MessageResponseDto

    @POST("auth/refresh")
    fun refreshCall(@Body body: RefreshRequestDto): Call<TokenPairDto>

    /** Mesmo endpoint que [refreshCall]; preferir em corrotinas para erros HTTP mapeados (HttpException). */
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenPairDto
}
