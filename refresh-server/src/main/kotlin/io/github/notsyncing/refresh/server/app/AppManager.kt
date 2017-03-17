package io.github.notsyncing.refresh.server.app

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.di.EarlyProvide
import io.github.notsyncing.manifold.di.ProvideAsSingleton
import io.github.notsyncing.refresh.common.*
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.common.utils.deleteRecursive
import io.github.notsyncing.refresh.common.utils.hash
import io.vertx.core.impl.ConcurrentHashSet
import java.io.InvalidObjectException
import java.nio.file.*
import kotlin.concurrent.thread

@ProvideAsSingleton
@EarlyProvide
class AppManager {
    companion object {
        var appFileStoragePath: Path = Paths.get(".")
        val generatingDeltas = ConcurrentHashSet<String>()
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

    fun createAppVersion(appName: String, version: Version, phase: Int, packageFile: Path, packageExt: String,
                         additionalData: JSONObject?) {
        val path = appRootPath.resolve(appName).resolve(version.toString())

        if (!Files.exists(path)) {
            Files.createDirectories(path)
        } else {
            deleteAppDeltasByVersion(appName, version)
        }

        val fn = packageFile.fileName.toString()
        val targetFile = path.resolve("package.$packageExt")

        Files.copy(packageFile, targetFile, StandardCopyOption.REPLACE_EXISTING)

        val checksumInfo = additionalData?.getJSONObject("checksum")

        if (checksumInfo != null) {
            val checksumType = checksumInfo.getString("type")
            val checksumExpected = checksumInfo.getString("data")
            val checksumActual = hash(targetFile, checksumType)

            if (checksumExpected != checksumActual) {
                Files.deleteIfExists(targetFile)
                throw InvalidObjectException("Uploaded app $appName $version has wrong checksum $checksumActual " +
                        "while expecting $checksumExpected ($checksumType)")
            }
        }

        val typeFile = path.resolve(".type")
        Files.write(typeFile, packageExt.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        val phaseFile = path.resolve(".phase")
        Files.write(phaseFile, phase.toString().toByteArray(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)

        if (additionalData != null) {
            val infoFile = path.resolve(".info")
            Files.write(infoFile, additionalData.toJSONString().toByteArray(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)
        }

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

    private fun deleteAppDeltasByVersion(name: String, version: Version) {
        val deltas = appRootPath.resolve(name).resolve("deltas")

        Files.list(deltas)
                .filter { it.fileName.toString().contains("-$version.") || it.fileName.toString().contains("-$version-") }
                .forEach { Files.delete(it) }
    }

    fun deleteAppVersion(appName: String, version: Version) {
        val path = appRootPath.resolve(appName).resolve(version.toString())

        if (!Files.exists(path)) {
            return
        }

        deleteRecursive(path)
        deleteAppDeltasByVersion(appName, version)

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
        val verRoot = appRootPath.resolve(appName).resolve(version.toString())
        val typeFile = verRoot.resolve(".type")
        val ext = String(Files.readAllBytes(typeFile))

        return verRoot.resolve("package.$ext")
    }

    fun getAppPackageType(appName: String, version: Version): String {
        val verRoot = appRootPath.resolve(appName).resolve(version.toString())
        val typeFile = verRoot.resolve(".type")
        val ext = String(Files.readAllBytes(typeFile))

        return ext
    }

    fun setAppVersionPhase(appName: String, version: Version, phase: Int) {
        val p = appRootPath.resolve(appName).resolve(version.toString()).resolve(".phase")
        Files.write(p, phase.toString().toByteArray())

        getApp(appName)?.versionPhases?.set(version, phase)
    }

    fun generatePackageDelta(appName: String, fromVersion: Version, toVersion: Version): Path? {
        val deltaDir = appRootPath.resolve(appName).resolve("deltas")

        if (!Files.exists(deltaDir)) {
            Files.createDirectories(deltaDir)
        }

        val deltaPackage = deltaDir.resolve("$appName-$fromVersion-to-$toVersion.delta")

        if (Files.exists(deltaPackage)) {
            return deltaPackage
        }

        val s = "$appName-$fromVersion-$toVersion"

        if (generatingDeltas.contains(s)) {
            return null
        }

        val fromPackage = getAppPackage(appName, fromVersion)
        val toPackage = getAppPackage(appName, toVersion)

        generatingDeltas.add(s)

        thread {
            try {
                val tmpDeltaPackage = deltaDir.resolve("$appName-$fromVersion-to-$toVersion.delta.tmp")

                XDelta.make(fromPackage, toPackage, tmpDeltaPackage)
                Files.move(tmpDeltaPackage, deltaPackage, StandardCopyOption.REPLACE_EXISTING)

                val checksumType = "MD5"
                val checksum = hash(deltaPackage, checksumType)

                println("Checksum of delta $deltaPackage: $checksumType $checksum")

                val info = JSONObject()
                        .fluentPut("checksum", JSONObject()
                                .fluentPut("type", checksumType)
                                .fluentPut("data", checksum))

                val infoFile = deltaDir.resolve("$appName-$fromVersion-to-$toVersion.info")
                Files.write(infoFile, info.toJSONString().toByteArray(), StandardOpenOption.CREATE)

                println("Delta info written.")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                generatingDeltas.remove(s)
            }
        }

        return null
    }

    fun canAppHasDelta(appName: String, fromVersion: Version, toVersion: Version): OperationResult {
        val deltaDir = appRootPath.resolve(appName).resolve("deltas")
        val deltaPackage = deltaDir.resolve("$appName-$fromVersion-to-$toVersion.delta")

        if (Files.exists(deltaPackage)) {
            return OperationResult.Success
        }

        if ((!appHasVersion(appName, fromVersion)) || (!appHasVersion(appName, toVersion))) {
            return OperationResult.Failed
        } else {
            return OperationResult.Success
        }
    }

    fun getAppPackageInfo(appName: String, version: Version): String {
        val infoFile = appRootPath.resolve(appName).resolve(version.toString()).resolve(".info")

        if (!Files.exists(infoFile)) {
            return "{}"
        } else {
            return String(Files.readAllBytes(infoFile))
        }
    }

    fun getAppDeltaPackageInfo(appName: String, fromVersion: Version, toVersion: Version): String {
        val infoFile = appRootPath.resolve(appName).resolve("deltas").resolve("$appName-$fromVersion-to-$toVersion.info")

        if (!Files.exists(infoFile)) {
            return "{}"
        } else {
            return String(Files.readAllBytes(infoFile))
        }
    }
}