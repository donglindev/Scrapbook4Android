/**
 * JD Patrol Settings Data
 * 京东巡检设置数据文件
 *
 * @author donglin6
 * @date 2024/06/03
 */
package com.mb.scrapbook.app.module.example.lib.recyclerview.model

import android.view.View.OnClickListener
import com.blankj.utilcode.util.SPUtils
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository

/** JD巡检服务页工具类 */
object JDPatrolSettingsUtils {

    /** 系统信息的SP配置文件名称 */
    private const val TYPE_SYSTEM_SP_CONFIG_NAME = "jd_patrol_system"
    /** 巡检配置的SP配置文件名称 */
    private const val TYPE_SETTINGS_SP_CONFIG_NAME = "jd_patrol_settings"

    /** 通过key写入SP配置值 */
    fun writeConfigValue(key: ItemSettingsData, value: String?) = findSPKey(key.uniqueType)?.let { spKey ->
        value?.apply { writeSPValue(key.groupType, spKey, this) }
    }

    /** 通过uniqueType和groupType查找SP配置的值 */
    fun searchConfigValue(uniqueType: Int, groupType: Int): String? = findSPKey(uniqueType)?.let { spKey ->
        findSPValue(spKey, groupType)
    }

    /** 通过key查找SP配置的值 */
    fun searchConfigValue(key: ItemSettingsData) : String? = findSPKey(key.uniqueType)?.let { spKey ->
        findSPValue(spKey, key.groupType)
    }

    /** 查询SP的key值 */
    private fun findSPKey(uniqueType: Int): String? = when (uniqueType) {
        // 系统版本
        JDPatrolSettingsRepository.TYPE_SYSTEM_DEVICE_NAME -> "deviceVersion"
        // 巡检服务
        JDPatrolSettingsRepository.TYPE_SYSTEM_PATROL_SERVICE -> "patrolState"
        // 权限状态
        JDPatrolSettingsRepository.TYPE_SYSTEM_PERMISSION_STATE -> "permissionState"
        // 未查询到
        else -> null
    }

    /** 根据key和group查找SP配置的值 */
    private fun findSPValue(key: String, group: Int): String? = when (group) {
        // 系统配置
        JDPatrolSettingsRepository.TYPE_SYSTEM -> SPUtils.getInstance(TYPE_SYSTEM_SP_CONFIG_NAME).getString(key)
        // 巡检配置
        JDPatrolSettingsRepository.TYPE_SETTINGS -> SPUtils.getInstance(TYPE_SETTINGS_SP_CONFIG_NAME).getString(key)
        // 其他配置
        else -> SPUtils.getInstance().getString(key)
    }

    /** 根据key、group将value写入到SP配置 */
    private fun writeSPValue(group: Int, key: String, value: String) {
        when (group) {
            // 系统配置
            JDPatrolSettingsRepository.TYPE_SYSTEM -> {
                SPUtils.getInstance(TYPE_SYSTEM_SP_CONFIG_NAME).put(key, value)
            }
            // 巡检配置
            JDPatrolSettingsRepository.TYPE_SETTINGS -> {
                SPUtils.getInstance(TYPE_SETTINGS_SP_CONFIG_NAME).put(key, value)
            }
            // 其他配置
            else -> SPUtils.getInstance().put(key, value)
        }
    }
}

/**
 * 设置项数据
 *
 * @author donglin6
 * @date 2024/06/03
 */
data class ItemSettingsData(val uniqueType: Int,
                            var groupType: Int,
                            var value: SettingsValue
)

/**  配置/设置的值 */
sealed class SettingsValue(val type: Int, val title: String) {

    /** 伴生对象 */
    companion object {
        const val TYPE_TEXT = 0xA // 文本类型
        const val TYPE_BUTTON = 0xB // 按钮类型
        const val TYPE_TOGGLE = 0xC // 开关类型
        const val TYPE_PICKER = 0xD // 选择类型
    }

    /** 文本设置 */
    class TextSettings(title: String,
                       private var displayText: String? = null
    ) : SettingsValue(TYPE_TEXT, title) {

        /** 获得文本内容 */
        fun displayText() = displayText

        /** 设置文本内容 */
        fun updateDisplayText(value: String) {
            if (displayText != value) {
                displayText = value
            }
        }
    }

    /** 按钮设置 */
    class ButtonSettings(title: String,
                         private val normalText: String,
                         private val clickableText: String,
                         private var status: Int = 0,
                         private var clickImpl: OnClickListener? = null
    ) : SettingsValue(TYPE_BUTTON, title) {

        companion object {
            // 按钮不可点击状态
            const val NORMAL = 0
            // 按钮可点击状态
            const val CLICKABLE = 1
        }

        /** 获得按钮显示文本 */
        fun displayText() = if (clickable()) clickableText else normalText

        /** 按钮是否可点击状态 */
        fun clickable() = (CLICKABLE == status)

        /** 更新按钮点击状态 */
        fun updateButtonStatus(state: Int) {
            if (status != state) {
                status = state
            }
        }

        /** 获得点击事件实现类 */
        fun getClickListenerImpl() = clickImpl

        /** 设置按钮点击事件 */
        fun setOnClickListener(impl: OnClickListener?) {
            if (impl != clickImpl) {
                clickImpl = impl
            }
        }
    }

    /** 开关设置 */
    class ToggleSettings(title: String,
                         private var switchOn: Boolean
    ) : SettingsValue(TYPE_TOGGLE, title) {

        /** 获得开关值 */
        fun isToggleOn() = switchOn

        /** 设置开关值 */
        fun toggle(value: Boolean) {
            if (switchOn != value) {
                switchOn = value
            }
        }
    }

    /** 选择类型 */
    class PickerSettings<T>(title: String,
                            val datList: ArrayList<T> = arrayListOf(),
                            private var selectValue: T
    ) : SettingsValue(TYPE_PICKER, title) {

        /** 获得选择值 */
        fun getPickValue() = selectValue

        /** 设置选择值 */
        fun pickValue(value: T) {
            if (selectValue !== value) {
                selectValue = value
            }
        }

    }

}
