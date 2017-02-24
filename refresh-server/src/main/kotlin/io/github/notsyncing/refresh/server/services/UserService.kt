package io.github.notsyncing.refresh.server.services

import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpPost
import io.github.notsyncing.cowherd.service.CowherdService
import io.github.notsyncing.refresh.common.enums.LoginResult
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.user.UserManager
import io.vertx.core.http.HttpServerRequest
import java.net.HttpCookie

class UserService(private val userManager: UserManager) : CowherdService() {
    @Exported
    @HttpGet
    fun getLoginRand(@Parameter("username") username: String): String {
        return userManager.generateLoginRand(username)
    }

    @Exported
    @HttpPost
    fun login(@Parameter("username") username: String,
              @Parameter("password") password: String, request: HttpServerRequest): LoginResult {
        val (r, token) = userManager.login(username, password)
        putCookie(request, HttpCookie("token", token))

        return r
    }

    @Exported
    @HttpPost
    fun logout(@Parameter("token") token: HttpCookie?, request: HttpServerRequest): OperationResult {
        val v = token?.value ?: return OperationResult.NoAuth
        val cookie = HttpCookie("token", v)
        cookie.maxAge = 0

        userManager.logout(v)
        putCookie(request, cookie)

        return OperationResult.Success
    }

    @Exported
    @HttpPost
    fun modifyPassword(@Parameter("token") token: HttpCookie?,
                       @Parameter("currentPassword") currentPassword: String,
                       @Parameter("newPassword") newPassword: String): OperationResult {
        val v = token?.value ?: return OperationResult.NoAuth
        val username = userManager.getUsernameByToken(v) ?: return OperationResult.Failed

        userManager.modifyPassword(username, currentPassword, newPassword)

        return OperationResult.Success
    }
}