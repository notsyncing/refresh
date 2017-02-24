package io.github.notsyncing.refresh.server.scenes.app

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.App
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.util.concurrent.CompletableFuture

@Feature(Features.GetAppList, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class GetAppListScene : ManifoldScene<List<App>>() {
    @AutoProvide
    lateinit var appManager: AppManager

    override fun stage(): CompletableFuture<List<App>> {
        return CompletableFuture.completedFuture(appManager.getAppList())
    }
}