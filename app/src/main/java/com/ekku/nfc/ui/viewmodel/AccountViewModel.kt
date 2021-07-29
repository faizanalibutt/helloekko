package com.ekku.nfc.ui.viewmodel

import androidx.lifecycle.*
import com.ekku.nfc.model.Resource
import com.ekku.nfc.network.ApiClient
import com.ekku.nfc.network.ApiService
import com.ekku.nfc.repository.AccountRepository
import com.ekku.nfc.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException

class AccountViewModel(repository: AccountRepository) : ViewModel() {

    // it will handle all of the users login like consumer, partner, admin

    private val apiService: ApiService by lazy {
        ApiClient.apiClient().create(ApiService::class.java)
    }

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
            emit(
                Resource.error(
                    data = null,
                    message = NetworkUtils.getError(exception as HttpException)
                )
            )
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
            emit(
                Resource.error(
                    data = null,
                    message = NetworkUtils.getError(exception as HttpException)
                )
            )
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
                Resource.success(data = apiService.dropBoxCredentials(username))
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

    class AccountViewModelFactory(private val repository: AccountRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AccountViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}