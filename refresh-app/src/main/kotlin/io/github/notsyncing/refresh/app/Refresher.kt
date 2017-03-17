package io.github.notsyncing.refresh.app

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.mashape.unirest.http.Unirest
import io.github.notsyncing.refresh.app.unique.UniqueProvider
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.XDelta
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.common.utils.hash
import io.github.notsyncing.refresh.common.utils.isUrlReachable
import org.rauschig.jarchivelib.ArchiverFactory
import java.io.InvalidObjectException
import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission
import java.util.stream.Collectors

class Refresher(private val config: () -> RefreshConfig,
                private val uniqueProvider: UniqueProvider,
                private val inAppVersionDir: Boolean = false) {
    val machineId = uniqueProvider.provide()

    private var clientData = Client("", machineId, "", Version.empty, "")

    var onAppDownloaded: (() -> Unit)? = null

    val appDir: Path
        get() = if (inAppVersionDir)
            Paths.get("..")
        else
            Paths.get(config().name)

    val appCurrentVersionDir: Path?
        get() {
            val ver = getCurrentLocalVersion() ?: return null
            return appDir.resolve(ver.toString())
        }

    fun getCurrentLocalVersion(): Version? {
        val f = if (inAppVersionDir)
            Paths.get("../..", config().name, ".current")
        else
            Paths.get(config().name, ".current")

        if (!Files.exists(f)) {
            return null
        }

        val s = String(Files.readAllBytes(f))
        return Version.parse(s)
    }

    fun getLatestLocalVersion(): Version? {
        val f = Paths.get(config().name)

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

    fun updateServerUrl(url: String): String {
        var s = config().updateServer

        if (!s.endsWith("/")) {
            s += "/"
        }

        return s + url
    }

    fun getCurrentRemoteVersion(): Version? {
        val cd = JSON.toJSONString(clientData)

        println("getCurrentRemoteVersion: clientData = $cd, clientData.currentVersion = ${clientData.currentVersion}")

        val ver = Unirest.get(updateServerUrl("AppService/getAppClientLatestVersion"))
                .queryString(mapOf("name" to config().name,
                        "clientData" to cd))
                .asString()
                .body

        return Version.parse(ver)
    }

    private fun downloadApp(name: String, version: Version): OperationResult {
        if (!Files.exists(appDir)) {
            Files.createDirectories(appDir)
        }

        val flagFile = appDir.resolve(".downloading")
        Files.write(flagFile, version.toString().toByteArray())

        var packageFile: Path?

        try {
            val type = Unirest.get(updateServerUrl("AppService/getAppVersionPackageType"))
                    .queryString(mapOf("name" to name,
                            "version" to version.toString()))
                    .asString()
                    .body

            val (hasDelta, pkgFile) = downloadAppDelta(name, type, version)
            packageFile = pkgFile

            if (packageFile == null) {
                if (hasDelta) {
                    println("App has delta, but not ready.")
                    return OperationResult.Failed
                }

                println("App has no delta, download full package.")
                packageFile = downloadAppFullPackage(name, type, version)
            } else {
                println("App has delta.")
            }

            if (packageFile == null) {
                return OperationResult.Failed
            }

            val pkg = packageFile.toFile()
            val path = Paths.get(".")

            val archiver = ArchiverFactory.createArchiver(pkg)
            archiver.extract(pkg, path.toFile())

            val startFile = path.resolve("start.sh")

            if (Files.exists(startFile)) {
                Files.setPosixFilePermissions(startFile, setOf(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OTHERS_READ))
            }

            Files.deleteIfExists(flagFile)

            onAppDownloaded?.invoke()
        } finally {
            Files.deleteIfExists(flagFile)
        }

        return OperationResult.Success
    }

    private fun downloadAppDelta(name: String, type: String?, version: Version): Pair<Boolean, Path?> {
        val currLocalVer = getCurrentLocalVersion()
        var hasDelta = false

        if (currLocalVer != null) {
            val hasDeltaResult = Unirest.get(updateServerUrl("AppService/canAppHasDelta"))
                    .queryString(mapOf("name" to name,
                            "curr_ver" to currLocalVer.toString(),
                            "new_ver" to version.toString()))
                    .asString()
                    .body

            if (hasDeltaResult == OperationResult.Success.ordinal.toString()) {
                hasDelta = true

                val currLocalVerPackageInfoData = Unirest.get(updateServerUrl("AppService/getAppPackageInfo"))
                        .queryString(mapOf("name" to name, "version" to currLocalVer.toString()))
                        .asString()
                        .body

                val currLocalVerPackageInfo = JSON.parseObject(currLocalVerPackageInfoData)
                val downloadDir = appDir.resolve("downloads")
                val localPackage = downloadDir.resolve("$name-$currLocalVer.$type")
                val currLocalVerPackageChecksumInfo = currLocalVerPackageInfo.getJSONObject("checksum")

                if (currLocalVerPackageChecksumInfo != null) {
                    println("Checking local version package integrity...")

                    if (!checkFileIntegrity(localPackage, currLocalVerPackageChecksumInfo)) {
                        throw InvalidObjectException("Local version $currLocalVer has wrong checksum compared to remote!")
                    }

                    println("Integrity ok.")
                }

                val deltaPackageInfoData = Unirest.get(updateServerUrl("AppService/getAppDeltaPackageInfo"))
                        .queryString(mapOf("name" to name,
                                "curr_ver" to currLocalVer.toString(),
                                "new_ver" to version.toString()))
                        .asString()
                        .body

                val deltaPackageInfo = JSON.parseObject(deltaPackageInfoData)
                val deltaPackageChecksumInfo = deltaPackageInfo.getJSONObject("checksum")

                val resp = Unirest.get(updateServerUrl("AppService/downloadAppDelta"))
                        .queryString(mapOf("name" to name,
                                "curr_ver" to currLocalVer.toString(),
                                "new_ver" to version.toString()))
                        .asBinary()

                if (resp.status != 200) {
                    println("downloadAppDelta: server returned ${resp.status}")
                    return Pair(hasDelta, null)
                }

                val data = resp.body

                val tmpPath = Files.createTempFile("refresh-$name-$version-", ".delta")

                data.use {
                    Files.copy(it, tmpPath, StandardCopyOption.REPLACE_EXISTING)
                }

                if (deltaPackageChecksumInfo != null) {
                    println("Checking delta package integrity...")

                    if (!checkFileIntegrity(tmpPath, deltaPackageChecksumInfo)) {
                        throw InvalidObjectException("Download delta package $tmpPath has wrong checksum!")
                    }

                    println("Integrity ok.")
                }

                val newPackage = downloadDir.resolve("$name-$version.$type")
                Files.deleteIfExists(newPackage)

                try {
                    XDelta.patch(localPackage, tmpPath, newPackage)
                    return Pair(hasDelta, newPackage)
                } catch (e: Exception) {
                    return Pair(hasDelta, null)
                }
            }
        }

        return Pair(hasDelta, null)
    }

    private fun downloadAppFullPackage(name: String, type: String?, version: Version): Path? {
        val infoData = Unirest.get(updateServerUrl("AppService/getAppPackageInfo"))
                .queryString(mapOf("name" to name, "version" to version.toString()))
                .asString()
                .body

        val info = JSON.parseObject(infoData)

        val tmpPath = Files.createTempFile("refresh-$name-$version-", ".$type")

        val data = Unirest.get(updateServerUrl("AppService/downloadApp"))
                .queryString(mapOf("name" to name,
                        "version" to version.toString()))
                .asBinary()
                .body

        data.use {
            Files.copy(it, tmpPath, StandardCopyOption.REPLACE_EXISTING)
        }

        val downloadDir = appDir.resolve("downloads")

        if (!Files.exists(downloadDir)) {
            Files.createDirectories(downloadDir)
        }

        val newPackage = downloadDir.resolve("$name-$version.$type")
        Files.copy(tmpPath, newPackage, StandardCopyOption.REPLACE_EXISTING)

        Files.deleteIfExists(tmpPath)

        val checksumInfo = info.getJSONObject("checksum")

        if (checksumInfo != null) {
            println("Check package integrity...")

            if (!checkFileIntegrity(newPackage, checksumInfo)) {
                Files.delete(newPackage)
                throw InvalidObjectException("Downloaded app package has wrong checksum!")
            }

            println("Integrity ok.")
        }

        return newPackage
    }

    private fun checkFileIntegrity(file: Path, checksumInfo: JSONObject): Boolean {
        val checksumType = checksumInfo.getString("type")
        val checksumExpected = checksumInfo.getString("data")
        val checksumActual = hash(file, checksumType)

        return checksumExpected == checksumActual
    }

    private fun hasLocalVersion(name: String, version: Version): Boolean {
        return Files.exists(Paths.get(name, version.toString()))
    }

    fun checkForUpdate(): UpdateCheckResult {
        val localVer = getCurrentLocalVersion() ?: getLatestLocalVersion() ?: Version.empty
        clientData = clientData.modifyVersion(localVer)

        println("checkForUpdate: localVer = $localVer, clientData.currentVersion = ${clientData.currentVersion}")

        val remoteVer = getCurrentRemoteVersion() ?: Version.empty

        println("Check for update: local $localVer, remote $remoteVer")

        return UpdateCheckResult(remoteVer, localVer)
    }

    private fun updateCurrentLocalVersion(version: Version) {
        val f = Paths.get(config().name, ".current")
        Files.write(f, version.toString().toByteArray(), StandardOpenOption.CREATE)

        clientData = clientData.modifyVersion(version)
    }

    fun checkAndDownload(): OperationResult {
        val r = checkForUpdate()

        if (!r.hasUpdate()) {
            println("No update found.")
            return OperationResult.Success
        }

        var dr = OperationResult.Success

        if (r.isUpgrade()) {
            dr = downloadApp(config().name, r.remoteVersion)
        } else {
            if (!hasLocalVersion(config().name, r.remoteVersion)) {
                dr = downloadApp(config().name, r.remoteVersion)
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

        println("getRemoteVersions: server returned $r")

        return JSON.parseArray(r)
                .map { Version.parse(it.toString())!! }
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
        if (!isUrlReachable(config().updateServer)) {
            println("The update server ${config().updateServer} is not reachable. Use local versions.")

            return rollbackToLocalVersion(version)
        }

        if (version == null) {
            val versions = getRemoteVersions(config().name)

            if (versions.isEmpty()) {
                println("This app has no versions. Cannot rollback.")
                return OperationResult.Failed
            }

            val lv = getCurrentLocalVersion()

            if (lv == null) {
                println("No current version or current version is not installed correctly. Try to download latest...")
                return rollbackToVersion(versions[0])
            }

            val i = versions.indexOf(lv) + 1

            if (i > versions.size - 1) {
                println("Current version is earliest version, cannot rollback!")
                return OperationResult.Failed
            }

            return rollbackToVersion(versions[i])
        } else {
            val r = downloadApp(config().name, version)

            if (r != OperationResult.Success) {
                println("Failed to download app ${config().name} version $version")
                return r
            }

            updateCurrentLocalVersion(version)

            return OperationResult.Success
        }
    }

    private fun rollbackToLocalVersion(version: Version?): OperationResult {
        if (version == null) {
            val versions = getLocalVersions(config().name)

            if (versions.isEmpty()) {
                println("This app has no local versions. Cannot rollback.")
                return OperationResult.Failed
            }

            val lv = getCurrentLocalVersion()!!
            val i = versions.indexOf(lv) + 1

            if (i > versions.size - 1) {
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

    fun getCurrentDownloadingVersion(): Version? {
        val p = appDir.resolve(".downloading")

        if (!Files.exists(p)) {
            return null
        }

        val ver = String(Files.readAllBytes(p))
        return Version.parse(ver)
    }
}