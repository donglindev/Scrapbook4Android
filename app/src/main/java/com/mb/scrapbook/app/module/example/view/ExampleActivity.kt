package com.mb.scrapbook.app.module.example.view

import android.util.Log
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.SizeUtils
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.module.example.model.ExampleItemData
import com.mb.scrapbook.app.module.example.viewmodel.ExampleViewModel
import com.mb.scrapbook.lib.base.mvvm.view.BaseViewModelActivity
import com.thekhaeng.recyclerviewmargin.LayoutMarginDecoration
import kotlinx.android.synthetic.main.activity_example.*

class ExampleActivity: BaseViewModelActivity<ExampleViewModel>() {

    // RecyclerView adapter
    private lateinit var adapterExample: ExampleItemAdapter

    companion object {
        const val TAG = "ExampleActivity"
    }


    // 设置布局
    override fun getLayoutId(): Int = R.layout.activity_example


    /**
     * 初始化布局
     */
    override fun initView() {
        super.initView()
        // 设置RecyclerView相关属性
        val mgrLinear = LinearLayoutManager(this)
        mgrLinear.orientation = LinearLayoutManager.VERTICAL
        adapterExample = ExampleItemAdapter(mutableListOf(), mViewModel)

        val spaceCount = 1
        val spaceSize: Int = SizeUtils.dp2px(2.5f)
        val space = LayoutMarginDecoration(spaceCount, spaceSize)

        rvList.adapter = adapterExample
        rvList.layoutManager = mgrLinear
        rvList.addItemDecoration(space)
    }


    // 监听LiveData数据
    override fun initDataObserver() {
        mViewModel.mainLiveData.observe(this, Observer {
            /* {
             *    监听首页数据改变
             * }
             */
            onUpdateRecyclerViewData(it)
        })
    }


    /**
     * 初始化数据
     */
    override fun initData() {
        // 请求首页数据
        mViewModel.loadMainList()
    }


    /**
     * 更新RecyclerView数据
     */
    private fun onUpdateRecyclerViewData(data: MutableList<ExampleItemData>) {
        Log.d(TAG, "on update recycler view data. ${data.size}")
        adapterExample?.let {
            it.data = data
            it.notifyDataSetChanged()
        }
    }
}