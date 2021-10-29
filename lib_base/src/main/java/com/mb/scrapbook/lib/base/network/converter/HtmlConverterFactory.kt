package com.mb.scrapbook.lib.base.network.converter

import com.mb.scrapbook.lib.base.network.NetConstant
import com.mb.scrapbook.lib.base.network.response.BaseResponse
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type


/**
 * Retrofit中数据响应转换
 * 将okhttp3.Response转换成BaseResponse<data, code, message>对象
 */
class HtmlConverterFactory private constructor(): Converter.Factory() {

    // Retrofit HTML 请求转换器
    private val requestConverter by lazy { HtmlRequestBodyConverter.create() }

    // Retrofit HTML 响应转换器
    private val responseConvert by lazy { HtmlResponseBodyConverter.create() }

    companion object {
        fun create(): HtmlConverterFactory {
            return HtmlConverterFactory();
        }
    }


    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        // 创建请求HTML时的转换器
        return requestConverter
    }


    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        // 创建响应HTML时的转换器
        return responseConvert
    }


    /**
     * HTML retrofit 响应转换器
     */
    class HtmlResponseBodyConverter private constructor(): Converter<ResponseBody, BaseResponse<String>> {
        companion object {
            fun create() : HtmlResponseBodyConverter {
                return HtmlResponseBodyConverter()
            }
        }

        override fun convert(value: ResponseBody): BaseResponse<String>? {
            val body: String = value.string() ?: ""
            return BaseResponse(body, NetConstant.STATE_SUCCESS, NetConstant.MESSAGE_SUCCESS)
        }
    }


    /**
     * HTML retrofit 请求转换器
     */
    class HtmlRequestBodyConverter<T> private constructor(): Converter<T, RequestBody> {
        companion object {
            val MEDIA_HTML: MediaType = MediaType.get("text/html;charset=UTF-8")

            fun create(): HtmlRequestBodyConverter<Any> {
                return HtmlRequestBodyConverter()
            }
        }

        override fun convert(value: T): RequestBody? {
            return RequestBody.create(MEDIA_HTML, value.toString())
        }
    }
}