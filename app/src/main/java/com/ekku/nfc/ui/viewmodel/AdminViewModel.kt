package com.ekku.nfc.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.ekku.nfc.model.Container
import com.ekku.nfc.model.Resource
import com.ekku.nfc.network.ApiClient
import com.ekku.nfc.network.ApiService
import com.ekku.nfc.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException

class AdminViewModel(context: Context): ViewModel() {

    private val apiService: ApiService by lazy {
        ApiClient.apiClient().create(ApiService::class.java)
    }

    /**
     * adding container to fleet
     */
    fun postContainersToFleet(containers: List<Container>) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.postContainersFleet(containers)
                )
            )
        } catch (exception: Exception) {
            emit(
                Resource.error(
                    data = null,
                    message = NetworkUtils.getError(exception as HttpException)
                )
            )
        }
    }

    // integrate your apis here.

    
    class AdminViewModelFactory(private val context: Context? = null) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return context?.let { AdminViewModel(it) } as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}