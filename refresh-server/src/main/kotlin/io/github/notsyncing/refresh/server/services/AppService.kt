package io.github.notsyncing.refresh.server.services

import com.alibaba.fastjson.JSON
import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpPost
import io.github.notsyncing.cowherd.models.UploadFileInfo
import io.github.notsyncing.cowherd.responses.FileResponse
import io.github.notsyncing.cowherd.service.CowherdService
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.PhasedVersion
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.scenes.app.*
import kotlinx.coroutines.experimental.future.future
import java.net.HttpCookie
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

class AppService(private val appManager: AppManager) : CowherdService() {
    @Exported
    @HttpPost
    fun reload(@Parameter("token") token: HttpCookie?): CompletableFuture<OperationResult> {
        return Manifold.run(ReloadAppScene(), token?.value)
    }

    @Exported
    @HttpPost
    fun createAppVersion(@Parameter("name") appName: String,
                         @Parameter("version") version: String,
                         @Parameter("phase") phase: Int,
                         @Parameter("package") file: UploadFileInfo,
                         @Parameter("token") token: HttpCookie?): CompletableFuture<OperationResult> {
        val ver = Version.parse(version) ?: return CompletableFuture.completedFuture(OperationResult.Failed)
        val path = file.file.toPath()
        var ext = file.filename.substringAfterLast('.')

        if (ext == "gz") {
            ext = "tar.gz"
        }

        return Manifold.run(CreateAppVersionScene(appName, ver, phase, path, ext), token?.value)
    }

    @Exported
    @HttpGet
    fun getAppLatestVersion(@Parameter("name") appName: String,
                            @Parameter("token") token: HttpCookie?): CompletableFuture<String> {
        return Manifold.run(GetAppLatestVersionScene(appName), token?.value)
                .thenApply { it.toString() }
    }

    @Exported
    @HttpGet
    fun getAppClientLatestVersion(@Parameter("name") appName: String,
                                  @Parameter("clientData") clientData: String): CompletableFuture<String> {
        return Manifold.run(GetAppClientLatestVersionScene(appName, JSON.parseObject(clientData, Client::class.java)))
                .thenApply { it.toString() }
    }

    @Exported
    @HttpGet
    fun getAppVersions(@Parameter("name") appName: String,
                       @Parameter("top") top: Int?,
                       @Parameter("token") token: HttpCookie?): CompletableFuture<List<String>> {
        return Manifold.run(GetAppVersionsScene(appName, top ?: 0), token?.value)
                .thenApply { it.map { it.toString() } }
    }

    @Exported
    @HttpGet
    fun getAppPhasedVersions(@Parameter("name") appName: String,
                             @Parameter("top") top: Int?,
                             @Parameter("token") token: HttpCookie?): CompletableFuture<List<PhasedVersion>> {
        return Manifold.run(GetAppPhasedVersionsScene(appName, top ?: 0), token?.value)
    }

    @Exported
    @HttpGet
    fun getAppClientVersions(@Parameter("name") appName: String,
                             @Parameter("top") top: Int?,
                             @Parameter("clientData") clientData: String): CompletableFuture<List<String>> {
        return Manifold.run(GetAppClientVersionsScene(appName, JSON.parseObject(clientData, Client::class.java),
                top ?: 0))
                .thenApply { it.map { it.toString() } }
    }

    @Exported
    @HttpPost
    fun deleteAppVersion(@Parameter("name") appName: String,
                         @Parameter("version") version: String,
                         @Parameter("token") token: HttpCookie?): CompletableFuture<OperationResult> {
        val ver = Version.parse(version) ?: return CompletableFuture.completedFuture(OperationResult.Failed)
        return Manifold.run(DeleteAppVersionScene(appName, ver), token?.value)
    }

    @Exported
    @HttpPost
    fun deleteApp(@Parameter("name") appName: String,
                  @Parameter("token") token: HttpCookie?): CompletableFuture<OperationResult> {
        return Manifold.run(DeleteAppScene(appName), token?.value)
    }

    @Exported
    @HttpGet
    fun getAppList(@Parameter("token") token: HttpCookie?): CompletableFuture<List<String>> {
        return Manifold.run(GetAppListScene(), token?.value)
                .thenApply { it.map { it.name } }
    }

    @Exported
    @HttpGet
    fun getAppVersionPackageType(@Parameter("name") appName: String,
                                 @Parameter("version") version: String): String {
        return appManager.getAppPackageType(appName, Version.parse(version)!!)
    }

    @Exported
    @HttpGet
    fun downloadApp(@Parameter("name") appName: String,
                    @Parameter("version") version: String) = future {
        val ver = Version.parse(version) ?: return@future FileResponse(Paths.get("NOT_FOUND"))
        val path = appManager.getAppPackage(appName, ver)

        FileResponse(path)
    }

    @Exported
    @HttpPost
    fun setAppVersionPhase(@Parameter("name") appName: String,
                           @Parameter("version") version: String,
                           @Parameter("phase") phase: Int,
                           @Parameter("token") token: HttpCookie?): CompletableFuture<OperationResult> {
        val ver = Version.parse(version) ?: return CompletableFuture.completedFuture(OperationResult.Failed)
        return Manifold.run(SetAppVersionPhaseScene(appName, ver, phase), token?.value)
    }
}