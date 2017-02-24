package io.github.notsyncing.refresh.app

import com.alibaba.fastjson.JSON
import com.mashape.unirest.http.Unirest
import io.github.notsyncing.refresh.app.unique.UniqueProvider
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.common.utils.copyRecursive
import io.github.notsyncing.refresh.common.utils.isUrlReachable
import net.java.truevfs.access.TPath
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors

class Refresher(private val config: RefreshConfig,
                private val uniqueProvider: UniqueProvider) {
    private val machineId = uniqueProvider.provide()
    private var clientData = Client("", machineId, "", Version.empty, "")

    val appDir: Path
        get() = Paths.get(config.name)

    val appCurrentVersionDir: Path?
        get() {
            val ver = getCurrentLocalVersion() ?: return null
            return appDir.resolve(ver.toString())
        }

    fun getCurrentLocalVersion(): Version? {
        val f = Paths.get(config.name, ".current")

        if (!Files.exists(f)) {
            return null
        }

        val s = String(Files.readAllBytes(f))
        return Version.parse(s)
    }

    fun getLatestLocalVersion(): Version? {
        val f = Paths.get(config.name)

        if (!Files.isDirectory(f)) {
            return null
        }

        return Files.list(f)
                .filter { (Files.isDirectory(f)) && (!Files.isHidden(f))  }
                .map { Version.parse(it.fileName.toString()) }
                .filter { it != null }
                .max { o1, o2 -> o1!!.compareTo(o2!!) }
                .orElse(null)
    }

    private fun updateServerUrl(url: String): String {
        var s = config.updateServer

        if (!s.endsWith("/")) {
            s += "/"
        }

        return s + url
    }

    fun getCurrentRemoteVersion(): Version? {
        val ver = Unirest.get(updateServerUrl("AppService/getAppClientLatestVersion"))
                .queryString(mapOf("name" to config.name,
                        "clientData" to JSON.toJSONString(clientData)))
                .asString()
                .body

        return Version.parse(ver)
    }

    private fun downloadApp(name: String, version: Version): OperationResult {
        val tmpPath = Files.createTempFile("refresh-$name-$version-", ".tmp")

        val data = Unirest.get(updateServerUrl("AppService/downloadApp"))
                .queryString(mapOf("name" to name,
                        "version" to version.toString()))
                .asBinary()
                .body

        data.use {
            Files.copy(it, tmpPath)
        }

        val inner = TPath(tmpPath)
        val path = Paths.get(name, version.toString())

        Files.createDirectories(path)
        copyRecursive(inner, path)

        return OperationResult.Success
    }

    private fun hasLocalVersion(name: String, version: Version): Boolean {
        return Files.exists(Paths.get(name, version.toString()))
    }

    fun checkForUpdate(): UpdateCheckResult {
        val localVer = getCurrentLocalVersion() ?: Version.empty
        val remoteVer = getCurrentRemoteVersion() ?: Version.empty

        println("Check for update: local $localVer, remote $remoteVer")

        return UpdateCheckResult(remoteVer, localVer)
    }

    private fun updateCurrentLocalVersion(version: Version) {
        val f = Paths.get(config.name, ".current")
        Files.write(f, version.toString().toByteArray(), StandardOpenOption.CREATE)
    }

    fun checkAndDownload(): OperationResult {
        val r = checkForUpdate()

        if (!r.hasUpdate()) {
            println("No update found.")
            return OperationResult.Success
        }

        var dr = OperationResult.Success

        if (r.isUpgrade()) {
            dr = downloadApp(config.name, r.remoteVersion)
        } else {
            if (!hasLocalVersion(config.name, r.remoteVersion)) {
                dr = downloadApp(config.name, r.remoteVersion)
            } else {
                println("Downgrade to local version ${r.remoteVersion}")
            }
        }

        if (dr == OperationResult.Success) {
            updateCurrentLocalVersion(r.remoteVersion)
            println("Current local version updated to ${r.remoteVersion}")
        }

        return dr
    }

    private fun getRemoteVersions(name: String): List<Version> {
        val r = Unirest.get(updateServerUrl("AppService/getAppClientVersions"))
                .queryString(mapOf("name" to name,
                        "clientData" to JSON.toJSONString(clientData)))
                .asString()
                .body

        return JSON.parseArray(r, Version::class.java)
    }

    private fun getLocalVersions(name: String): List<Version> {
        val p = Paths.get(name)

        return Files.list(p)
                .filter { Files.isDirectory(it) }
                .map { it.fileName.toString() }
                .map { Version.parse(it) }
                .filter { it != null }
                .map { it as Version }
                .collect(Collectors.toList<Version>())
    }

    fun rollbackToVersion(version: Version?): OperationResult {
        if (!isUrlReachable(config.updateServer)) {
            println("The update server ${config.updateServer} is not reachable. Use local versions.")

            return rollbackToLocalVersion(version)
        }

        if (version == null) {
            val versions = getRemoteVersions(config.name)

            if (versions.isEmpty()) {
                println("This app has no versions. Cannot rollback.")
                return OperationResult.Failed
            }

            val lv = getCurrentLocalVersion()!!
            val i = versions.indexOf(lv) - 1

            if (i < 0) {
                println("Current version is earliest version, cannot rollback!")
                return OperationResult.Failed
            }

            return rollbackToVersion(versions[i])
        } else {
            val r = downloadApp(config.name, version)

            if (r != OperationResult.Success) {
                println("Failed to download app ${config.name} version $version")
                return r
            }

            updateCurrentLocalVersion(version)

            return OperationResult.Success
        }
    }

    private fun rollbackToLocalVersion(version: Version?): OperationResult {
        if (version == null) {
            val versions = getLocalVersions(config.name)

            if (versions.isEmpty()) {
                println("This app has no local versions. Cannot rollback.")
                return OperationResult.Failed
            }

            val lv = getCurrentLocalVersion()!!
            val i = versions.indexOf(lv) - 1

            if (i < 0) {
                println("Current version is earliest version, cannot rollback!")
                return OperationResult.Failed
            }

            return rollbackToLocalVersion(versions[i])
        } else {
            updateCurrentLocalVersion(version)

            return OperationResult.Success
        }
    }

    fun rollbackToPreviousVersion() = rollbackToVersion(null)

    fun updateClientAccount(id: String, name: String) {
        clientData = clientData.modifyAccount(id, name)
    }
}