package com.mb.scrapbook.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mb.scrapbook.app.R
import com.mb.scrapbook.app.ui.fragments.DetailFragment
import com.mb.scrapbook.app.ui.fragments.ListFragment

/**
 * FragmentTransferActivity
 * 实现单Activity多Fragment交互的中转站
 */
class FragmentTransferActivity: AppCompatActivity(), ListFragment.Callback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_fragment)

        // 添加ListFragment实例
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.side_right_in, R.anim.side_left_out)
            .add(R.id.fragmentContainer, ListFragment.newInstance())
            .commit()
    }

    /**
     * 实现ListFragment与Activity交互
     */
    override fun onSelectItem(itemId: String) {
        /**
         * 将数据itemId传递给DetailFragment界面并添加回退栈
         * 回退栈表示：为此次replace()事务添加回滚标记，当点击back键后，表示FragmentManager会优先回滚此次replace()操作
         * 添加回滚标记后，在DetailFragment界面点击back键后，会回退到ListFragment界面
         */
        supportFragmentManager.beginTransaction()
            // 设置A和B两个Fragment进出动画
            .setCustomAnimations(
                // B进入              A退出
                R.anim.side_right_in, R.anim.side_left_out,
                // A进入              B退出
                R.anim.side_left_in, R.anim.side_right_out
            )
            .replace(R.id.fragmentContainer, DetailFragment.newInstance(itemId))
            .addToBackStack(null)
            .commit()
    }
}