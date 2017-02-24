package io.github.notsyncing.refresh.server.app

import io.github.notsyncing.manifold.di.EarlyProvide
import io.github.notsyncing.manifold.di.ProvideAsSingleton
import io.github.notsyncing.refresh.common.App
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.PhasedVersion
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.utils.deleteRecursive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@ProvideAsSingleton
@EarlyProvide
class AppManager {
    companion object {
        var appFileStoragePath: Path = Paths.get(".")
    }

    private val apps = mutableListOf<App>()
    private val appRootPath = appFileStoragePath.resolve("apps")

    init {
        if (!Files.exists(appRootPath)) {
            Files.createDirectories(appRootPath)
        }

        reload()
    }

    fun reload() {
        val tempApps = mutableListOf<App>()

        for (p in Files.list(appRootPath)) {
            val name = p.last().toString()

            if ((Files.isHidden(p)) || (!Files.isDirectory(p))) {
                continue
            }

            val app = App(name)

            tempApps.add(app)

            for (v in Files.list(p)) {
                val verName = v.last().toString()
                val ver = Version.parse(verName)

                if (ver == null) {
                    println("Invalid version $verName found in app $name")
                    continue
                }

                app.versions.add(ver)

                val phaseFile = v.resolve(".phase")

                if (Files.exists(phaseFile)) {
                    val phase = String(Files.readAllBytes(phaseFile)).toInt()
                    app.versionPhases[ver] = phase
                } else {
                    app.versionPhases[ver] = 0
                }
            }
        }

        synchronized(apps) {
            apps.clear()
            apps.addAll(tempApps)
        }
    }

    private fun addAppVersion(appName: String, version: Version, phase: Int) {
        var app = apps.firstOrNull { it.name == appName }

        if (app == null) {
            val newApp = App(appName, mutableListOf(version), mutableMapOf(version to phase))
            apps.add(newApp)

            app = newApp
        } else {
            app.versions.add(version)
            app.versionPhases[version] = phase
        }

        app.versions.sortDescending()
    }

    fun createAppVersion(appName: String, version: Version, phase: Int, packageFile: Path) {
        val path = appRootPath.resolve(appName).resolve(version.toString())

        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }

        Files.copy(packageFile, path.resolve("package"), StandardCopyOption.REPLACE_EXISTING)

        val phaseFile = path.resolve(".phase")
        Files.write(phaseFile, phase.toString().toByteArray())

        addAppVersion(appName, version, phase)
    }

    fun getAppLatestVersion(appName: String): Version? {
        return getApp(appName)?.versions?.firstOrNull()
    }

    fun getAppLatestVersion(appName: String, clientData: Client, phase: Int): Version? {
        val app = getApp(appName) ?: return null

        return app.versionPhases.entries.sortedByDescending { it.key }
                .firstOrNull { it.value <= phase }
                ?.key
    }

    fun getAppVersions(appName: String, top: Int = 0): List<Version> {
        val list = getApp(appName)?.versions

        if (top > 0) {
            return list?.take(top) ?: emptyList()
        } else {
            return list ?: emptyList()
        }
    }

    fun getAppVersionPhases(appName: String): List<PhasedVersion> {
        return getApp(appName)?.versionPhases
                ?.map { (k, v) -> PhasedVersion(k.major, k.minor, k.patch, k.build, v) }
                ?.sortedDescending()
                ?.toList() ?: emptyList()
    }

    fun getAppVersions(appName: String, clientData: Client, phase: Int, top: Int = 0): List<Version> {
        val app = getApp(appName) ?: return emptyList()

        val l = app.versionPhases.entries
                .filter { it.value <= phase }
                .map { it.key }
                .sortedByDescending { it }

        return if (top > 0) l.take(top) else l
    }

    fun deleteAppVersion(appName: String, version: Version) {
        val path = appRootPath.resolve(appName).resolve(version.toString())

        if (!Files.exists(path)) {
            return
        }

        deleteRecursive(path)

        getApp(appName)?.apply {
            this.versions.remove(version)
            this.versionPhases.remove(version)
        }
    }

    fun deleteApp(appName: String) {
        val path = appRootPath.resolve(appName)

        if (!Files.exists(path)) {
            return
        }

        deleteRecursive(path)

        apps.removeIf { it.name == appName }
    }

    fun appHasVersion(appName: String, version: Version): Boolean {
        return apps.any { (it.name == appName) && (it.versions.any { it == version }) }
    }

    fun hasApp(appName: String): Boolean {
        return apps.any { it.name == appName }
    }

    fun getAppList(): List<App> {
        return apps
    }

    fun getApp(appName: String): App? {
        return apps.firstOrNull { it.name == appName }
    }

    fun getAppPackage(appName: String, version: Version): Path {
        return appRootPath.resolve(appName).resolve(version.toString()).resolve("package")
    }

    fun setAppVersionPhase(appName: String, version: Version, phase: Int) {
        val p = appRootPath.resolve(appName).resolve(version.toString()).resolve(".phase")
        Files.write(p, phase.toString().toByteArray())

        getApp(appName)?.versionPhases?.set(version, phase)
    }
}