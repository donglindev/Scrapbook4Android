package com.mb.scrapbook.lottery.ui.arena

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.mb.scrapbook.lib.base.mvvm.view.BaseFragment
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.databinding.FragmentArenaBinding
import com.mb.scrapbook.lottery.ui.table.TableViewModel

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
    }

    override fun onInitData() {
        tableViewModel.rows.observe(this) { rows ->
            binding.arenaSkeleton.visibility = if (rows == null) View.VISIBLE else View.GONE
            binding.arenaList.visibility = if (rows == null) View.GONE else View.VISIBLE
            (binding.arenaList.adapter as? StrategyCardAdapter)?.submit(rows.orEmpty())
        }
    }
}
