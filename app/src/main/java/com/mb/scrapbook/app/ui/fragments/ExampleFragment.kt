package com.mb.scrapbook.app.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mb.scrapbook.app.R
import java.util.concurrent.atomic.AtomicInteger

class ExampleFragment: Fragment() {

    /**
     * 使用伴生对象创建Fragment实例
     */
    companion object {
        const val TAG = "ExampleFragment"

        private val COUNTER = AtomicInteger(1);
        val ID = COUNTER.getAndIncrement()

        /**
         * 静态工厂方法
         * 创建Fragment实例并设置argument数据
         */
        fun newInstance(uuid: String): ExampleFragment = ExampleFragment().apply {
            val args = Bundle().apply {
                putString("uuid", uuid)
            }
            // 存储数据
            arguments = args
        }
    }

    /**
     * 第一类交互：Fragment与Activity交互；
     * 定义接口实现回调对象
     */
    private var callback: Callback? = null;

    /**
     * 第一类交互：Fragment与Activity交互；
     * Fragment中定义Callback接口；
     */
    interface Callback {
        /**
         * 托管Activity需要实现的接口函数
         */
        fun onSendData(data: Any?);
    }

    /**
     * 第一类交互：Fragment与Activity交互；
     * 在onAttach生命周期回调函数中实现Callback接口的绑定；
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.i(TAG, "=== onAttach ===")
        /*
         * 当托管Activity未实现被托管Fragment回调接口时，此时callback对象等于null；
         */
        callback = context as? Callback
        Log.i(TAG, "=== onAttach === callback? $callback")
    }

    /**
     * 第一类交互：Fragment与Activity交互；
     * 在onDetach生命周期回调函数中实现Callback接口取消绑定；
     */
    override fun onDetach() {
        super.onDetach()
        Log.i(TAG, "=== onDetach ===")
        callback = null
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "=== onResume ===")
        // 任何地方通过callback实例通知托管Activity处理相关操作；
        callback?.onSendData(null)
    }

    override fun toString(): String {
        return "ExampleFragment.id=$ID"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.i(TAG, "=== onSaveInstanceState ===")
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        Log.i(TAG, "=== onViewStateRestored ===")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "=== onCreate ===")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "=== onCreateView ===")
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "=== onViewCreated ===")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.i(TAG, "=== onActivityCreated ===")
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "=== onStart ===")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "=== onStop ===")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "=== onDestroyView ===")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "=== onDestroy ===")
    }
}