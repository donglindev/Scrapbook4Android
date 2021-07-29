package com.mb.scrapbook.app.stock.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.mb.scrapbook.app.stock.repository.StockRepository
import com.mb.scrapbook.lib.base.mvvm.viewmodel.BaseViewModel
import com.mb.scrapbook.lib.base.network.initiateRequest

class StockViewModel: BaseViewModel<StockRepository>() {

    companion object {
        const val TAG = "StockViewModel"
    }

    private var _stockData: MutableLiveData<String> = MutableLiveData();
    val stockData = _stockData;

    fun loadStockData() {
        initiateRequest({
            Log.d(TAG, "request load stock data on: ${Thread.currentThread().id}")
            stockData.value = repository.loadUrl()
        }, state)
    }
}