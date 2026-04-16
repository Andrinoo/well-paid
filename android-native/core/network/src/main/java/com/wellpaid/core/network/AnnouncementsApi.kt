package com.wellpaid.core.network

import com.wellpaid.core.model.announcement.AnnouncementListDto
import com.wellpaid.core.model.announcement.ApiOkResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AnnouncementsApi {
    @GET("announcements/active")
    suspend fun listActive(
        @Query("placement") placement: String = "home_banner",
        @Query("limit") limit: Int = 5,
    ): AnnouncementListDto

    @POST("announcements/{id}/read")
    suspend fun markRead(@Path("id") id: String): ApiOkResponse

    @POST("announcements/{id}/hide")
    suspend fun hide(@Path("id") id: String): ApiOkResponse
}
