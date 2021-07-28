package com.mb.scrapbook.lib.base.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit Factory
 * 初始化OkHttpClient和Retrofit对象
 *
 * @author DongLin
 * @date 2021/07/28
 */
class RetrofitFactory private constructor() {

    companion object {
        val instance by lazy { RetrofitFactory() }
    }

    private lateinit var retrofit : Retrofit

    fun <T> create(baseUrl: String, clazz: Class<T>) : T {
        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(initOkHttpClient())
            .build();
        return retrofit.create(clazz)
    }


    /**
     * 创建OkHttpClient对象
     */
    private fun initOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(NetConstant.CONNECT_TIME_OUT, TimeUnit.SECONDS)
            .readTimeout(NetConstant.READ_TIME_OUT, TimeUnit.SECONDS)
            .writeTimeout(NetConstant.WRITE_TIME_OUT, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
            .addInterceptor(initHeadInterceptor())
            .build()
    }


    /**
     * 创建请求头拦截器，添加User-Agent和Connection头信息
     */
    private fun initHeadInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader(NetConstant.HEAD_CONNECTION, NetConstant.HEAD_VAL_CLOSE)
                .addHeader(NetConstant.HEAD_USER_AGENT, NetConstant.HEAD_VAL_USER_AGENT)
                .build()
            val response = chain.proceed(request)
            response
        }
    }
}