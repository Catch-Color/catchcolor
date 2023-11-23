package com.example.catchcolors

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object FileUploadUtils {
    fun goSend(file: File, isSkirtChecked: Boolean, isCoatChecked: Boolean) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("isSkirtChecked", isSkirtChecked.toString())
            .addFormDataPart("isCoatChecked",isCoatChecked.toString())
            .build()

        val request = Request.Builder()
            .url("http://43.201.32.24/upload") // 서버 URL 입력
            .post(requestBody)
            .build()

        var delaymills = 25000 //이미지 업로드 최대 딜레이?

        val client = OkHttpClient.Builder()
            .connectTimeout(delaymills.toLong(), TimeUnit.SECONDS)
            .readTimeout(delaymills.toLong(), TimeUnit.SECONDS)
            .writeTimeout(delaymills.toLong(), TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("TEST: ", response.body?.string() ?: "No response")
            }
        })
    }
}