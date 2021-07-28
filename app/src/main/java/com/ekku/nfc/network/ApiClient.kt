package com.ekku.nfc.network

import android.content.Context
import com.ekku.nfc.ui.activity.AccountActivity
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_PREF
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_TOKEN
import com.ekku.nfc.util.getDefaultPreferences
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


object ApiClient {

    //please use your own url
    private var BASE_URL: String = /*"https://docs.google.com/forms/d/e/"*/
        "https://ekkocommunity.uc.r.appspot.com/api/user/"

    fun apiClient(context: Context? = null): Retrofit {

        val gson = GsonBuilder().setLenient().create()

        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val okHttpClient: OkHttpClient =
            OkHttpClient.Builder().addInterceptor(
                if (context?.getDefaultPreferences()?.getBoolean(LOGIN_PREF, false) == true)
                    Interceptor { chain ->
                        val newRequest: Request = chain.request().newBuilder()
                            .addHeader(
                                "x-auth-token",
                                "${context.getDefaultPreferences().getString(LOGIN_TOKEN, null)}"
                            )
                            .build()
                        chain.proceed(newRequest)
                    } else interceptor
            ).build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    }
}