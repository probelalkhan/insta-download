package net.simplifiedcoding.instadownload.network

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface MyApi {

    @GET
    suspend fun getInstaPostInfo(
        @Url url: String
    ): ResponseBody

    companion object {
        private const val BASE_URL = "https://www.simplifiedcoding.net/"
        operator fun invoke(): MyApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().also {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    it.addInterceptor(loggingInterceptor)
                }.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MyApi::class.java)
        }
    }
}