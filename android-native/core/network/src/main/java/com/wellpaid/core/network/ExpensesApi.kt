package com.wellpaid.core.network

import com.wellpaid.core.model.expense.ExpenseCreateDto
import com.wellpaid.core.model.expense.ExpenseCreateOutcomeDto
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.core.model.expense.ExpensePayDto
import com.wellpaid.core.model.expense.ExpenseUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ExpensesApi {
    @GET("expenses")
    suspend fun listExpenses(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
        @Query("category_id") categoryId: String? = null,
        @Query("status") status: String? = null,
        @Query("installment_group_id") installmentGroupId: String? = null,
    ): List<ExpenseDto>

    @GET("expenses/{id}")
    suspend fun getExpense(@Path("id") id: String): ExpenseDto

    @POST("expenses")
    suspend fun createExpense(@Body body: ExpenseCreateDto): ExpenseCreateOutcomeDto

    @PUT("expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: String,
        @Body body: ExpenseUpdateDto,
    ): ExpenseDto

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(
        @Path("id") id: String,
        @Query("delete_target") deleteTarget: String? = null,
        @Query("delete_scope") deleteScope: String? = null,
        @Query("confirm_delete_paid") confirmDeletePaid: Boolean? = null,
    ): Response<Void>

    @POST("expenses/{id}/pay")
    suspend fun payExpense(
        @Path("id") id: String,
        @Body body: ExpensePayDto = ExpensePayDto(),
    ): ExpenseDto
}
