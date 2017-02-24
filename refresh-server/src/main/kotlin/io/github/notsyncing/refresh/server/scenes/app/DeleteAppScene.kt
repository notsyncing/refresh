package io.github.notsyncing.refresh.server.scenes.app

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.DeleteApp, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class DeleteAppScene(private val appName: String) : ManifoldScene<OperationResult>() {
    @AutoProvide
    lateinit var appManager: AppManager

    constructor() : this("")

    override fun stage(): CompletableFuture<OperationResult> {
        appManager.deleteApp(appName)
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}