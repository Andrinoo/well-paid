package com.wellpaid.core.network

import com.wellpaid.core.model.income.IncomeCategoryDto
import retrofit2.http.GET

interface IncomeCategoriesApi {
    @GET("income-categories")
    suspend fun listIncomeCategories(): List<IncomeCategoryDto>
}
