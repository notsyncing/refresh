package io.github.notsyncing.refresh.server.scenes.app

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.GetAppVersion, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class GetAppVersionsScene(private val appName: String,
                          private val top: Int = 0) : ManifoldScene<List<Version>>() {
    @AutoProvide
    lateinit var appManager: AppManager

    constructor() : this("")

    override fun stage(): CompletableFuture<List<Version>> {
        return CompletableFuture.completedFuture(appManager.getAppVersions(appName, top))
    }
}