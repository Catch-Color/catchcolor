package com.example.catchcolors

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RetrofitAPI {
    @GET("/get_data") // 서버에 GET 요청을 할 엔드포인트 입력
    fun getTodoList() : Call<JsonObject> // json 파일을 가져오는 메소드

    @GET("/get_image/{imageName}") // 새로운 엔드포인트: 이미지 가져오기
    fun getImage(@Path("imageName") imageName: String): Call<ResponseBody>
}