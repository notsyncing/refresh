package io.github.notsyncing.refresh.server.scenes.app

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

@Feature(Features.CreateAppVersion, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class CreateAppVersionScene(private val appName: String,
                            private val version: Version,
                            private val phase: Int,
                            private val packagePath: Path,
                            private val packageExt: String,
                            private val additionalData: JSONObject?) : ManifoldScene<OperationResult>() {
    @AutoProvide
    lateinit var appManager: AppManager

    constructor() : this("", Version.empty, 0, Paths.get(""), "", null)

    override fun stage(): CompletableFuture<OperationResult> {
        appManager.createAppVersion(appName, version, phase, packagePath, packageExt, additionalData)
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}