package com.mb.scrapbook.app.module.example.view

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.SizeUtils
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.module.example.model.ExampleItemData
import com.mb.scrapbook.app.module.example.repository.ExampleRepository
import com.mb.scrapbook.app.module.example.viewmodel.ExampleViewModel
import com.mb.scrapbook.lib.base.mvvm.view.BaseViewModelActivity
import com.mb.scrapbook.lib.view.avt.engine.DolphinVTEngine
import com.thekhaeng.recyclerviewmargin.LayoutMarginDecoration
import kotlinx.android.synthetic.main.activity_example.*

class ExampleActivity: BaseViewModelActivity<ExampleViewModel>() {

    /**
     * Example Container display status
     */
    private var isDisplayExampleContainer: Boolean = false

    /**
     * Current display fragment
     */
    private var fragmentDisplay: Fragment? = null

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
    override fun onInitView() {
        super.onInitView()
        btnClick.setOnClickListener {
            val jsonContent = "{\"abTestDefault\":false,\"abTestFloor\":false,\"backgroundCorner\":0,\"expVarMap\":{},\"flexWidget\":{\"gravity\":\"\",\"layoutType\":\"vertical\",\"viewGroups\":[{\"backgroundColor\":\"#ff0000\",\"backgroundImage\":\"\",\"gravity\":\"\",\"height\":100,\"id\":\"1642056236058\",\"index\":0,\"layoutType\":\"\",\"reuse\":0,\"width\":375},{\"backgroundColor\":\"#00ff00\",\"backgroundImage\":\"\",\"gravity\":\"\",\"height\":150,\"id\":\"1642056255999\",\"index\":1,\"layoutType\":\"\",\"reuse\":0,\"width\":375},{\"backgroundColor\":\"#0000ff\",\"backgroundImage\":\"\",\"gravity\":\"\",\"height\":250,\"id\":\"1642056277287\",\"index\":2,\"layoutType\":\"\",\"reuse\":1,\"width\":375}]},\"floorId\":\"230235\",\"floorType\":\"FLEX_WIDGET\",\"height\":500,\"index\":6,\"marginBottom\":0,\"marginLeft\":0,\"marginRight\":0,\"marginTop\":0,\"paddingBottom\":0,\"paddingLeft\":0,\"paddingRight\":0,\"paddingTop\":0}"
            val engine = DolphinVTEngine()
            engine.makeTree(jsonContent)
        }
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
        if (isDisplayExampleContainer) {
            mViewModel.displayListContainer()
            return; // ignore
        }
        super.onBackPressed()
    }


    /**
     * 更新Fragment显示状态
     */
    private fun onUpdateExampleContainer(data: ExampleItemData, display: Boolean) {
        if (isDisplayExampleContainer != display) {
            isDisplayExampleContainer = display
        }

        supportFragmentManager?.let {
            val transaction = it.beginTransaction()
            // if 'fragmentDisplay' isn't null, then anyway remove current display fragment
            fragmentDisplay?.let {
                transaction.remove(fragmentDisplay!!)
            }

            if (isDisplayExampleContainer) {
                // show list and hide fragment
                layoutContainer.visibility = View.VISIBLE
                rvList.visibility = View.GONE
                data.fragment?.let {
                    transaction.add(R.id.layoutContainer, data.fragment!!)
                }
            } else {
                // show list and hide fragment
                layoutContainer.visibility = View.GONE
                rvList.visibility = View.VISIBLE
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