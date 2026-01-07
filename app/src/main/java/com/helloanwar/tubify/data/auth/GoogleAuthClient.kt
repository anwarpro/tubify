package com.helloanwar.tubify.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import com.helloanwar.tubify.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GoogleAuthClient(
    private val context: Context
) {
    private val signInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/youtube.readonly"))
            // .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID) // Not strictly needed for access token if using GoogleAuthUtil, but good for backend.
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }

    suspend fun signInWithIntent(intent: Intent): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.await()
            Result.success(account)
        } catch (e: ApiException) {
            Result.failure(e)
        }
    }

    suspend fun getAccessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val scope = "oauth2:https://www.googleapis.com/auth/youtube.readonly"
            GoogleAuthUtil.getToken(context, account.account!!, scope)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun signOut() {
        signInClient.signOut().await()
    }
}
