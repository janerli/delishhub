package com.janerli.delishhub.feature.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

object GoogleAuthHelper {

    fun signInIntent(context: Context): Intent {
        val webClientId = GoogleAuthConfig.WEB_CLIENT_ID.trim()
        check(webClientId.isNotEmpty() && webClientId != "REPLACE_ME_WEB_CLIENT_ID") {
            "Google Sign-In не настроен: вставь WEB_CLIENT_ID в GoogleAuthConfig.WEB_CLIENT_ID"
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()

        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    /**
     * Достаём idToken из результата Google Sign-In.
     */
    fun getIdTokenFromResult(data: Intent?): String {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account: GoogleSignInAccount = try {
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            throw IllegalStateException("Google Sign-In отменён/ошибка: ${e.statusCode}", e)
        }

        return account.idToken ?: throw IllegalStateException(
            "idToken == null. Проверь: включён Google provider, добавлен SHA-1, " +
                    "и WEB_CLIENT_ID взят именно от Web client в Firebase."
        )
    }
}
