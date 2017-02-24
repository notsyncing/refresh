package io.github.notsyncing.refresh.server.services

import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpPost
import io.github.notsyncing.cowherd.service.CowherdService
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.scenes.client.GetClientListScene
import io.github.notsyncing.refresh.server.scenes.client.GetClientUpdatePhaseScene
import io.github.notsyncing.refresh.server.scenes.client.SetClientUpdatePhaseScene
import java.net.HttpCookie
import java.util.concurrent.CompletableFuture

class ClientService : CowherdService() {
    @Exported
    @HttpGet
    fun getClientList(@Parameter("token") token: HttpCookie?): CompletableFuture<List<Client>> {
        return Manifold.run(GetClientListScene(), token?.value)
    }

    @Exported
    @HttpGet
    fun getClientUpdatePhase(@Parameter("client") clientAccountId: String,
                             @Parameter("token") token: HttpCookie?): CompletableFuture<Int> {
        return Manifold.run(GetClientUpdatePhaseScene(clientAccountId), token?.value)
    }

    @Exported
    @HttpPost
    fun setClientUpdatePhase(@Parameter("client") clientAccountId: String,
                             @Parameter("phase") phase: Int,
                             @Parameter("token") token: HttpCookie?): CompletableFuture<OperationResult> {
        return Manifold.run(SetClientUpdatePhaseScene(clientAccountId, phase), token?.value)
    }
}