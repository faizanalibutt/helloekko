package com.ekku.nfc.network

import com.ekku.nfc.model.*
import com.ekku.nfc.network.ApiService.Companion.DOWNLOAD_CONTAINER_SIZE
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    /**
     * partner & admin login are same
     *
     * partner token info
     * {
    "id": "Ny7ePTuliflJ8N14qDo2",
    "created": "2021-07-13T13:54:59.223Z",
    "pMenu": "https://react-icons.github.io/react-icons/search?q=container",
    "city": "Glace Bay",
    "user": "1mEECTbL7P4L9qWRCCso",
    "lastUpdate": "2021-07-25T17:25:58.676Z",
    "pURL": "https://react-icons.github.io/react-icons/search?q=container",
    "partnerName": "Xufya",
    "address": "Street No 1,Haji park",
    "launchDate": "2021-07-13T13:53:56.763Z",
    "state": "Nova Scotia",
    "email": "xufyan.wbm@gmail.com",
    "postalCode": "X1X1X1",
    "contact": "xuyfan_partner",
    "pLogo": "https://storage.googleapis.com/storage-ekkocommunity/download.png",
    "phoneNo": "(321) 678-8787",
    "archive": false,
    "region": "ALBERTA",
    "iat": 1627382819,
    "exp": 1785170819
     * }
     *
     * admin token info
     *
     * {
    "id": "1mEECTbL7P4L9qWRCCso",
    "image": "https://storage.googleapis.com/storage-ekkocommunity/download.png",
    "name": "Sufyan ali",
    "postalCode": "X1X1X1",
    "city": "ALBERTA",
    "archive": false,
    "role": "ADMIN",
    "email": "samplehy123@gmail.com",
    "created": "2021-07-13T13:42:52.551Z",
    "firstName": "Sufyan",
    "state": "Alberta",
    "region": "ALBERTA",
    "user": "Hyx56UxpUPkQe8VSA1rO",
    "phoneNo": "(321) 482-2702",
    "lastName": "ali",
    "address": "Street No 1,Haji park",
    "lastUpdate": "2021-07-15T13:24:39.631Z",
    "status": "active",
    "iat": 1627395397,
    "exp": 1785183397
     * }
     *
     * Partner api
     */
    @FormUrlEncoded
    @POST(PARTNER_LOGIN)
    suspend fun partnerCredentials(
        @Field("email") username: String,
        @Field("password") password: String,
    ): Account

    // admin api
    @FormUrlEncoded
    @POST(ADMIN_LOGIN)
    suspend fun adminCredentials(
        @Field("email") username: String,
        @Field("password") password: String,
    ): Account

    /**
     * Dropbox login api
     *
     * token info
     *
     * {
    "id": "RZUGuVtWml9pxFsYTZ51",
    "region": "ALBERTA",
    "state": "Alberta",
    "postalCode": "X1X1X1",
    "created": "2021-07-13T13:52:33.474Z",
    "contact": "xufyan_ali",
    "type": "INDOOR VERSION",
    "size": "LARGE",
    "empty": false,
    "address": "Street No 1,Haji park",
    "phoneNo": "(321) 720-2004",
    "city": "GLACE BAI",
    "launchDate": "2021-07-13T13:51:21.725Z",
    "user": "1mEECTbL7P4L9qWRCCso",
    "dropboxName": "xufyan_dropbox",
    "lastUpdate": "2021-07-26T10:50:23.735Z",
    "email": "xufyan.wbm@gmail.com",
    "dbURL": "https://www.wezedotivom.co",
    "archive": false,
    "iat": 1627395769,
    "exp": 1628000569
     * }
     *
     * */
    @FormUrlEncoded
    @POST(DROPBOX_LOGIN)
    suspend fun dropBoxCredentials(
        @Field("dropboxName") username: String
    ): Account

    /**
     * consumers data to verify customer upon order from partner.
     * */
    @GET(CONSUMERS_DATA)
    suspend fun consumersData(): Customer

    /**
     * customer order will be send along with containers list
     */
    @POST(CUSTOMER_ORDER)
    @Headers("Content-Type: application/json")
    suspend fun customerOrder(
        @Body customerOrderPair: String
    ): GenericResponse

    /**
     * on each scan this api will be called and send data to firebase
     */
    @POST(DROPBOX_SCAN)
    @FormUrlEncoded
    suspend fun dropBoxData(
        @Field("containerId") container_id: String,
        @Field("dropboxId") dropBox_id: String,
    ): String

    /**
     * device logs are sending using thig api to specific location in firestore LOGS
     */
    @POST(UPLOAD_DEVICE)
    @FormUrlEncoded
    fun sendDeviceInfo(
        @Field("id") user_id: String?,
        @Field("name") name: String,
        @Field("latitude") latitude: String,
        @Field("longitude") longitude: String,
        @Field("imei") phone_uid: String,
        @Field("battery") battery: String,
        @Field("network") network: String,
    ): Call<String>

    /////////////////// ADMIN MODES APIS \\\\\\\\\\\\\\\\\\\

    /**
     * adding containers by using fleet api
     */
    @POST(ADMIN_FLEET_MODE)
    @Headers("Content-Type: application/json")
    suspend fun postContainersFleet(
         @Body fleetContainers: String
    ): GenericResponse

    /**
     * assigning containers to different partners using this api
     */
    @POST(ADMIN_ASSIGN_MODE)
    @Headers("Content-Type: application/json")
    suspend fun postContainersAssign(
        @Body assignPartners: String,
    ): GenericResponse

    /**
     * fetching most valuable partners of ekko
     */
    @GET(ADMIN_ASSIGN_PARTNER_LIST)
    suspend fun fetchPartners(): PartnerShell

    /**
     * this implies when container is returned from dropbox
     */
    @POST(ADMIN_CHECK_IN_MODE)
    @Headers("Content-Type: application/json")
    suspend fun postContainersCheckIN(
        @Body containersCheckIn: String
    ): GenericResponse

    /**
     * finally container is empty and ready to dump in dropbox
     */
    @POST(ADMIN_EMPTY_MODE)
    @FormUrlEncoded
    suspend fun postContainersEmpty(
        @Field("dropboxId") id: String,
        @Field("latitude") latitude: Float,
        @Field("longitude") longitude: Float
    ): GenericResponse

    /**
     * we got dropboxes list hurrah!
     */
    @GET(ADMIN_EMPTY_DROPBOX_LIST)
    suspend fun gatherDropBoxes(): DropBoxShell

    /**
     * item is going to be retired.
     */
    @POST(ADMIN_RETIRED_MODE)
    @Headers("Content-Type: application/json")
    suspend fun postContainersRetired(
        @Body containersCheckIn: String,
    ): GenericResponse

    /**
     * dropbox didn't scan container so we are doing it at the time checkIn
     */
    @POST(ADMIN_RETURN_CONTAINER_STATUS)
    @FormUrlEncoded
    suspend fun postContainersReturnStatus(
        @Field("containerId") tagUid: String,
        @Field("dropboxId") argCheckInDropbox: String
    ): GenericResponse

    /**
     * customer is new not present in our system.
     */
    @POST(POST_NEW_CUSTOMER)
    @FormUrlEncoded
    suspend fun postNewCustomer(
        @Field("phoneNo") customerNumber: String
    ): GenericResponse

    /**
     * Item type is a lot.
     */
    @GET(DOWNLOAD_CONTAINER_TYPE)
    suspend fun getItemType(): ItemShell

    /**
     * Item size is of variuos types.
     */
    @GET(DOWNLOAD_CONTAINER_SIZE)
    suspend fun getItemSize(): ItemShell

    companion object {
        const val PARTNER_LOGIN = "partner/signin"
        const val DROPBOX_LOGIN = "dropbox/signin"
        const val ADMIN_LOGIN = "admin/admin/signin"
        const val CONSUMERS_DATA = "partner/get/userlist"
        const val CUSTOMER_ORDER = "partner/consumer/order"
        const val DROPBOX_SCAN = "order/consumer/container/return"
        const val ADMIN_FLEET_MODE = "admin/admin/add/container/fleet"
        const val ADMIN_ASSIGN_MODE = "admin/admin/assign/container/partner"
        const val ADMIN_ASSIGN_PARTNER_LIST = "admin/admin/get/partner/list"
        const val ADMIN_CHECK_IN_MODE = "take/containers/dropbox"
        const val ADMIN_RETURN_CONTAINER_STATUS = "check/status/order/container"
        const val ADMIN_EMPTY_MODE = "admin/admin/makedropboxempty"
        const val ADMIN_EMPTY_DROPBOX_LIST = "admin/admin/get/dropbox/list"
        const val ADMIN_RETIRED_MODE = "admin/admin/containers/retired"
        const val UPLOAD_DEVICE = "logs"
        const val DOWNLOAD_CONTAINER_TYPE = "admin/admin/list/containerType"
        const val DOWNLOAD_CONTAINER_SIZE = "admin/admin/list/containerSize"
        const val POST_NEW_CUSTOMER = "partner/EkkoId/unconfirmed"
    }
}