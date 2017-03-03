package io.github.notsyncing.refresh.server.scenes.app

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.PhasedVersion
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.GetAppPhasedVersion, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class GetAppPhasedVersionsScene(private val appName: String,
                          private val top: Int = 0) : ManifoldScene<List<PhasedVersion>>() {
    @AutoProvide
    lateinit var appManager: AppManager

    constructor() : this("")

    override fun stage(): CompletableFuture<List<PhasedVersion>> {
        return CompletableFuture.completedFuture(appManager.getAppVersionPhases(appName))
    }
}