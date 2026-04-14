package com.wellpaid.core.network

import com.wellpaid.core.model.income.IncomeCreateDto
import com.wellpaid.core.model.income.IncomeDto
import com.wellpaid.core.model.income.IncomeUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface IncomesApi {
    @GET("incomes")
    suspend fun listIncomes(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
    ): List<IncomeDto>

    @GET("incomes/{id}")
    suspend fun getIncome(@Path("id") id: String): IncomeDto

    @POST("incomes")
    suspend fun createIncome(@Body body: IncomeCreateDto): IncomeDto

    @PUT("incomes/{id}")
    suspend fun updateIncome(
        @Path("id") id: String,
        @Body body: IncomeUpdateDto,
    ): IncomeDto

    @DELETE("incomes/{id}")
    suspend fun deleteIncome(@Path("id") id: String): Response<Void>
}
