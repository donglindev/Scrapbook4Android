package com.mb.scrapbook.app

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import com.mb.scrapbook.app.stock.viewmodel.StockViewModel
import com.mb.scrapbook.lib.base.mvvm.view.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseViewModelActivity<StockViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btnLoad.setOnClickListener {
            mViewModel.loadStockData()
        }
    }

    override fun getLayoutId() = R.layout.activity_main

    override fun initView() {
        super.initView()
    }

    override fun initData() {
    }

    override fun initDataObserver() {
        mViewModel.stockData.observe(this, Observer {
            it?.let { content ->
                Log.d("donglin", "$content")
            }
        })
    }
}