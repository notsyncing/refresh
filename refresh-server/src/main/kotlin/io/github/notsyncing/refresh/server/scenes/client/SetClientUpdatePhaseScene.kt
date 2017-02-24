package io.github.notsyncing.refresh.server.scenes.client

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.client.ClientManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.SetClientUpdatePhase, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class SetClientUpdatePhaseScene(private val accountId: String,
                                private val phase: Int) : ManifoldScene<OperationResult>() {
    @AutoProvide
    lateinit var clientManager: ClientManager

    constructor() : this("", 0)

    override fun stage(): CompletableFuture<OperationResult> {
        return clientManager.setClientUpdatePhase(accountId, phase)
    }
}