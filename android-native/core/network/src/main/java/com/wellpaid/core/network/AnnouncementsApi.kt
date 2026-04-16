package com.wellpaid.core.network

import com.wellpaid.core.model.announcement.AnnouncementListDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AnnouncementsApi {
    @GET("announcements/active")
    suspend fun listActive(
        @Query("placement") placement: String = "home_banner",
        @Query("limit") limit: Int = 5,
    ): AnnouncementListDto
}
