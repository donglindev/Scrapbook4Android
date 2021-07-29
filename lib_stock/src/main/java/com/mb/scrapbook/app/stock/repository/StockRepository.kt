package com.mb.scrapbook.app.stock.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.mb.scrapbook.app.stock.api.ApiStocks
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.mvvm.repository.BaseRepository
import com.mb.scrapbook.lib.base.network.RetrofitFactory
import com.mb.scrapbook.lib.base.network.dataConvert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockRepository(private val state: MutableLiveData<State>): BaseRepository() {

    companion object {
        const val TAG = "StockRepository"
        const val URL_STOCK = "https://www.baidu.com";
    }

    private val api: ApiStocks by lazy {
        RetrofitFactory.instance.create(URL_STOCK, ApiStocks::class.java)
    }

    suspend fun loadUrl(): String {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "request load url on: ${Thread.currentThread().id}")
            api.loadStock().dataConvert(state)
        }
    }
}