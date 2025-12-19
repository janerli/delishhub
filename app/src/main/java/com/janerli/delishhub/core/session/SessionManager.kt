package com.janerli.delishhub.core.session

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {

    // ❗ Единственный реальный админ
    private const val ADMIN_EMAIL = "admin@mail.ru"
    // если захочешь — можно потом перейти на UID
    // private const val ADMIN_UID = "xxxxx"

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
        val isGuest: Boolean get() = role == Roles.GUEST
        val isAdmin: Boolean get() = role == Roles.ADMIN
    }

    private val _session = MutableStateFlow(UserSession())
    val session: StateFlow<UserSession> = _session.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun init() {
        val user = auth.currentUser
        if (user == null) setGuest() else setFromFirebase(user)
    }

    fun setFromFirebase(user: FirebaseUser) {
        val email = user.email

        val role = if (
            !email.isNullOrBlank() &&
            email.equals(ADMIN_EMAIL, ignoreCase = true)
        ) {
            Roles.ADMIN
        } else {
            Roles.USER
        }

        _session.value = UserSession(
            userId = user.uid,
            role = role,
            name = user.displayName ?: email ?: "Пользователь",
            email = email
        )
    }

    fun setGuest() {
        _session.value = UserSession()
    }

    fun signOut() {
        auth.signOut()
        setGuest()
    }
}
