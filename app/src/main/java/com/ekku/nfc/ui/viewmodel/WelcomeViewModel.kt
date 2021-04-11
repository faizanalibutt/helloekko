package com.ekku.nfc.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ekku.nfc.ui.activity.ConsumerActivity
import com.ekku.nfc.ui.activity.RestaurantActivity
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.getDefaultPreferences
import timber.log.Timber

class WelcomeViewModel : ViewModel() {

    // TODO: 4/8/21 make DI for this action.
    fun handleButtonAction(activity: Context?) {
        when(activity?.getDefaultPreferences()?.getInt("APP_TYPE", -1)) {
            0 -> activity.startActivity<RestaurantActivity>()
            1 -> activity.startActivity<ConsumerActivity>()
            else -> Timber.d("nothing is selected, i will not come here.")
        }
    }

}