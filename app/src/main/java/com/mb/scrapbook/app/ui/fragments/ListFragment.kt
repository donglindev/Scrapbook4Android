package com.mb.scrapbook.app.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.mb.scrapbook.app.R
import java.util.*

/**
 * ListFragment
 * 显示条目数据以及通过点击条目打开DetailFragment界面；
 * ListFragment处理需要处理较多逻辑：
 * 1）绑定布局和添加事件；
 * 2）通过回调接口实现与Activity交互；
 */
class ListFragment: Fragment() {

    /**
     * 与Activity交互使用的接口
     */
    interface Callback {
        /**
         * 委托Activity打开DetailFragment入口函数
         */
        fun onSelectItem(itemId: String)
    }

    /**
     * 伴生对象
     */
    companion object {
        private const val TAG = "ListFragment";

        /**
         * 使用静态工厂创建ListFragment实例
         */
        fun newInstance(): Fragment = ListFragment().apply {
            arguments = Bundle().apply { }
        }
    }

    /** 与Activity交互的对象 */
    private var mCallback: Callback? = null

    /** ListFragment中按钮对象 */
    private lateinit var mBtnOpenDetailFragment: Button

    /**
     * 在onAttach(Context)函数中绑定交互对象
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallback = context as? Callback
    }

    /**
     * 在onDetach函数中取消绑定交互对象
     */
    override fun onDetach() {
        super.onDetach()
        mCallback = null
    }

    /**
     * onCreateView中绑定布局
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        mBtnOpenDetailFragment = view.findViewById(R.id.btnOpenDetailFragment)
        return view
    }

    /**
     * onViewCreated中绑定数据
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 绑定点击事件
        mBtnOpenDetailFragment.setOnClickListener {
            mCallback?.onSelectItem(UUID.randomUUID().toString())
        }
    }
}