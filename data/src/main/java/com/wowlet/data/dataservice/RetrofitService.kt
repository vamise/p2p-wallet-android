package com.wowlet.data.dataservice


import com.wowlet.entities.responce.CallRequest
import com.wowlet.entities.responce.ResponseData
import com.wowlet.entities.responce.ResponseDataAirDrop
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RetrofitService {
    @Headers( "Content-Type: application/json")
    @POST(".")
   suspend fun getBalance(@Body jsonObject: CallRequest): Response<ResponseData>
    @Headers( "Content-Type: application/json")
    @POST(".")
   suspend fun requestAirdrop(@Body jsonObject: CallRequest): Response<ResponseDataAirDrop>
}