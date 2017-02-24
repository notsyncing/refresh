package io.github.notsyncing.refresh.server.scenes.client

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.server.client.ClientManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.GetClientUpdatePhase, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class GetClientUpdatePhaseScene(private val accountId: String) : ManifoldScene<Int>() {
    @AutoProvide
    lateinit var clientManager: ClientManager

    constructor() : this("")

    override fun stage(): CompletableFuture<Int> {
        return clientManager.getClientUpdatePhase(accountId)
    }
}