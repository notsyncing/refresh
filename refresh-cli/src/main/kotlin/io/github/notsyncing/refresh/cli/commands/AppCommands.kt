package io.github.notsyncing.refresh.cli.commands

import com.alibaba.fastjson.JSON
import io.github.notsyncing.refresh.common.PhasedVersion
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.common.utils.deleteRecursive
import io.github.notsyncing.refresh.common.utils.pack
import java.nio.file.Files
import java.nio.file.Paths

class AppCommands : CommandBase() {
    @Command
    fun reload() {
        val r = post("AppService/reload")

        if (r != OperationResult.Success.ordinal.toString()) {
            println("Failed to reload app data: Server returned $r")
        } else {
            println("App data reloaded.")
        }
    }

    @Command
    fun createAppVersion(name: String, version: String, phase: String, filePath: String) {
        var path = Paths.get(filePath)
        val temp = Files.createTempDirectory("refresh-")

        if (Files.isDirectory(path)) {
            println("$filePath is a directory.")

            val compressed = Paths.get(temp.toString(), "$name-$version.zip")
            pack(path, compressed)

            path = compressed
        } else {
            val fn = path.fileName.toString()

            if ((!fn.endsWith(".gz")) && (!fn.endsWith(".zip"))) {
                println("$filePath is not a compressed file nor a directory!")
                return
            }
        }

        val r = post("AppService/createAppVersion", listOf("name" to name, "version" to version, "phase" to phase),
                listOf("package" to path))

        if (r != OperationResult.Success.ordinal.toString()) {
            println("Failed to reload app data: Server returned $r")
        } else {
            println("App $name version $version created.")
        }

        deleteRecursive(temp)
    }

    @Command
    fun appLatestVersion(name: String) {
        val r = get("AppService/getAppLatestVersion", listOf("name" to name))

        println(r)
    }

    @Command
    fun appVersions(name: String, top: String) {
        val r = get("AppService/getAppVersions", listOf("name" to name, "top" to top))
        val data = JSON.parseArray(r, Version::class.java)

        if (data == null) {
            println("Invalid data returned: $r")
            return
        }

        data.forEach(::println)
    }

    @Command
    fun appVersionPhases(name: String, top: String) {
        val r = get("AppService/getAppPhasedVersions", listOf("name" to name, "top" to top))
        val data = JSON.parseArray(r, PhasedVersion::class.java)

        if (data == null) {
            println("Invalid data returned: $r")
            return
        }

        data.forEach(::println)
    }

    @Command
    fun deleteAppVersion(name: String, version: String) {
        val r = post("AppService/deleteAppVersion", listOf("name" to name, "version" to version))

        if (r != OperationResult.Success.ordinal.toString()) {
            println("Failed to delete app $name version $version: Server returned $r")
        } else {
            println("App $name version $version deleted.")
        }
    }

    @Command
    fun deleteApp(name: String) {
        val r = post("AppService/deleteApp", listOf("name" to name))

        if (r != OperationResult.Success.ordinal.toString()) {
            println("Failed to delete app $name: Server returned $r")
        } else {
            println("App $name deleted.")
        }
    }

    @Command
    fun getAppList() {
        val r = get("AppService/getAppList")
        val data = JSON.parseArray(r)

        data.forEach {
            println(it)
        }
    }

    @Command
    fun setAppVersionPhase(name: String, version: String, phase: String) {
        val r = post("AppService/setAppVersionPhase", listOf("name" to name, "version" to version, "phase" to phase))

        if (r != OperationResult.Success.ordinal.toString()) {
            println("Failed to set app version phase: Server returned $r")
        } else {
            println("App $name version $version set to phase $phase.")
        }
    }
}
