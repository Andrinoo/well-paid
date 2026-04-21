package com.wellpaid.core.network

import com.wellpaid.core.model.investment.InvestmentOverviewDto
import com.wellpaid.core.model.investment.InvestmentEvolutionPointDto
import com.wellpaid.core.model.investment.InvestmentPositionCreateDto
import com.wellpaid.core.model.investment.InvestmentPositionDto
import com.wellpaid.core.model.investment.InvestmentSuggestedRatesDto
import com.wellpaid.core.model.investment.StockQuoteDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Path

interface InvestmentsApi {
    @GET("investments/suggested-rates")
    suspend fun getSuggestedRates(): InvestmentSuggestedRatesDto

    @GET("investments/quote")
    suspend fun getStockQuote(
        @Query("symbol") symbol: String,
    ): StockQuoteDto

    @GET("investments/overview")
    suspend fun getOverview(): InvestmentOverviewDto

    @GET("investments/evolution")
    suspend fun getEvolution(@Query("months") months: Int = 6): List<InvestmentEvolutionPointDto>

    @GET("investments/positions")
    suspend fun listPositions(): List<InvestmentPositionDto>

    @POST("investments/positions")
    suspend fun createPosition(@Body body: InvestmentPositionCreateDto): InvestmentPositionDto

    @DELETE("investments/positions/{positionId}")
    suspend fun deletePosition(@Path("positionId") positionId: String): Response<Void>
}
