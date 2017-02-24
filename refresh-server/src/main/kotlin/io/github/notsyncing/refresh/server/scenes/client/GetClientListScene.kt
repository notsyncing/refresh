package io.github.notsyncing.refresh.server.scenes.client

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.server.client.ClientManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.GetClientList, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class GetClientListScene : ManifoldScene<List<Client>>() {
    @AutoProvide
    lateinit var clientManager: ClientManager

    override fun stage(): CompletableFuture<List<Client>> {
        return clientManager.getClientList()
    }
}