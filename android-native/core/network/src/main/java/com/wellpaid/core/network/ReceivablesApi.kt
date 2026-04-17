package com.wellpaid.core.network

import com.wellpaid.core.model.receivable.ReceivableDto
import com.wellpaid.core.model.receivable.ReceivablesListDto
import com.wellpaid.core.model.receivable.SettleReceivableDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ReceivablesApi {
    @GET("receivables")
    suspend fun listReceivables(): ReceivablesListDto

    @POST("receivables/{id}/settle")
    suspend fun settleReceivable(
        @Path("id") id: String,
        @Body body: SettleReceivableDto,
    ): ReceivableDto
}
