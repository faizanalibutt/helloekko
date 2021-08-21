package com.ekku.nfc.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.ekku.nfc.model.AssignPartnerPair
import com.ekku.nfc.model.ContainerPair
import com.ekku.nfc.model.Containers
import com.ekku.nfc.model.Resource
import com.ekku.nfc.network.ApiClient
import com.ekku.nfc.network.ApiService
import com.ekku.nfc.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException


class AdminViewModel(context: Context) : ViewModel() {

    // integrate your admin apis here for FLEET, ASSIGN, EMPTY, CHECK_IN, RETIRED of containers

    private val apiService: ApiService by lazy {
        ApiClient.apiClient(context).create(ApiService::class.java)
    }

    /**
     * adding containers to fleet
     */
    fun postContainersToFleet(containers: Containers) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.postContainersFleet(ApiClient.gson.toJson(containers))
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

    /**
     * assigning containers to partner from fleet
     */
    fun postAssignedContainers(
        partnerId: String,
        containers: List<String>
    ) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.postContainersAssign(
                        ApiClient.gson.toJson(
                            AssignPartnerPair(
                                partnerId,
                                containers
                            )
                        )
                    )
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

    /**
     * fetch partners from fire store we don't own it.
     */
    fun fetchPartners() = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.fetchPartners()
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

    /**
     * empty drop box we have used bunch of containers for the greater.
     */
    fun emptyDropBox(
        dropBoxId: String,
        latitude: Float,
        longitude: Float
    ) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.postContainersEmpty(
                        dropBoxId,
                        latitude,
                        longitude
                    )
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

    /**
     * collect around all drop boxes from admin region
     */
    fun collectDropBoxes() = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.gatherDropBoxes()
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

    /**
     * containers are going to check in by admin
     */
    fun checkInContainers(containers: List<String>) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.postContainersCheckIN(
                        ApiClient.gson.toJson(
                            ContainerPair(
                                containers
                            )
                        )
                    )
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

    /**
     * containers are going to retired from fleet
     */
    fun retiredContainers(containers: List<String>) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.postContainersRetired(
                        ApiClient.gson.toJson(
                            ContainerPair(
                                containers
                            )
                        )
                    )
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