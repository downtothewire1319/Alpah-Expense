package com.example.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

object PinSecurityManager {

    private val _isPrivateUnlocked = MutableStateFlow(false)
    val isPrivateUnlocked: StateFlow<Boolean> = _isPrivateUnlocked.asStateFlow()

    private var lastActiveTime: Long = System.currentTimeMillis()
    var autoLockMinutes: Int = 5
    var autoLockEnabled: Boolean = true

    fun onUserActivity() {
        if (_isPrivateUnlocked.value) {
            checkAutoLock()
            lastActiveTime = System.currentTimeMillis()
        }
    }

    fun checkAutoLock(): Boolean {
        if (!_isPrivateUnlocked.value || !autoLockEnabled) return false
        val elapsedMillis = System.currentTimeMillis() - lastActiveTime
        val maxIdleMillis = autoLockMinutes * 60 * 1000L
        if (elapsedMillis >= maxIdleMillis) {
            lock()
            return true
        }
        return false
    }

    fun unlock() {
        _isPrivateUnlocked.value = true
        lastActiveTime = System.currentTimeMillis()
    }

    fun lock() {
        _isPrivateUnlocked.value = false
    }

    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$pin"
        val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
