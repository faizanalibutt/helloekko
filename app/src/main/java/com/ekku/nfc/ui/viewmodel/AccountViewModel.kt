package com.ekku.nfc.ui.viewmodel

import androidx.lifecycle.*
import com.ekku.nfc.model.Resource
import com.ekku.nfc.network.ApiClient
import com.ekku.nfc.network.ApiService
import com.ekku.nfc.repository.AccountRepository
import kotlinx.coroutines.Dispatchers

class AccountViewModel(repository: AccountRepository): ViewModel() {

    // it will handle all of the users login like consumer, partner, admin

    private val apiService: ApiService by lazy { ApiClient.apiClient().create(ApiService::class.java) }

    /**
     * Admin API
     * @param username email provided by admin & partner
     * @param password must be at least 6 characters.
     */
    fun postAdminCredentials(
        username: String,
        password: String
    ) = liveData(Dispatchers.IO) {
        try {
            emit(
                Resource.success(
                    data = apiService.adminCredentials(
                        username,
                        password
                    )
                )
            )
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error message unknown"))
        }
    }

    /**
     * Partner API
     * @param username email provided by admin & partner
     * @param password must be at least 6 characters.
     */
    fun postPartnerCredentials(
        username: String,
        password: String
    ) = liveData(Dispatchers.IO) {
        try {
            emit(
                Resource.success(
                    data = apiService.partnerCredentials(
                        username,
                        password
                    )
                )
            )
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error message unknown"))
        }
    }

    /**
     * @param username only this is required for dropbox
     */
    fun postDropBoxCredentials(
        username: String
    ) = liveData(Dispatchers.IO) {
        try {
            emit(
                Resource.success(
                    data = apiService.dropBoxCredentials(
                        username
                    )
                )
            )
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error message unknown"))
        }
    }

    class AccountViewModelFactory(private val repository: AccountRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AccountViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}