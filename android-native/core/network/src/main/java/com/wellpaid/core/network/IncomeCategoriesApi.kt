package com.wellpaid.core.network

import com.wellpaid.core.model.income.IncomeCategoryCreateRequest
import com.wellpaid.core.model.income.IncomeCategoryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IncomeCategoriesApi {
    @GET("income-categories")
    suspend fun listIncomeCategories(): List<IncomeCategoryDto>

    @POST("income-categories")
    suspend fun createIncomeCategory(@Body body: IncomeCategoryCreateRequest): IncomeCategoryDto
}
