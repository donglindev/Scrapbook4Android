package com.mb.scrapbook.app.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mb.scrapbook.app.R

/**
 * DetailFragment
 * 显示ListFragment传入的条目ID；
 * 由于DetailFragment只是用于显示内容，所以仅需要重写绑定布局所需要生命周期回调函数；
 */
class DetailFragment: Fragment() {

    private lateinit var mTextDisplay: TextView

    /**
     * 伴生对象创建DetailFragment实例
     */
    companion object {
        private const val TAG = "DetailFragment"

        private const val VAL_EMPTY = ""
        private const val KEY_ITEM_ID = "itemId";

        /**
         * 静态工厂方法创建DetailFragment实例和设置数据
         */
        fun newInstance(itemId: String): Fragment = DetailFragment().apply {
            arguments = Bundle().apply { putString(KEY_ITEM_ID, itemId) }
        }
    }

    /**
     * onCreateView中绑定布局
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "*** onCreateView ***")
        val view = inflater.inflate(R.layout.fragment_detail, container, false)
        mTextDisplay = view.findViewById(R.id.textDisplay)
        return view
    }

    /**
     * onViewCreated中绑定数据
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "*** onViewCreated ***")
        super.onViewCreated(view, savedInstanceState)
        mTextDisplay.text = arguments?.getString(KEY_ITEM_ID) ?: VAL_EMPTY
    }
}