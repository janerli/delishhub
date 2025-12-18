package com.janerli.delishhub.core.session

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {

    object Roles {
        const val GUEST = "GUEST"
        const val USER = "USER"
        const val ADMIN = "ADMIN"
    }

    data class UserSession(
        val userId: String = "guest",
        val role: String = Roles.GUEST,
        val name: String = "Гость",
        val email: String? = null
    ) {
        val isGuest: Boolean get() = role.uppercase() == Roles.GUEST || userId == "guest"
        val isAdmin: Boolean get() = role.uppercase() == Roles.ADMIN
    }

    private val _session = MutableStateFlow(UserSession())
    val session: StateFlow<UserSession> = _session.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Инициализация сессии.
     * Вызывать один раз при старте приложения (Splash / MainActivity).
     */
    fun init() {
        val user = auth.currentUser
        if (user == null) {
            setGuest()
        } else {
            setFromFirebase(user)
        }
    }

    /**
     * Обновляем сессию из FirebaseUser
     */
    fun setFromFirebase(user: FirebaseUser) {
        _session.value = UserSession(
            userId = user.uid,
            role = Roles.USER, // ADMIN можно будет подтягивать из Firestore позже
            name = user.displayName ?: user.email ?: "Пользователь",
            email = user.email
        )
    }

    /**
     * Гостевой режим (без Firebase)
     */
    fun setGuest() {
        _session.value = UserSession()
    }

    /**
     * Выход:
     * - Firebase signOut
     * - очистка session
     */
    fun signOut() {
        auth.signOut()
        setGuest()
    }
}
