package com.mb.scrapbook.app.module.example.lib.recyclerview.repository

import androidx.lifecycle.MutableLiveData
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.ItemSettingsData
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.SettingsValue
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.model.ItemKey
import com.mb.scrapbook.lib.base.mvvm.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JD巡检设置页数据
 *
 * @author donglin6
 * @date 2024/05/31
 */
class JDPatrolSettingsRepository(private val state: MutableLiveData<State>) : BaseRepository() {

    /** 伴生对象 */
    companion object {
        /** 开关：开 */
        const val TOGGLE_ON = "1"
        /** 系统配置 */
        const val TYPE_SYSTEM: Int = 0xA
        /** 页面配置 */
        const val TYPE_SETTINGS: Int = 0xB

        /** 系统版本 */
        const val TYPE_SYSTEM_DEVICE_NAME = 0xA001
        /** 巡检服务 */
        const val TYPE_SYSTEM_PATROL_SERVICE = 0xA002
        /** 权限状态 */
        const val TYPE_SYSTEM_PERMISSION_STATE = 0xA003

        /** 打印全量信息 */
        const val TYPE_SETTINGS_PRINT_ACCESSIBILITY = 0xB001
        /** 打印节点信息 */
        const val TYPE_SETTINGS_PRINT_NODE = 0xB002
        /** 收集器容量 */
        const val TYPE_SETTINGS_COLLECT_CAPACITY = 0xB003
        /** 收集器持续收集时间 */
        const val TYPE_SETTINGS_COLLECT_DURATION = 0xB004
        /** 绘制边界 */
        const val TYPE_SETTINGS_DRAW_RECT = 0xB005
    }
    // 数据
    private var mapData: MutableMap<ItemKey, MutableList<ItemSettingsData>>

    init {
        /** 系统设置数据集合 */
        val listSystem: MutableList<ItemSettingsData> = mutableListOf(
            // 系统版本 -> Android 9
            ItemSettingsData(TYPE_SYSTEM_DEVICE_NAME,
                             groupType = TYPE_SYSTEM,
                             SettingsValue.TextSettings("系统版本")
            ),
            // 巡检服务 -> 正常/重启
            ItemSettingsData(TYPE_SYSTEM_PATROL_SERVICE,
                             groupType = TYPE_SYSTEM,
                             SettingsValue.ButtonSettings("巡检服务", "正常", "重启")
            ),
            // 权限状态 -> 正常/申请
            ItemSettingsData(TYPE_SYSTEM_PERMISSION_STATE,
                             groupType = TYPE_SYSTEM,
                             SettingsValue.ButtonSettings("权限状态", "正常", "申请")
            )
        )

        /** 巡检设置数据集合 */
        val listSettings: MutableList<ItemSettingsData> = mutableListOf(
            // 打印全量信息 -> false
            ItemSettingsData(TYPE_SETTINGS_PRINT_ACCESSIBILITY,
                             groupType = TYPE_SETTINGS,
                             SettingsValue.ToggleSettings("打印全量信息", false)
            ),
            // 打印节点信息 -> false
            ItemSettingsData(TYPE_SETTINGS_PRINT_NODE,
                             groupType = TYPE_SETTINGS,
                             SettingsValue.ToggleSettings("打印节点信息", false)
            ),
            // 收集器容量 -> 3
            ItemSettingsData(TYPE_SETTINGS_COLLECT_CAPACITY,
                             groupType = TYPE_SETTINGS,
                             SettingsValue.PickerSettings("收集器容量", arrayListOf(3, 5, 10), 3)
            ),
            // 收集持续时间 -> 3
            ItemSettingsData(TYPE_SETTINGS_COLLECT_DURATION,
                             groupType = TYPE_SETTINGS,
                             SettingsValue.PickerSettings("收集持续时间", arrayListOf(3, 5, 8), 3)
            ),
            // 绘制边界 -> false
            ItemSettingsData(TYPE_SETTINGS_DRAW_RECT,
                             groupType = TYPE_SETTINGS,
                             SettingsValue.ToggleSettings("绘制边界", false)
            )
        )

        /** 全部数据 */
        mapData = mutableMapOf(
            Pair(ItemKey(TYPE_SYSTEM, "系统信息"), listSystem),
            Pair(ItemKey(TYPE_SETTINGS, "巡检配置"), listSettings)
        )
    }

    /** 请求巡检设置页数据Key集合 */
    suspend fun requestDataKeys() : MutableList<ItemKey> = withContext(Dispatchers.IO) {
        mapData.keys.toMutableList()
    }

    /** 请求系统设置数据源 */
    suspend fun requestSystemDataList() : MutableList<ItemSettingsData> = withContext(Dispatchers.IO) {
        mapData[mapData.keys.filter { it.id == TYPE_SYSTEM }[0]] ?: mutableListOf()
    }

    /** 请求巡检配置数据源 */
    suspend fun requestSettingsDataList() : MutableList<ItemSettingsData> = withContext(Dispatchers.IO) {
        mapData[mapData.keys.filter { it.id == TYPE_SETTINGS }[0]] ?: mutableListOf()
    }

}
