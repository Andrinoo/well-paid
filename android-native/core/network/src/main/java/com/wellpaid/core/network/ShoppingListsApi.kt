package com.wellpaid.core.network

import com.wellpaid.core.model.shopping.ShoppingListCompleteDto
import com.wellpaid.core.model.shopping.ShoppingListCreateDto
import com.wellpaid.core.model.shopping.ShoppingListDetailDto
import com.wellpaid.core.model.shopping.ShoppingListItemCreateDto
import com.wellpaid.core.model.shopping.ShoppingListPatchDto
import com.wellpaid.core.model.shopping.ShoppingListSummaryDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ShoppingListsApi {
    @GET("shopping-lists")
    suspend fun listShoppingLists(): List<ShoppingListSummaryDto>

    @POST("shopping-lists")
    suspend fun createShoppingList(@Body body: ShoppingListCreateDto): ShoppingListDetailDto

    @GET("shopping-lists/{id}")
    suspend fun getShoppingList(@Path("id") id: String): ShoppingListDetailDto

    @PATCH("shopping-lists/{id}")
    suspend fun patchShoppingList(
        @Path("id") id: String,
        @Body body: ShoppingListPatchDto,
    ): ShoppingListDetailDto

    @DELETE("shopping-lists/{id}")
    suspend fun deleteShoppingList(@Path("id") id: String): Response<Unit>

    @POST("shopping-lists/{listId}/items")
    suspend fun addShoppingListItem(
        @Path("listId") listId: String,
        @Body body: ShoppingListItemCreateDto,
    ): ShoppingListDetailDto

    @PATCH("shopping-lists/{listId}/items/{itemId}")
    suspend fun patchShoppingListItem(
        @Path("listId") listId: String,
        @Path("itemId") itemId: String,
        @Body body: JsonObject,
    ): ShoppingListDetailDto

    @DELETE("shopping-lists/{listId}/items/{itemId}")
    suspend fun deleteShoppingListItem(
        @Path("listId") listId: String,
        @Path("itemId") itemId: String,
    ): ShoppingListDetailDto

    @POST("shopping-lists/{listId}/complete")
    suspend fun completeShoppingList(
        @Path("listId") listId: String,
        @Body body: ShoppingListCompleteDto,
    ): ShoppingListDetailDto
}
