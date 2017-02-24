package io.github.notsyncing.refresh.server.user

import io.github.notsyncing.manifold.authenticate.AuthRole
import io.github.notsyncing.manifold.authenticate.AuthenticateInformationProvider
import io.github.notsyncing.manifold.authenticate.SpecialRole
import java.util.concurrent.CompletableFuture

class SimpleUserAuth(private val userManager: UserManager) : AuthenticateInformationProvider {
    override fun getRole(id: String): CompletableFuture<AuthRole?> {
        val user = userManager.getUserByToken(id) ?: return CompletableFuture.completedFuture(null)

        if (user.username == "admin") {
            return CompletableFuture.completedFuture(SpecialRole.SuperUser)
        }

        val role = AuthRole(user.username, permissions = emptyArray())

        return CompletableFuture.completedFuture(role)
    }
}