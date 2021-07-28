package com.mb.scrapbook.lib.base.network

/**
 * Network Constant
 * 网络常量类
 *
 * @author DongLin
 * @date 2021/07/28
 */
object NetConstant {

    const val CONNECT_TIME_OUT  = 30L    // 30/sec
    const val READ_TIME_OUT     = 180L   // 180/sec
    const val WRITE_TIME_OUT    = 30L    // 30/sec

    const val HEAD_CONNECTION = "Connection"
    const val HEAD_VAL_CLOSE = "close"
    const val HEAD_USER_AGENT = "User-Agent"
    const val HEAD_VAL_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36;"

    const val BASE_URL_LOTTERY = "";
}