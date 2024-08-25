package com.mb.scrapbook.app.module.example.lib.recyclerview.view

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SizeUtils
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.module.example.lib.recyclerview.viewmode.JDPatrolSettingsViewModel
import com.mb.scrapbook.lib.base.mvvm.view.BaseViewModelFragment
import com.thekhaeng.recyclerviewmargin.LayoutMarginDecoration

/**
 * JD巡检类
 *
 * @author donglin6
 * @date 2024/05/23
 */
@Suppress("INVISIBLE_SETTER_FROM_DERIVED")
class JDAccessibilityFragment : BaseViewModelFragment<JDPatrolSettingsViewModel>() {

    /** 容器组件 */
    private lateinit var mRvContainer: RecyclerView
    /** 容器数据源 */
    private lateinit var mRvAdapter: JDPatrolSettingsAdapter

    /** 设置布局文件 */
    override fun getLayoutId(): Int = R.layout.fragment_jd_accessibility

    /** 初始化组件 */
    override fun onInitView(parent: View) {
        super.onInitView(parent)
        // 设置RecyclerView容器
        mRvContainer = parent.findViewById(R.id.rvContainer)
        mRvContainer.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        mRvAdapter = JDPatrolSettingsAdapter(mutableListOf(), mViewModel)
        mRvContainer.adapter = mRvAdapter
        mRvContainer.addItemDecoration(LayoutMarginDecoration(1, SizeUtils.dp2px(20f)))
    }

    /** 监听LiveData数据变化 */
    @SuppressLint("NotifyDataSetChanged")
    override fun initDataObserver() {
        // 监听巡检页Key值集合
        mViewModel.keysLiveData.observe(this) {
            // 更新容器数据源
            mRvAdapter.apply {
                data = it
                notifyDataSetChanged()
            }
        }
        // 监听系统信息初始化完成
        mViewModel.initSystemLiveData.observe(this) {
            // 更新容器数据源
            mRvAdapter.apply {
                notifyDataSetChanged()
            }
        }
        // 监听巡检配置初始化完成
        mViewModel.initSettingsLiveData.observe(this) {
            // 更新容器数据源
            mRvAdapter.apply {
                notifyDataSetChanged()
            }
        }
        // 监听巡检配置更新
        mViewModel.updatePatrolConfigLiveData.observe(this) {
            // 更新容器数据源
            mRvAdapter.apply {
                notifyDataSetChanged()
            }
        }
    }

    /** 初始化数据 */
    override fun onInitData() {
        // 1）请求巡检设置页数据
        mViewModel.loadPatrolDataKeys()
    }
}