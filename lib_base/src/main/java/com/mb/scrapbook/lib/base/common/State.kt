package com.mb.scrapbook.lib.base.common

import androidx.annotation.StringRes

/**
 * 全局状态
 *
 * @author DongLin
 * @date 2021/07/28
 */
data class State(val state: StateEnum,
                 var message: String = "",
                 @StringRes var tip: Int = 0)