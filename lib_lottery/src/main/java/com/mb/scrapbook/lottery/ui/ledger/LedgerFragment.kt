package com.mb.scrapbook.lottery.ui.ledger

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.mb.scrapbook.lib.base.mvvm.view.BaseFragment
import com.mb.scrapbook.lottery.R
import com.mb.scrapbook.lottery.databinding.FragmentLedgerBinding
import com.mb.scrapbook.lottery.ui.table.TableViewModel

/** 诚实账本(D5=A):徽章行 + 命中分布 + 免责声明常驻底部(R9)。 */
class LedgerFragment : BaseFragment() {

    private lateinit var binding: FragmentLedgerBinding

    override fun getLayoutId(): Int = R.layout.fragment_ledger

    override fun onInitView(layout: View) {
        binding = DataBindingUtil.bind(layout) ?: return
        binding.ledgerList.layoutManager = LinearLayoutManager(context)
        binding.ledgerList.adapter = LedgerRowAdapter()
    }

    override fun onInitData() {
        ViewModelProvider(requireActivity()).get(TableViewModel::class.java).rows.observe(this) { rows ->
            (binding.ledgerList.adapter as? LedgerRowAdapter)?.submit(rows.orEmpty())
        }
    }
}
