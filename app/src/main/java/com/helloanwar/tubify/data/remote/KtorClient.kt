package com.helloanwar.tubify.data.remote

import android.util.Log
import com.helloanwar.tubify.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KtorClient {
    private const val BASE_URL = "www.googleapis.com/youtube/v3"

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message)
                }
            }
            level = LogLevel.ALL
        }

        install(DefaultRequest) {
            url {
                protocol = URLProtocol.HTTPS
                host = BASE_URL
                parameters.append("key", BuildConfig.YOUTUBE_API_KEY)
            }
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }
}
