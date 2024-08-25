package com.mb.scrapbook.app.module.example.lib.recyclerview.viewmode

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.DeviceUtils
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.ItemSettingsData
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.JDPatrolSettingsUtils
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.SettingsValue.TextSettings
import com.mb.scrapbook.app.module.example.lib.recyclerview.model.SettingsValue.ButtonSettings
import com.mb.scrapbook.app.module.example.lib.recyclerview.repository.JDPatrolSettingsRepository
import com.mb.scrapbook.lib.base.model.ItemKey
import com.mb.scrapbook.lib.base.mvvm.viewmodel.BaseViewModel
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * JD巡检服务设置ViewModel对象
 *
 * @author donglin6
 * @date 2024/06/03
 */
class JDPatrolSettingsViewModel : BaseViewModel<JDPatrolSettingsRepository>() {

    /** 系统设置数据源 */
    private val mSystemDataList: MutableList<ItemSettingsData> = mutableListOf()
    /** 巡检设置数据源 */
    private val mSettingsDataList: MutableList<ItemSettingsData> = mutableListOf()

    /** 巡检设置Key数据源 */
    private var _keysLiveData: MutableLiveData<MutableList<ItemKey>> = MutableLiveData()
    val keysLiveData = _keysLiveData

    /** 初始化系统信息 */
    private var _initSystemLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val initSystemLiveData = _initSystemLiveData

    /** 初始化巡检配置 */
    private var _initSettingsLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val initSettingsLiveData = _initSettingsLiveData

    /** 更新巡检配置 */
    private var _updatePatrolConfigLiveData: MutableLiveData<ItemSettingsData> = MutableLiveData()
    val updatePatrolConfigLiveData = _updatePatrolConfigLiveData

    /** 根据key查询巡检一级数据 */
    fun searchDataListByKey(key: ItemKey) : MutableList<ItemSettingsData> = when (key.id) {
        JDPatrolSettingsRepository.TYPE_SYSTEM -> mSystemDataList.toMutableList() // copy
        JDPatrolSettingsRepository.TYPE_SETTINGS -> mSettingsDataList.toMutableList() // copy
        else -> mutableListOf()
    }

    /**
     * 初始化巡检页配置数据集
     * 此方法在巡检列表页显示loading时调用
     * {@see #JDPatrolSettingsAdapter.convert()}
     *
     * 说明：在ViewModel中不不不要保存Context对象；
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun initPatrolConfigureList(key: ItemKey, context: Context) {
        when (key.id) {
            // 初始化系统信息数据集
            JDPatrolSettingsRepository.TYPE_SYSTEM -> {
                mSystemDataList.forEach { item ->
                    item.value.apply {
                        when (item.uniqueType) {
                            JDPatrolSettingsRepository.TYPE_SYSTEM_DEVICE_NAME -> { // 系统版本
                                val version = ("Android ${ DeviceUtils.getSDKVersionName() }")
                                JDPatrolSettingsUtils.writeConfigValue(item, version)

                                val settings = (this as TextSettings)
                                settings.updateDisplayText(version)
                            }
                            JDPatrolSettingsRepository.TYPE_SYSTEM_PATROL_SERVICE -> { // 巡检服务
                                JDPatrolSettingsUtils.writeConfigValue(item, ButtonSettings.CLICKABLE.toString())
                                val settings = (this as ButtonSettings)
                                settings.updateButtonStatus(ButtonSettings.CLICKABLE)
                            }
                            JDPatrolSettingsRepository.TYPE_SYSTEM_PERMISSION_STATE -> { // 权限状态
                                JDPatrolSettingsUtils.writeConfigValue(item, ButtonSettings.NORMAL.toString())
                                val settings = (this as ButtonSettings)
                                // 检查应用所需权限
                                (context as? FragmentActivity).let { activity ->
                                    if (Settings.canDrawOverlays(activity)) {
                                        settings.updateButtonStatus(ButtonSettings.NORMAL)
                                        settings.setOnClickListener(null) // 清空点击事件
                                    } else {
                                        settings.updateButtonStatus(ButtonSettings.CLICKABLE)
                                        settings.setOnClickListener {
                                            val requestPermissions = arrayListOf(
                                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                android.Manifest.permission.SYSTEM_ALERT_WINDOW
                                            )
                                            PermissionX.init(activity)
                                                .permissions(requestPermissions)
                                                .onExplainRequestReason { scope, deniedList ->
                                                    val message = "JD巡检服务需要您同意以下权限才能正常使用"
                                                    scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
                                                }
                                                .request { allGranted, grantedList, deniedList ->
                                                    if (allGranted) {
                                                        settings.updateButtonStatus(ButtonSettings.NORMAL)
                                                    } else {
                                                        settings.updateButtonStatus(ButtonSettings.CLICKABLE)
                                                    }
                                                    updatePatrolConfigureData(item) // 通知数据改变
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 系统信息初始化完成
                _initSystemLiveData.value = true
            }
            // 初始化巡检配置数据集
            JDPatrolSettingsRepository.TYPE_SETTINGS -> {
                // 巡检配置初始化完成
                _initSettingsLiveData.value = true
            }
        }
    }

    /** 加载巡检数据Key内容 */
    fun loadPatrolDataKeys() {
        viewModelScope.launch {
            _keysLiveData.value = repository.requestDataKeys()
        }
    }

    /** 根据Key加载数据源 */
    suspend fun loadPatrolDataListByKey(key: ItemKey): MutableList<ItemSettingsData> = viewModelScope.async {
        when (key.id) {
            // 系统设置数据源
            JDPatrolSettingsRepository.TYPE_SYSTEM -> {
                if (mSystemDataList.isEmpty()) {
                    mSystemDataList.addAll(repository.requestSystemDataList())
                }
                mSystemDataList.toMutableList()
            }
            // 巡检设置数据源
            JDPatrolSettingsRepository.TYPE_SETTINGS -> {
                if (mSettingsDataList.isEmpty()) {
                    mSettingsDataList.addAll(repository.requestSettingsDataList())
                }
                mSettingsDataList.toMutableList()
            }
            // 空数据源
            else -> mutableListOf()
        }
    }.await()

    /** 更新巡检配置数据 */
    private fun updatePatrolConfigureData(item: ItemSettingsData) {
        viewModelScope.launch {
            _updatePatrolConfigLiveData.value = item
        }
    }

}