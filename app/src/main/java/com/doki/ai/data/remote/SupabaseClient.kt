package com.doki.ai.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SupabaseClient(
    private var supabaseUrl: String,
    private var anonKey: String
) {
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("SupabaseClient", message)
                }
            }
        }
    }

    fun updateConfig(url: String, key: String) {
        this.supabaseUrl = url.trimEnd('/')
        this.anonKey = key
    }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank() && supabaseUrl.startsWith("http")

    suspend fun <T> get(table: String, queryParams: String = ""): Result<String> {
        if (!isConfigured) return Result.failure(IllegalStateException("Supabase is not configured"))
        return runCatching {
            val url = "$supabaseUrl/rest/v1/$table?$queryParams"
            val response = httpClient.get(url) {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                throw Exception("Supabase GET error: ${response.status} - ${response.bodyAsText()}")
            }
        }
    }

    suspend fun post(table: String, jsonBody: String): Result<String> {
        if (!isConfigured) return Result.failure(IllegalStateException("Supabase is not configured"))
        return runCatching {
            val url = "$supabaseUrl/rest/v1/$table"
            val response = httpClient.post(url) {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                throw Exception("Supabase POST error: ${response.status} - ${response.bodyAsText()}")
            }
        }
    }

    suspend fun delete(table: String, filterQuery: String): Result<String> {
        if (!isConfigured) return Result.failure(IllegalStateException("Supabase is not configured"))
        return runCatching {
            val url = "$supabaseUrl/rest/v1/$table?$filterQuery"
            val response = httpClient.delete(url) {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                throw Exception("Supabase DELETE error: ${response.status}")
            }
        }
    }
}

