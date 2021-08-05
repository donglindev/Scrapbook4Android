package com.mb.scrapbook.lib.view.common

import com.mb.scrapbook.lib.base.mvvm.repository.BaseRepository

/**
 * View合约数据仓库
 *
 * @author DongLin
 * @date 2021/08/05
 */
class ViewContractRepository private constructor(
            private val viewContractList: MutableList<ViewContract>,
            private val viewLayoutContractList: MutableList<ViewLayoutContract>,
            private val viewDataContractList: MutableList<ViewDataContract>
) : BaseRepository() {

    companion object {
        private val instance: ViewContractRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                ViewContractRepository(
                    mutableListOf(),
                    mutableListOf(),
                    mutableListOf()
                )
        }


        /**
         * 根据uniqueId查找ViewContract
         */
        fun findViewContract(uniqueId: Long) = instance.viewContractList.firstOrNull {
            it.uniqueId == uniqueId
        }


        /**
         * 根据uniqueId查找ViewLayoutContract
         */
        fun findViewLayoutContract(uniqueId: Long) = instance.viewLayoutContractList.firstOrNull {
            it.view.uniqueId == uniqueId
        }


        /**
         * 根据uniqueId查找ViewDataContract
         */
        fun findViewDataContract(uniqueId: Long) = instance.viewDataContractList.firstOrNull {
            it.view.uniqueId == uniqueId
        }


        /**
         * 新增View合约
         */
        fun addViewContract(viewContract: ViewContract): Boolean =
            instance.viewContractList.add(viewContract)


        /**
         * 新增View合约
         */
        fun addViewContract(viewContactArray: List<ViewContract>): Boolean =
            instance.viewContractList.addAll(viewContactArray)


        /**
         * 新增ViewLayout合约
         */
        fun addViewLayoutContract(viewLayoutContract: ViewLayoutContract): Boolean =
            instance.viewLayoutContractList.add(viewLayoutContract)


        /**
         * 新增ViewLayout合约
         */
        fun addViewLayoutContract(viewLayoutContractArray: List<ViewLayoutContract>): Boolean =
            instance.viewLayoutContractList.addAll(viewLayoutContractArray)


        /**
         * 新增ViewData合约
         */
        fun addViewDataContract(viewDataContract: ViewDataContract): Boolean =
            instance.viewDataContractList.add(viewDataContract)


        /**
         * 新增ViewData合约
         */
        fun addViewDataContract(viewDataContractArray: List<ViewDataContract>): Boolean =
            instance.viewDataContractList.addAll(viewDataContractArray)
    }
}