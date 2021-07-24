package com.ekku.nfc.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ekku.nfc.repository.AccountRepository

class AccountViewModel(repository: AccountRepository): ViewModel() {
    // it will handle all of the users login like consumer, partner, admin

    val adminMode = MutableLiveData<String>()


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