package io.github.notsyncing.refresh.app

import com.alibaba.fastjson.JSON
import io.github.notsyncing.refresh.app.client.RefreshClient
import io.github.notsyncing.refresh.app.unique.UUIDProvider
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.common.enums.OperationResult
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.concurrent.thread

class RefreshAppLauncher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = RefreshAppLauncher()
            app.start()
        }
    }

    private val config: RefreshConfig
    private val refresher: Refresher
    private var stop = false

    init {
        val f = Paths.get("refresh.json")
        val s = String(Files.readAllBytes(f))
        val allConfig = JSON.parseObject(s)
        config = allConfig.getObject("app", RefreshConfig::class.java)
        refresher = Refresher(config, UUIDProvider())
        RefreshClient.instance = RefreshClient(refresher)

        Files.deleteIfExists(Paths.get(".account"))
    }

    private fun launchApp(localVer: Version) {
        val p = Paths.get(config.name, localVer.toString()).toAbsolutePath()
        val app = ProcessBuilder()
                .also {
                    for ((k, v) in it.environment()) {
                        println("$k=$v")
                    }
                }
                .inheritIO()
                .directory(p.toFile())
                .command(config.cmdLine.split(" "))
                .also {
                    println("------------")
                }
                .start()

        val appResult = app.waitFor()

        println("------------")

        if (appResult != 0) {
            println("App exited abnormally with code $appResult. Checking for alternative versions...")

            val ur = refresher.checkForUpdate()

            if (ur.hasUpdate()) {
                println("Update found. local version ${ur.localVersion}, remote version ${ur.remoteVersion}. Will download it and retry.")

                val r = refresher.checkAndDownload()

                if (r != OperationResult.Success) {
                    println("Failed to download update!")
                    return
                }

                println("Starting version ${ur.remoteVersion}")

                launchApp(ur.remoteVersion)
            } else {
                println("No update found. Rolling back to previous version...")

                val r = refresher.rollbackToPreviousVersion()

                if (r != OperationResult.Success) {
                    println("Failed to rollback!")
                    return
                }

                val lv = refresher.getCurrentLocalVersion()!!

                println("Rollback succeeded. Starting version $lv")

                launchApp(lv)
            }
        } else {
            println("App exited normally.")
        }
    }

    fun start() {
        var localVer = refresher.getCurrentLocalVersion()

        if (localVer == null) {
            localVer = refresher.getLatestLocalVersion()
        }

        if (localVer == null) {
            val r = refresher.checkAndDownload()

            if (r != OperationResult.Success) {
                println("No local version found, and download failed!")
                return
            } else {
                localVer = refresher.getCurrentLocalVersion()
            }
        }

        val accountFile = Paths.get(".account")

        val checkUpdateThread = thread(priority = Thread.MIN_PRIORITY) {
            while (!stop) {
                try {
                    if (!Files.exists(accountFile)) {
                        //Thread.sleep(10 * 60 * 1000)

                        if (!stop) {
                            Thread.sleep(10000)
                        }

                        continue
                    } else {
                        val accountData = Files.readAllLines(accountFile)

                        refresher.updateClientAccount(accountData[0], accountData[1])
                    }

                    val r = refresher.checkAndDownload()

                    println("Check and download result: $r")
                } catch (e: Exception) {
                    println("Exception occured when checking for update: ${e.message}")
                    e.printStackTrace()
                }

                if (!stop) {
                    //Thread.sleep(60 * 60 * 1000)
                    Thread.sleep(10000)
                }
            }
        }

        if (localVer != null) {
            launchApp(localVer)
        } else {
            println("Local version not found, and cannot download from remote!")
        }

        stop = true
        checkUpdateThread.interrupt()
        checkUpdateThread.join()
    }
}