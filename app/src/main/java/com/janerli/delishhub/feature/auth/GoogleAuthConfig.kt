package com.janerli.delishhub.feature.auth

/**
 * ВАЖНО:
 * Для Google Sign-In нужен WEB_CLIENT_ID (OAuth 2.0 Client ID типа "Web client").
 *
 * Где взять:
 * Firebase Console → Authentication → Sign-in method → Google (включить)
 * + Project settings → SHA-1/SHA-256 добавить
 * + скачать обновлённый google-services.json
 *
 * Затем вставь сюда WEB_CLIENT_ID (выглядит как: 1234-abcdefg.apps.googleusercontent.com)
 */
object GoogleAuthConfig {
    const val WEB_CLIENT_ID = "433786748186-nuef5s0kbtup40028dlpe4rgglmves8m.apps.googleusercontent.com"
}
