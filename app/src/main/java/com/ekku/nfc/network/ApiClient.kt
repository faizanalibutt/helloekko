package com.ekku.nfc.network

import android.content.Context
import com.ekku.nfc.ui.activity.AccountActivity
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_PREF
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_TOKEN
import com.ekku.nfc.util.getDefaultPreferences
import com.google.firebase.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber


object ApiClient {

    //please use your own url
    private var BASE_URL: String = /*"https://docs.google.com/forms/d/e/"*/
        if (BuildConfig.DEBUG) "https://ekkocommunity.uc.r.appspot.com/api/user/" else
            "https://ekkoreuse.uc.r.appspot.com/api/user/"

    val gson: Gson = GsonBuilder().setLenient().create()

    fun apiClient(context: Context? = null): Retrofit {

        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val okHttpClient: OkHttpClient =
            OkHttpClient.Builder().addInterceptor(
                if (context?.getDefaultPreferences()?.getBoolean(LOGIN_PREF, false) == true)
                    Interceptor { chain ->
                        Timber.d(
                            "LOGIN_TOKEN is: ${
                                context.getDefaultPreferences().getString(
                                    LOGIN_TOKEN, null
                                )
                            }"
                        )
                        val newRequest: Request = chain.request().newBuilder()
                            .addHeader(
                                "x-auth-token",
                                "${
                                    context.getDefaultPreferences().getString(
                                        LOGIN_TOKEN, null
                                    )
                                }"
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