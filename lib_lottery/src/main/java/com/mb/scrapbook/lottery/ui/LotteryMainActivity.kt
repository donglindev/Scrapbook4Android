package com.mb.scrapbook.lottery.ui

import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.BarUtils
import com.mb.scrapbook.lib.base.mvvm.view.BaseActivity
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.databinding.ActivityLotteryBinding
import com.mb.scrapbook.lottery.ui.arena.ArenaFragment
import com.mb.scrapbook.lottery.ui.drawnight.DrawNightFragment
import com.mb.scrapbook.lottery.ui.ledger.LedgerFragment

/**
 * 竞技场主 Activity(D21=A:放 lib_lottery,app manifest launcher 指向此处)。
 * BottomNav 三 tab(D3=A):竞技场(HOME)/ 诚实账本 / 开奖夜。
 */
class LotteryMainActivity : BaseActivity() {

    private lateinit var binding: ActivityLotteryBinding

    private val fragments by lazy {
        mapOf(
            R.id.nav_arena to ArenaFragment(),
            R.id.nav_ledger to LedgerFragment(),
            R.id.nav_drawnight to DrawNightFragment(),
        )
    }
    private var current: Fragment = ArenaFragment()

    override fun getLayoutId(): Int = R.layout.activity_lottery

    override fun onInitView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_lottery)
    }

    override fun onInitData() {
        switchTo(R.id.nav_arena)
        binding.lotteryNav.setOnNavigationItemSelectedListener { item ->
            switchTo(item.itemId)
            true
        }
    }

    private fun switchTo(id: Int) {
        val target = fragments.getValue(id)
        supportFragmentManager.beginTransaction()
            .apply {
                fragments.values.forEach {
                    if (it.isAdded && it != target) hide(it)
                }
                if (target.isAdded) show(target) else add(R.id.lottery_container, target)
            }
            .commit()
        current = target
    }

    /** R11 D14=A:暗色状态栏(宿主默认白底亮色模式,不适用)。 */
    override fun setupStatusBarStyle() {
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.lottery_bg))
        BarUtils.setStatusBarLightMode(this, false)
    }
}
