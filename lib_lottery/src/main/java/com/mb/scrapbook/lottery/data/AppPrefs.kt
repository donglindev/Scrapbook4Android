package com.mb.scrapbook.lottery.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** 偏好存储(D22:Preferences DataStore)。首启向导标记 / 赛季起始期 / 活动赛季。 */
object AppPrefs {

    private val Context.store by preferencesDataStore("lottery_prefs")
    private val KEY_FIRST_RUN = booleanPreferencesKey("first_run_done")
    private val KEY_SEASON_START = stringPreferencesKey("season_start")
    private val KEY_SEASON = stringPreferencesKey("active_season")
    private val KEY_ASSUME_NOTICE = booleanPreferencesKey("assume_value_notice_shown")

    /** 首次结算后弹一次「一/二等奖按假设值计」说明(D5)。 */
    suspend fun assumeNoticeShown(context: Context): Boolean =
        context.store.data.first()[KEY_ASSUME_NOTICE] ?: false

    suspend fun markAssumeNoticeShown(context: Context) {
        context.store.edit { it[KEY_ASSUME_NOTICE] = true }
    }

    suspend fun firstRunDone(context: Context): Boolean =
        context.store.data.first()[KEY_FIRST_RUN] ?: false

    suspend fun markFirstRunDone(context: Context) {
        context.store.edit { it[KEY_FIRST_RUN] = true }
    }

    suspend fun seasonStart(context: Context): String? =
        context.store.data.first()[KEY_SEASON_START]

    suspend fun setSeasonStart(context: Context, period: String) {
        context.store.edit { it[KEY_SEASON_START] = period }
    }

    /** 重建/归档赛季后清起始期(下赛季向导重新定)。 */
    suspend fun clearSeasonStart(context: Context) {
        context.store.edit { it.remove(KEY_SEASON_START) }
    }
}
