package com.mb.scrapbook.lottery.ui.models

import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.mb.scrapbook.lib.base.mvvm.view.BaseActivity
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.databinding.ActivityModelDownloadBinding

/** 模型下载管理页(D8=A):两槽位卡,状态机见 VM;非 arm64 显示「设备不支持」(D16)。 */
class ModelDownloadActivity : BaseActivity() {

    private lateinit var binding: ActivityModelDownloadBinding
    private val vm: ModelDownloadViewModel by lazy {
        ViewModelProvider(this).get(ModelDownloadViewModel::class.java)
    }

    override fun getLayoutId(): Int = R.layout.activity_model_download

    override fun onInitView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_model_download)
    }

    override fun onInitData() {
        binding.slotAction1.setOnClickListener { vm.download(vm.slots[0].id) }
        binding.slotAction2.setOnClickListener { vm.download(vm.slots[1].id) }
        if (!vm.deviceSupported) {
            binding.slotStatus1.text = "设备不支持(需 arm64-v8a,D16)"
            binding.slotStatus2.text = "设备不支持(需 arm64-v8a,D16)"
            binding.slotAction1.isEnabled = false
            binding.slotAction2.isEnabled = false
        }
        vm.slotUi.observe(this) { ui ->
            vm.slots.getOrNull(0)?.let { ui[it.id]?.let { s -> render(0, it.displayName, s) } }
            vm.slots.getOrNull(1)?.let { ui[it.id]?.let { s -> render(1, it.displayName, s) } }
        }
        vm.refresh()
    }

    private fun render(index: Int, name: String, state: ModelDownloadViewModel.SlotUi) {
        val status: TextView = if (index == 0) binding.slotStatus1 else binding.slotStatus2
        val error: TextView = if (index == 0) binding.slotError1 else binding.slotError2
        val progress: ProgressBar = if (index == 0) binding.slotProgress1 else binding.slotProgress2
        val action: Button = if (index == 0) binding.slotAction1 else binding.slotAction2

        status.text = buildString {
            append(state.stateText)
            if (state.bytesText.isNotEmpty()) append(" · ${state.bytesText}")
        }
        error.text = state.error ?: "" // D8 红条
        error.visibility = if (state.error == null) View.GONE else View.VISIBLE
        progress.visibility = if (state.downloading) View.VISIBLE else View.GONE
        progress.isIndeterminate = state.progressPercent < 0
        if (state.progressPercent >= 0) progress.progress = state.progressPercent
        action.isEnabled = !state.downloading && !state.ready && vm.deviceSupported
        action.text = when {
            state.ready -> "已就绪"
            state.error != null -> "重试"
            else -> "下载"
        }
        (if (index == 0) binding.slotCard1 else binding.slotCard2).contentDescription =
            "$name,${state.stateText}${state.error?.let { ",$it" } ?: ""}" // D16
    }
}
