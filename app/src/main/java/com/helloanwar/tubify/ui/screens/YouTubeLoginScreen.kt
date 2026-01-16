package com.helloanwar.tubify.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLoginScreen(
    email: String? = null,
    onLoginSuccess: () -> Unit,
    onClose: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.savePassword = true
                settings.saveFormData = true
                
                // Ensure cookies are enabled
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: ""
                        Log.d("YouTubeLogin", "Loading URL: $url")
                        
                        if (url.startsWith("https://m.youtube.com") || 
                            url.startsWith("https://www.youtube.com")) {
                            Log.d("YouTubeLogin", "Logged in successfully")
                            onLoginSuccess()
                            return false // Allow loading the YouTube page, or return true to stop here
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d("YouTubeLogin", "Page started: $url")
                        if (url?.startsWith("https://m.youtube.com") == true || 
                            url?.startsWith("https://www.youtube.com") == true) {
                            Log.d("YouTubeLogin", "Logged in successfully (onPageStarted)")
                            onLoginSuccess()
                        }
                    }
                }
                
                val baseUrl = "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Dm%26hl%3Dtr%26next%3Dhttps%253A%252F%252Fm.youtube.com%252F"
                val loadUrl = if (email != null) "$baseUrl&Email=$email" else baseUrl
                loadUrl(loadUrl)
            }
        }
    )
}
