package com.mb.scrapbook.app

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.lifecycle.Observer
import com.mb.scrapbook.app.stock.viewmodel.StockViewModel
import com.mb.scrapbook.lib.base.mvvm.view.BaseViewModelActivity

class MainActivity : BaseViewModelActivity<StockViewModel>() {

    /** 按钮组件 */
    private val btnLoad by lazy { findViewById<Button>(R.id.btnLoad) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btnLoad.setOnClickListener {
            mViewModel.loadStockData()
        }
    }

    override fun getLayoutId() = R.layout.activity_main

    override fun onInitView() {
        super.onInitView()
    }

    override fun onInitData() {
    }

    override fun initDataObserver() {
        mViewModel.stockData.observe(this, Observer {
            it?.let { content ->
                Log.d("donglin", "$content")
            }
        })
    }
}