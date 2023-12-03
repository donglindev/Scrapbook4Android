package com.mb.scrapbook.app.module.example.view

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.module.example.model.ExampleItemData
import com.mb.scrapbook.app.module.example.repository.ExampleRepository
import com.mb.scrapbook.app.module.example.viewmodel.ExampleViewModel
import com.mb.scrapbook.lib.base.mvvm.view.BaseViewModelActivity
import com.thekhaeng.recyclerviewmargin.LayoutMarginDecoration

class ExampleActivity: BaseViewModelActivity<ExampleViewModel>() {
    /** Current display fragment */
    private var fragmentDisplay: Fragment? = null

    /** RecyclerView adapter */
    private lateinit var adapterExample: ExampleItemAdapter

    /** RV组件 */
    private val rvList by lazy { findViewById<RecyclerView>(R.id.rvList) }

    /** 容器组件 */
    private val layoutContainer by lazy { findViewById<FrameLayout>(R.id.layoutContainer) }

    companion object {
        const val TAG = "ExampleActivity"
    }

    // 设置布局
    override fun getLayoutId(): Int = R.layout.activity_example

    /**
     * 初始化布局
     */
    override fun onInitView() {
        super.onInitView()
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
        mViewModel.mainLiveData.observe(this) {
            /* {
             *    监听首页数据改变
             * }
             */
            onUpdateRecyclerViewData(it)
        }

        mViewModel.exampleLiveData.observe(this) {
            /*
             * {
             *    监听Fragment显示状态
             * }
             */
            val hidden = (ExampleRepository.TYPE_HIDE_FRAGMENT == it.type)
            onUpdateExampleContainer(it, !hidden)
        }
    }


    /**
     * 初始化数据
     */
    override fun onInitData() {
        // 请求首页数据
        mViewModel.loadMainList()
    }


    override fun onBackPressed() {
        /**
         * 根据ViewModel显示栈状态退出界面
         */
        if (mViewModel.shouldBack()) {
            return
        }
        super.onBackPressed()
    }


    /**
     * 更新Fragment显示状态
     */
    private fun onUpdateExampleContainer(data: ExampleItemData, display: Boolean) {
        supportFragmentManager?.let { mgr ->
            val transaction = mgr.beginTransaction()
            // if 'fragmentDisplay' isn't null, then anyway remove current display fragment
            fragmentDisplay?.let { fragment -> transaction.remove(fragment) }

            if (display) {
                // show list and hide fragment
                layoutContainer.visibility = View.VISIBLE
                rvList.visibility = View.GONE
                data.fragment?.let { fragment ->
                    fragmentDisplay = fragment
                    transaction.add(R.id.layoutContainer, fragment)
                }
            } else {
                // show list and hide fragment
                layoutContainer.visibility = View.GONE
                rvList.visibility = View.VISIBLE
                fragmentDisplay = null
            }
            transaction.commit()
        }
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