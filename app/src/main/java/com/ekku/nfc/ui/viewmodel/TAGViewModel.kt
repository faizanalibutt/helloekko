package com.ekku.nfc.ui.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.ekku.nfc.model.*
import com.ekku.nfc.network.ApiClient
import com.ekku.nfc.network.ApiService
import com.ekku.nfc.model.CustomerOrderPair
import com.ekku.nfc.repository.TagRepository
import com.ekku.nfc.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TAGViewModel(private val tagRepository: TagRepository, context: Context) : ViewModel() {

    val allTags: LiveData<List<TagAPI>> = tagRepository.allTags.asLiveData()
    //val syncTags: LiveData<List<TagAPI>> = tagRepository.syncTags.asLiveData()

    private val apiService: ApiService by lazy {
        ApiClient.apiClient(context).create(ApiService::class.java)
    }

    fun insert(tag: Tag) = viewModelScope.launch {
        tagRepository.insert(tag)
    }

    fun update(tag: Tag) = viewModelScope.launch {
        tagRepository.update(tag)
    }

    fun update(tagUpdate: TagDao.TagUpdate) = viewModelScope.launch {
        tagRepository.update(tagUpdate)
    }

    fun postCustomerOrder(consumerId: String, containersList: List<String>) =
        liveData(Dispatchers.IO) {
            emit(Resource.loading(data = null))
            try {
                emit(
                    Resource.success(
                        data = apiService.customerOrder(
                            ApiClient.gson.toJson(
                                CustomerOrderPair(
                                    consumerId,
                                    containersList
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

    fun postDropBoxData(containerId: String, dropBoxId: String) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.dropBoxData(
                        containerId,
                        dropBoxId
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

    fun getConsumersData() = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.consumersData()
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

    class TagViewModelFactory(private val repository: TagRepository, val context: Context) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TAGViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TAGViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}