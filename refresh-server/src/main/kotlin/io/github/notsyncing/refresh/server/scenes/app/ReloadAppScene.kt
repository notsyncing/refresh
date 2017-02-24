package io.github.notsyncing.refresh.server.scenes.app

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.ReloadApp, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class ReloadAppScene : ManifoldScene<OperationResult>() {
    @AutoProvide
    lateinit var appManager: AppManager

    override fun stage(): CompletableFuture<OperationResult> {
        appManager.reload()
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}