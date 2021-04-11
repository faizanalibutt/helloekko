package com.ekku.nfc.network

import com.ekku.nfc.network.ApiService.Companion.UPLOAD_DEVICE
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    @POST(UPLOAD)
    @FormUrlEncoded
    suspend fun addTagData(
        @Field("entry.1034948794") id: String,
        @Field("entry.709515950") tag_uid: String,
        @Field("entry.1816688302") time: String,
        @Field("entry.487588514") phone_uid: String,
        @Field("entry.94249200") sync: String,
        @Field("entry.225872179") orderId: String
    ) : String

    @POST(UPLOAD_MIDNIGHT)
    @FormUrlEncoded
    fun sendDailyLogs(
        @Field("entry.2050290841") id: String,
        @Field("entry.2000132953") tag_uid: String,
        @Field("entry.650306471") time: String,
        @Field("entry.993409874") phone_uid: String,
        @Field("entry.521009648") sync: String,
        @Field("entry.392715999") orderId: String
    ) : Call<String>

    @POST(UPLOAD_DEVICE)
    @FormUrlEncoded
    fun sendDeviceInfo(
        @Field("entry.27789299") phone_guid: String,
        @Field("entry.525546522") time: String?,
        @Field("entry.1439954571") battery: String,
        @Field("entry.1478527647") location: String,
        @Field("entry.1687984879") network: String,
    ) : Call<String>

    companion object {
        const val UPLOAD = "1FAIpQLSfxztJxtyUSYWInI8s9qyLTv7PegtEhW3Hjw5VxyfzAmOmVkg/formResponse"
        const val UPLOAD_MIDNIGHT = "1FAIpQLSfjCrPKauMMS6M9wutJ6BEEdDGMZdV0HCoohKHRmSabGaYFkw/formResponse"
        const val UPLOAD_DEVICE = "1FAIpQLSdGd0ZqwD2OtyPNU5RPZzXV-1HJkyJVxiNk4sEUeq0Ife9zWg/formResponse"
    }
}