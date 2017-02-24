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

@Feature(Features.SetAppVersionPhase, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class SetAppVersionPhaseScene(private val appName: String,
                              private val version: Version,
                              private val phase: Int) : ManifoldScene<OperationResult>() {
    @AutoProvide
    lateinit var appManager: AppManager

    constructor() : this("", Version.empty, 0)

    override fun stage(): CompletableFuture<OperationResult> {
        appManager.setAppVersionPhase(appName, version, phase)
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}