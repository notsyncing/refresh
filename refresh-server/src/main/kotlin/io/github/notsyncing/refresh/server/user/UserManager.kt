package io.github.notsyncing.refresh.server.user

import io.github.notsyncing.manifold.di.EarlyProvide
import io.github.notsyncing.manifold.di.ProvideAsSingleton
import io.github.notsyncing.refresh.common.enums.LoginResult
import io.github.notsyncing.refresh.common.utils.sha256Salted
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@ProvideAsSingleton
@EarlyProvide
class UserManager {
    companion object {
        var userFileStoragePath: Path = Paths.get(".")
    }

    private val users = mutableListOf<User>()
    private val userFile = userFileStoragePath.resolve("users.txt")
    private val userLoginRands = ConcurrentHashMap<String, String>()
    private val userLoginTokens = ConcurrentHashMap<String, User>()

    init {
        loadUserFile()
    }

    private fun createUserFile() {
        val adminUser = User("admin").hashPassword("123456")
        users.add(adminUser)

        updateUserFile()
    }

    private fun loadUserFile() {
        if (!Files.exists(userFile)) {
            createUserFile()
        }

        users.clear()

        Files.newBufferedReader(userFile).use {
            it.lines().map { User.fromDataString(it) }
                    .forEach { users.add(it) }
        }
    }

    private fun updateUserFile() {
        Files.newBufferedWriter(userFile).use {
            for (u in users) {
                it.appendln(u.toDataString())
            }
        }
    }

    fun destroy() {
        updateUserFile()
    }

    fun generateLoginRand(username: String): String {
        val rand = Math.random().toString().substring(3)
        userLoginRands[username] = rand

        return rand
    }

    fun login(username: String, password: String): Pair<LoginResult, String> {
        val user = users.firstOrNull { it.username == username }

        if (user == null) {
            return Pair(LoginResult.NotExists, "")
        }

        val rand = userLoginRands[user.username]
        val passwordExpected = "${user.password}$rand".sha256Salted()

        if (password != passwordExpected) {
            return Pair(LoginResult.WrongPassword, "")
        }

        userLoginRands.remove(user.username)

        val token = "$passwordExpected${Instant.now().epochSecond}$rand".sha256Salted() ?: "HOW_COULD_YOU_HIT_THIS?"
        userLoginTokens[token] = user

        return Pair(LoginResult.Success, token)
    }

    fun logout(token: String) {
        userLoginTokens.remove(token)
    }

    fun modifyPassword(username: String, password: String) {
        users.firstOrNull { it.username == username }?.password = password

        updateUserFile()
    }

    fun modifyPassword(username: String, oldPassword: String, newPassword: String) {
        users.firstOrNull { (it.username == username) && (it.password == oldPassword) }?.password = newPassword

        updateUserFile()
    }

    fun getUsernameByToken(token: String): String? {
        return userLoginTokens[token]?.username
    }

    fun getUserByToken(token: String): User? {
        return userLoginTokens[token]
    }
}