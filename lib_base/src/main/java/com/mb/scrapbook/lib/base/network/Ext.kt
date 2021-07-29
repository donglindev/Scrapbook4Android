package com.mb.scrapbook.lib.base.network

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mb.scrapbook.lib.base.common.State
import com.mb.scrapbook.lib.base.common.StateEnum
import com.mb.scrapbook.lib.base.mvvm.repository.BaseRepository
import com.mb.scrapbook.lib.base.mvvm.viewmodel.BaseViewModel
import com.mb.scrapbook.lib.base.network.response.BaseResponse
import kotlinx.coroutines.launch


fun <T> BaseResponse<T>.dataConvert(
    loadState: MutableLiveData<State>
): T {
    when (code) {
        NetConstant.STATE_SUCCESS -> {
            if (data is List<*>) {
                if ((data as List<*>).isEmpty()) {
                    loadState.postValue(State(StateEnum.EMPTY))
                }
            }
            loadState.postValue(State(StateEnum.SUCCESS))
            return data
        }
        else -> {
            loadState.postValue(State(StateEnum.ERROR, message = message))
            return data
        }
    }
}


fun <T : BaseRepository> BaseViewModel<T>.initiateRequest(
    block: suspend () -> Unit,
    loadState: MutableLiveData<State>
) {
    viewModelScope.launch {
        runCatching {
            block()
        }.onSuccess {
        }.onFailure {
            NetExceptionHandle.handleException(it, loadState)
        }
    }
}
