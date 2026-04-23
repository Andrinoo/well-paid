package com.wellpaid.core.network

import com.wellpaid.core.model.investment.InvestmentOverviewDto
import com.wellpaid.core.model.investment.InvestmentEvolutionPointDto
import com.wellpaid.core.model.investment.InvestmentPositionCreateDto
import com.wellpaid.core.model.investment.InvestmentPositionDto
import com.wellpaid.core.model.investment.InvestmentSuggestedRatesDto
import com.wellpaid.core.model.investment.EquityFundamentalsDto
import com.wellpaid.core.model.investment.MacroSnapshotDto
import com.wellpaid.core.model.investment.StockHistoryDto
import com.wellpaid.core.model.investment.StockQuoteDto
import com.wellpaid.core.model.investment.TickerSearchItemDto
import com.wellpaid.core.model.investment.TopMoverItemDto
import retrofit2.Response
import com.wellpaid.core.model.investment.InvestmentPositionAddPrincipalDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @GET("investments/quote/history")
    suspend fun getStockQuoteHistory(
        @Query("symbol") symbol: String,
        @Query("range") range: String,
    ): StockHistoryDto

    @GET("investments/tickers/search")
    suspend fun searchTickers(
        @Query("q") query: String,
        @Query("limit") limit: Int = 12,
    ): List<TickerSearchItemDto>

    @GET("investments/tickers/top-movers")
    suspend fun getTopMovers(
        @Query("window") window: String,
        @Query("limit") limit: Int = 10,
    ): List<TopMoverItemDto>

    @GET("investments/macro/snapshot")
    suspend fun getMacroSnapshot(): MacroSnapshotDto

    @GET("investments/fundamentals")
    suspend fun getEquityFundamentals(
        @Query("symbol") symbol: String,
    ): EquityFundamentalsDto

    @GET("investments/overview")
    suspend fun getOverview(): InvestmentOverviewDto

    @GET("investments/evolution")
    suspend fun getEvolution(@Query("months") months: Int = 6): List<InvestmentEvolutionPointDto>

    @GET("investments/positions")
    suspend fun listPositions(): List<InvestmentPositionDto>

    @POST("investments/positions")
    suspend fun createPosition(@Body body: InvestmentPositionCreateDto): InvestmentPositionDto

    @PATCH("investments/positions/{positionId}")
    suspend fun addPrincipalToPosition(
        @Path("positionId") positionId: String,
        @Body body: InvestmentPositionAddPrincipalDto,
    ): InvestmentPositionDto

    @DELETE("investments/positions/{positionId}")
    suspend fun deletePosition(@Path("positionId") positionId: String): Response<Void>
}
