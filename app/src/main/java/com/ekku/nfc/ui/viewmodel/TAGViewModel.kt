package com.ekku.nfc.ui.viewmodel

import androidx.lifecycle.*
import com.ekku.nfc.model.*
import com.ekku.nfc.network.ApiClient
import com.ekku.nfc.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TAGViewModel(private val tagRepository: TagRepository) : ViewModel() {

    val allTags: LiveData<List<TagAPI>> = tagRepository.allTags.asLiveData()

    private val apiService: ApiService by lazy { ApiClient.apiClient().create(ApiService::class.java) }

    fun insert(tag: Tag) = viewModelScope.launch {
        tagRepository.insert(tag)
    }

    fun update(tag: Tag) = viewModelScope.launch {
        tagRepository.update(tag)
    }

    fun update(tagUpdate: TagDao.TagUpdate) = viewModelScope.launch {
        tagRepository.update(tagUpdate)
    }

    fun postTag(tagData: TagAPI) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try {
            emit(
                Resource.success(
                    data = apiService.addTagData(
                        tagData.id.toString(),
                        tagData.tag_uid,
                        tagData.tag_date_time,
                        tagData.tag_phone_uid,
                        tagData.tag_sync.toString()
                    )
                )
            )
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error occured"))
        }
    }

    class TagViewModelFactory(private val repository: TagRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TAGViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TAGViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}