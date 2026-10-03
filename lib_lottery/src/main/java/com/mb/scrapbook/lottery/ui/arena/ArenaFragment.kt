package com.mb.scrapbook.lottery.ui.arena

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mb.scrapbook.lib.base.mvvm.view.BaseFragment
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.databinding.FragmentArenaBinding
import com.mb.scrapbook.lottery.ui.table.TableViewModel
import kotlinx.coroutines.launch

/** 竞技场(D4=A):卡片网格,ROI 降序,对照组固定末位。 */
class ArenaFragment : BaseFragment() {

    private lateinit var binding: FragmentArenaBinding
    private val tableViewModel: TableViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TableViewModel::class.java)
    }

    override fun getLayoutId(): Int = R.layout.fragment_arena

    override fun onInitView(layout: View) {
        binding = DataBindingUtil.bind(layout) ?: return
        binding.arenaList.layoutManager = LinearLayoutManager(context)
        binding.arenaList.adapter = StrategyCardAdapter()
        binding.arenaSettings.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), com.mb.scrapbook.lottery.ui.settings.SettingsActivity::class.java))
        }
    }

    override fun onInitData() {
        tableViewModel.rows.observe(this) { rows ->
            binding.arenaSkeleton.visibility = if (rows == null) View.VISIBLE else View.GONE
            binding.arenaList.visibility = if (rows == null) View.GONE else View.VISIBLE
            (binding.arenaList.adapter as? StrategyCardAdapter)?.submit(rows.orEmpty())
        }
    }

    override fun onResume() {
        super.onResume()
        // D7=A 横幅:赛季 S1 就绪,live 自第 N 期
        lifecycleScope.launch {
            val start = com.mb.scrapbook.lottery.data.AppPrefs.seasonStart(requireContext())
            binding.arenaSubtitle.text =
                if (start != null) "赛季 S1 · live 自第 $start 期 · JVM 判决表 K=6 阈值 α/6≈0.0083"
                else "JVM 走前回放判决表 · K=6 · 阈值 α/6≈0.0083"
        }
    }
}
