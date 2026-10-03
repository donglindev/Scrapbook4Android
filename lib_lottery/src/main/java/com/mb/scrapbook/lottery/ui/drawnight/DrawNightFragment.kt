package com.mb.scrapbook.lottery.ui.drawnight

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.mb.scrapbook.lib.base.mvvm.view.BaseFragment
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.core.domain.Verdict
import com.mb.scrapbook.lottery.data.DrawEntryValidator
import com.mb.scrapbook.lottery.databinding.FragmentDrawnightBinding

/**
 * 开奖夜三态(D6=A):出票(聚光灯)→ 锁定(录入 D14)→ 结算(stagger 揭晓 D12)。
 * keep-screen-on 常亮(状态矩阵);「减少动画」开启时 stagger 瞬时呈现(D16)。
 */
class DrawNightFragment : BaseFragment() {

    private lateinit var binding: FragmentDrawnightBinding
    private val vm: DrawNightViewModel by lazy {
        ViewModelProvider(this).get(DrawNightViewModel::class.java)
    }
    private val handler = Handler(Looper.getMainLooper())

    override fun getLayoutId(): Int = R.layout.fragment_drawnight

    override fun onInitView(layout: View) {
        binding = DataBindingUtil.bind(layout) ?: return
        binding.btnStartPicking.setOnClickListener { vm.startPeriod() }
        binding.btnConfirmEntry.setOnClickListener { onConfirmClicked() }
        binding.btnClearEntry.setOnClickListener { clearEntry() }
        entryFields().forEach { it.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refreshEntryState()
        }) }
    }

    override fun onInitData() {
        vm.ui.observe(this) { render(it) }
    }

    // ---- 三态渲染 ----

    private fun render(ui: DrawNightViewModel.Ui) {
        val goneIf = { show: Boolean -> if (show) View.VISIBLE else View.GONE }
        binding.sectionIdle.visibility = goneIf(ui is DrawNightViewModel.Ui.Idle)
        binding.btnStartPicking.visibility = goneIf(ui is DrawNightViewModel.Ui.Idle)
        binding.sectionPicking.visibility = goneIf(ui is DrawNightViewModel.Ui.Picking)
        binding.sectionLocked.visibility = goneIf(ui is DrawNightViewModel.Ui.Locked)
        binding.sectionResult.visibility = goneIf(ui is DrawNightViewModel.Ui.Resulted)
        when (ui) {
            is DrawNightViewModel.Ui.Idle -> {
                binding.drawnightPeriod.text = "已结算 ${ui.settledCount} 期"
                binding.sectionIdle.text = "下一期:第 ${ui.nextPeriod} 期\n${ui.note}"
                binding.btnStartPicking.text = "第 ${ui.nextPeriod} 期 · 出票"
            }
            is DrawNightViewModel.Ui.Picking -> {
                binding.pickingProgress.text = "${ui.index}/${ui.total} 出票中"
                binding.pickingName.text = ui.name
                binding.pickingStatus.text = ui.streamingText // D10 聚光灯流式理由
                renderDoneTickets(ui.doneTickets)
            }
            is DrawNightViewModel.Ui.Locked -> {
                binding.drawnightPeriod.text = "第 ${ui.period} 期 · 全员锁定,等待录入"
                binding.lockedTickets.text = ui.tickets.joinToString("\n") { (name, t) -> "$name:$t" }
                refreshEntryState()
            }
            is DrawNightViewModel.Ui.Resulted -> {
                binding.drawnightPeriod.text = "第 ${ui.period} 期 · 已结算(不可变)"
                renderResultRows(ui)
            }
        }
    }

    private fun renderDoneTickets(done: List<Pair<String, String>>) {
        binding.pickingDone.removeAllViews()
        done.forEach { (name, t) -> binding.pickingDone.addView(ticketLine("$name:$t", isResult = false)) }
    }

    /** D12:按序 stagger 揭晓(间隔 600ms;系统动画关闭时瞬时)。 */
    private fun renderResultRows(ui: DrawNightViewModel.Ui.Resulted) {
        binding.resultRows.removeAllViews()
        handler.removeCallbacksAndMessages(null)
        val scale = Settings.Global.getFloat(requireContext().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val step = if (scale == 0f) 0L else 600L
        ui.rows.forEachIndexed { i, row ->
            handler.postDelayed({ binding.resultRows.addView(resultLine(row)) }, step * i)
        }
    }

    private fun resultLine(row: DrawNightViewModel.ResultRow): View {
        val badge = badgeTextFor(row.verdict)
        return ticketLine(
            "${row.name}  ${row.hitText}  ¥${row.winningsYuan}  [$badge]\n${row.ticketsText}",
            isResult = true,
        )
    }

    private fun badgeTextFor(verdict: Verdict): String = when (verdict) {
        Verdict.NOT_SIGNIFICANT -> "✕ 与随机无差异"
        Verdict.SIGNIFICANTLY_WORSE -> "▼ 显著更差"
        Verdict.SIGNIFICANTLY_BETTER -> "⚠ 数据警报:显著更好"
        Verdict.INSUFFICIENT_SAMPLE -> "… 样本不足"
    }

    private fun ticketLine(text: String, isResult: Boolean): TextView =
        TextView(requireContext()).apply {
            this.text = text
            setPadding(0, if (isResult) 12 else 8, 0, if (isResult) 12 else 8)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isResult) R.color.lottery_text_primary else R.color.lottery_text_secondary,
                )
            )
            textSize = if (isResult) 16f else 13f
        }

    // ---- 录入(D14:即时校验 + 回显确认 + 清空重录) ----

    private fun entryFields(): List<EditText> = with(binding) {
        listOf(red0, red1, red2, red3, red4, red5, blueEntry)
    }

    private fun currentEntry(): Pair<List<Int>, Int>? {
        val fields = entryFields()
        if (fields.any { it.text.isBlank() }) return null
        val nums = fields.map { it.text.toString().toIntOrNull() ?: return null }
        return nums.take(6) to nums[6]
    }

    private fun refreshEntryState() {
        val entry = currentEntry()
        val error = entry?.let { (reds, blue) -> DrawEntryValidator.validate(reds, blue) }
        binding.entryError.text = error ?: ""
        binding.btnConfirmEntry.isEnabled = entry != null && error == null
    }

    private fun onConfirmClicked() {
        val (reds, blue) = currentEntry() ?: return
        val echo = reds.sorted().joinToString(" ") + "  +  蓝 $blue"
        AlertDialog.Builder(requireContext())
            .setTitle("回显确认(D14)")
            .setMessage("红:$echo\n确认后结算,结算后不可变。")
            .setPositiveButton("确认结算") { _, _ ->
                vm.confirmEntry(reds, blue)?.let { binding.entryError.text = it }
            }
            .setNegativeButton("重录", null)
            .show()
    }

    private fun clearEntry() {
        entryFields().forEach { it.setText("") }
        binding.entryError.text = ""
    }

    // ---- keep-screen-on(R11 状态矩阵) ----

    override fun onResume() {
        super.onResume()
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }
}
