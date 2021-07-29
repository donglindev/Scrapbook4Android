package com.mb.scrapbook.lib.base.network.response

/**
 * Base Response
 *
 * @author DongLin
 * @date 2021/07/28
 */
class BaseResponse<T>(val data: T,
                      val code: Int = 0,
                      val message: String = "")