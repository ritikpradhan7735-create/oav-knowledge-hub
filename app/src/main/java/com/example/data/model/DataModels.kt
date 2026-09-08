package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NoteItem(
    @Json(name = "id") val id: String,
    @Json(name = "class") val classLevel: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "title") val title: String,
    @Json(name = "fileUrl") val fileUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class NotesResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "notes") val notes: List<NoteItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "prompt") val prompt: String
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "reply") val reply: String? = null,
    @Json(name = "success") val success: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class AdminAuthRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SimpleApiResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "note") val note: NoteItem? = null
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
