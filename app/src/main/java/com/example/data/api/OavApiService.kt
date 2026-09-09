package com.example.data.api

import com.example.data.model.ChatRequest
import com.example.data.model.ChatResponse
import com.example.data.model.NotesResponse
import com.example.data.model.SimpleApiResponse
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

interface OavApiService {
    @GET("api/notes")
    suspend fun getNotes(): NotesResponse

    @POST("api/chat")
    suspend fun sendChat(@Body request: ChatRequest): ChatResponse

    @Multipart
    @POST("api/upload-note")
    suspend fun uploadNote(
        @Part("username") username: RequestBody,
        @Part("password") password: RequestBody,
        @Part("classNum") classNum: RequestBody,
        @Part("subject") subject: RequestBody,
        @Part("title") title: RequestBody,
        @Part pdf: MultipartBody.Part
    ): SimpleApiResponse

    @HTTP(method = "DELETE", path = "api/delete-note/{id}", hasBody = true)
    suspend fun deleteNote(
        @retrofit2.http.Path("id", encoded = true) noteId: String,
        @Body body: Map<String, String>
    ): SimpleApiResponse
}

object ApiClient {
    private const val BASE_URL = "https://oav-hub.onrender.com/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: OavApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OavApiService::class.java)
    }
}
