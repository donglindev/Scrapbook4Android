package com.mb.scrapbook.lottery.ui.wizard

import android.content.Intent
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.mb.scrapbook.lib.base.mvvm.view.BaseActivity
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.core.data.DrawRepository
import com.mb.scrapbook.lottery.data.AppPrefs
import com.mb.scrapbook.lottery.databinding.ActivityWizardBinding
import com.mb.scrapbook.lottery.ui.LotteryMainActivity
import com.mb.scrapbook.lottery.ui.models.ModelDownloadActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 首启三步向导(D7=A):建赛季 → 模型下载(可跳过)→ 完成进入竞技场。 */
class WizardActivity : BaseActivity() {

    private lateinit var binding: ActivityWizardBinding
    private var defaultStart = "26094"

    override fun getLayoutId(): Int = R.layout.activity_wizard

    override fun onInitView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wizard)
    }

    override fun onInitData() {
        lifecycleScope.launch {
            defaultStart = "%05d".format(DrawRepository().load().last().period.toInt() + 1)
            binding.startPeriod.setText(defaultStart)
        }
        binding.step1Next.setOnClickListener { showStep(2) }
        binding.step2Download.setOnClickListener {
            startActivity(Intent(this, ModelDownloadActivity::class.java))
            showStep(3)
        }
        binding.step2Skip.setOnClickListener { showStep(3) }
        binding.step3Done.setOnClickListener { finishWizard() }
        showStep(1)
    }

    private fun showStep(step: Int) {
        binding.step1.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.step2.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.step3.visibility = if (step == 3) View.VISIBLE else View.GONE
        binding.wizardProgress.text = "$step/3"
        if (step == 3) {
            val start = binding.startPeriod.text.toString().ifBlank { defaultStart }
            binding.step3Summary.text =
                "赛季 S1 已就绪,live 自第 $start 期开始记录。\nJVM 回放判决表不受影响(与桌面同种子同结果)。\n模型稍后可在「设置 → 模型管理」下载。"
        }
    }

    private fun finishWizard() {
        val start = binding.startPeriod.text.toString().trim().ifBlank { defaultStart }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppPrefs.setSeasonStart(applicationContext, start)
                AppPrefs.markFirstRunDone(applicationContext)
            }
            startActivity(
                Intent(this@WizardActivity, LotteryMainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            finish()
        }
    }
}
