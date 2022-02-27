package com.mb.scrapbook.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.ui.fragments.ExampleFragment
import kotlinx.android.synthetic.main.activity_fragment_example.*
import java.util.*

class ExampleFragmentActivity : AppCompatActivity(), ExampleFragment.Callback {

    private var mH: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, ExampleFragment())
                .commit()
            Log.i(TAG, "*** commit fragment in Handler handleMessage method ***")
        }
    }

    companion object {
        const val TAG = "ExampleFragmentActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "*** onCreate ***")
        setContentView(R.layout.activity_fragment_example)
        Log.i(TAG, "*** setContentView Completed ***")
        setSupportActionBar(toolbar)

        /**
         * 首次从FragmentManager队列中获取displayFragment时，fragment实例等于NULL；
         * 当屏幕旋转或配置改变后，再次获取displayFragment时，此时新的FragmentManager会将原始的fragment队列重建，从而恢复原来的状态；
         * 所以，我们仅需判断获取的fragment实例是否为NULL，不为NULL时表示正在使用已恢复的fragment实例；
         */
        var displayFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        Log.i(TAG, "$displayFragment")

        /**
         * 我们使用了Jetpack库版本的Fragment和AppCompatActivity，所以这里需要使用supportFragmentManager管理器；
         * 前缀support表明最初来自v4支持库中，现在已经移动到Jetpack库中并重新打包；
         */
        if (displayFragment == null) {
            Log.i(TAG, "*** create an new ExampleFragment instance ***")
            // 使用newInstance函数创建Fragment实例并设置argument数据
            displayFragment = ExampleFragment.newInstance(UUID.randomUUID().toString())
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, displayFragment)
                .commit()
        }
        Log.i(TAG, "*** commit fragment in onCreate lifecycle ***")
        // mH.sendEmptyMessageDelayed(0, 1000L)
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "*** onStart ***")
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "*** onResume ***")
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "*** onPause ***")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "*** onStop ***")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.i(TAG, "*** onRestoreInstanceState ***")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.i(TAG, "*** onSaveInstanceState ***")
    }

    /**
     * 实现被托管Fragment回调接口函数
     */
    override fun onSendData(data: Any?) {
        Log.i(TAG, "=== onSendData ===")
    }
}