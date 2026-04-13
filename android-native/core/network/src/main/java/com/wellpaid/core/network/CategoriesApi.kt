package com.wellpaid.core.network

import com.wellpaid.core.model.expense.CategoryDto
import retrofit2.http.GET

interface CategoriesApi {
    @GET("categories")
    suspend fun listCategories(): List<CategoryDto>
}
