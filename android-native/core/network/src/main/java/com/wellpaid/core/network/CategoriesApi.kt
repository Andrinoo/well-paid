package com.wellpaid.core.network

import com.wellpaid.core.model.expense.CategoryCreateRequest
import com.wellpaid.core.model.expense.CategoryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CategoriesApi {
    @GET("categories")
    suspend fun listCategories(): List<CategoryDto>

    @POST("categories")
    suspend fun createCategory(@Body body: CategoryCreateRequest): CategoryDto
}
