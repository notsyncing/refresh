package io.github.notsyncing.refresh.cli.commands

import com.mashape.unirest.http.Unirest
import io.github.notsyncing.refresh.cli.RefreshCliApp
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.common.utils.sha256Salted
import java.net.HttpCookie

class UserCommands(private val app: RefreshCliApp) : CommandBase() {
    @Command
    fun server(scheme: String, h: String) {
        host = "$scheme://${h.trimEnd('/')}"

        println("Server changed to $host")
    }

    @Command
    fun exit() {
        app.stop()
    }

    @Command
    fun login(username: String, password: String) {
        val randCode = get("UserService/getLoginRand", listOf("username" to username))
        val stage1 = password.sha256Salted()
        val stage2 = "$stage1$randCode".sha256Salted()

        val rLogin = Unirest.post(api("UserService/login"))
                .field("username", username)
                .field("password", stage2)
                .asString()

        if (rLogin.status != 200) {
            throw RuntimeException("Error when posting UserService/login: ${rLogin.status} ${rLogin.statusText}")
        }

        val r = rLogin.body

        val tokenCookie = rLogin.headers["Set-Cookie"]?.flatMap { HttpCookie.parse(it) }
                ?.firstOrNull { it.name == "token" }

        if (tokenCookie == null) {
            println("Login succeeded, but server does not returned a token!")
            return
        }

        token = tokenCookie.value

        if (r != OperationResult.Success.ordinal.toString()) {
            println("Failed to login: Server returned $r")
            return
        }

        println("Login succeed.")
    }

    @Command
    fun logout() {
        post("UserService/logout")

        token = ""

        print("Logout succeed.")
    }

    @Command
    fun modifyPassword(currentPassword: String, newPassword: String) {
        val cp = currentPassword.sha256Salted()
        val np = newPassword.sha256Salted()

        val r = post("UserService/modifyPassword", listOf("currentPassword" to cp, "newPassword" to np))

        if (r != OperationResult.Success.ordinal.toString()) {
            println("Failed to modify password: Server returned $r")
            return
        }

        println("Password modified.")
    }
}