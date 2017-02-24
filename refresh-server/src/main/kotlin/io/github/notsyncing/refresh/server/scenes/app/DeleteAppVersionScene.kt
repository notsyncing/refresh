package io.github.notsyncing.refresh.server.scenes.app

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.DeleteAppVersion, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class DeleteAppVersionScene(private val appName: String,
                            private val version: Version) : ManifoldScene<OperationResult>() {
    @AutoProvide
    lateinit var appManager: AppManager

    constructor() : this("", Version.empty)

    override fun stage(): CompletableFuture<OperationResult> {
        appManager.deleteAppVersion(appName, version)
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}