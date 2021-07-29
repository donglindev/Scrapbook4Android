package com.mb.scrapbook.app.stock.api

import com.mb.scrapbook.lib.base.network.response.BaseResponse
import okhttp3.Response
import retrofit2.http.GET

interface ApiStocks {

    @GET("/")
    suspend fun loadStock(): BaseResponse<String>
}