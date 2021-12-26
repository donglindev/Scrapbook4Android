package com.mb.scrapbook.lib.base.network

import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonParseException
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.common.StateEnum
import retrofit2.HttpException
import java.net.ConnectException
import java.net.UnknownHostException

object NetExceptionHandle {
    fun handleException(throwable: Throwable?, loadState: MutableLiveData<State>) {
        // val ex = Exception()
        throwable?.let {
            when (it) {
                is HttpException -> {
                    loadState.postValue(State(StateEnum.NETWORK_ERROR))
                }
                is ConnectException -> {
                    loadState.postValue(State(StateEnum.NETWORK_ERROR))
                }
                is UnknownHostException -> {
                    loadState.postValue(State(StateEnum.NETWORK_ERROR))
                }
                is JsonParseException -> {
                    loadState.postValue(State(StateEnum.NETWORK_ERROR))
                }
            }
        }
    }
}