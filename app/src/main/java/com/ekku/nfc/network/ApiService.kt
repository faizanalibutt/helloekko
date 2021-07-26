package com.ekku.nfc.network

import com.ekku.nfc.network.ApiService.Companion.PARTNER_LOGIN
import com.ekku.nfc.network.ApiService.Companion.UPLOAD_DEVICE
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    @POST(UPLOAD)
    @FormUrlEncoded
    suspend fun addTagData(
        @Field("entry.84444851") id: String,
        @Field("entry.2070776725") tag_uid: String,
        @Field("entry.645920834") time: String,
        @Field("entry.2073481602") phone_uid: String,
        @Field("entry.1002271193") sync: String,
        @Field("entry.1359494710") orderId: String
    ) : String

    @POST(UPLOAD_MIDNIGHT)
    @FormUrlEncoded
    fun sendDailyLogs(
        @Field("entry.595285169") id: String,
        @Field("entry.1441080511") tag_uid: String,
        @Field("entry.1330180435") time: String,
        @Field("entry.1694076907") phone_uid: String,
        @Field("entry.1227786594") sync: String,
        @Field("entry.59441528") orderId: String
    ) : Call<String>

    @POST(UPLOAD_DEVICE)
    @FormUrlEncoded
    fun sendDeviceInfo(
        @Field("entry.1547847935") phone_guid: String?,
        @Field("entry.2011914284") time: String,
        @Field("entry.798199591") battery: String,
        @Field("entry.780089548") location: String,
        @Field("entry.1733715185") network: String,
    ) : Call<String>

    @POST(PARTNER_LOGIN)
    @FormUrlEncoded
    suspend fun adminCredentials(
        @Field("email") email: String,
        @Field("password") password: String,
    ) : String

    companion object {
        const val UPLOAD = "1FAIpQLSfOisc3AeD_Zo8uUqa_7pJW1MwaGRqyLGda8jafoXzFaO3HSg/formResponse"
        const val UPLOAD_MIDNIGHT = "1FAIpQLScFRWXUCXgswy7KTs7q1PsZ3N84wPDYjwvXq6-kfCLEWB2ESw/formResponse"
        const val UPLOAD_DEVICE = "1FAIpQLSeoCootOf6W3knjC4scF0h-j_U0fs4gRQeBO-2235nM0Zi0AQ/formResponse"
        const val PARTNER_LOGIN = "partner/signin"
        const val DROPBOX_LOGIN = "dropbox/signin"
        const val ADMIN_LOGIN = "admin/admin/signin"
    }
}