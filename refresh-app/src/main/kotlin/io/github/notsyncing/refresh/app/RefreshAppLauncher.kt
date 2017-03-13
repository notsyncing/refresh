package io.github.notsyncing.refresh.app

import com.alibaba.fastjson.JSON
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

    private lateinit var config: RefreshConfig
    private val refresher: Refresher
    private var stop = false
    private var gui: RefreshAppLauncherGui? = null

    init {
        reloadConfig()

        if (config.useGuiLauncher) {
            println("Use GUI launcher.")

            gui = RefreshAppLauncherGui()
            gui!!.changeText("正在启动...")
            gui!!.changeProgress(-1)
            gui!!.show()
        }

        refresher = Refresher(this::config, UUIDProvider())

        refresher.onAppDownloaded = {
            reloadConfig()
        }

        Files.deleteIfExists(Paths.get(".account"))
    }

    private fun reloadConfig() {
        val f = Paths.get("refresh.json")
        val s = String(Files.readAllBytes(f))
        val allConfig = JSON.parseObject(s)
        config = allConfig.getObject("app", RefreshConfig::class.java)
    }

    private fun launchApp(localVer: Version) {
        val p = Paths.get(config.name, localVer.toString()).toAbsolutePath()
        val app = ProcessBuilder()
                .inheritIO()
                .directory(p.toFile())
                .command(config.cmdLine.split(" "))
                .also {
                    println("------------")
                }
                .start()

        val startedFlag = Paths.get(".started")

        if (config.useGuiLauncher) {
            while (!Files.exists(startedFlag)) {
                Thread.sleep(1000)
            }

            gui!!.hide()
        }

        Files.deleteIfExists(startedFlag)

        val appResult = app.waitFor()

        println("------------")

        if (appResult != 0) {
            println("App exited abnormally with code $appResult. Checking for alternative versions...")

            if (config.useGuiLauncher) {
                gui!!.changeText("应用异常退出，正在检查更新...")
                gui!!.show()
            }

            val ur = refresher.checkForUpdate()

            if (ur.hasUpdate()) {
                println("Update found. local version ${ur.localVersion}, remote version ${ur.remoteVersion}. Will download it and retry.")

                if (config.useGuiLauncher) {
                    gui!!.changeText("发现新版本，正在下载...")
                }

                val r = refresher.checkAndDownload()

                if (r != OperationResult.Success) {
                    println("Failed to download update!")
                    return
                }

                println("Starting version ${ur.remoteVersion}")

                launchApp(ur.remoteVersion)
            } else {
                println("No update found. Rolling back to previous version...")

                if (config.useGuiLauncher) {
                    gui!!.changeText("未发现新版本，正在回滚上一版本...")
                }

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

            val restartFlag = Paths.get(".restart")

            if (Files.exists(restartFlag)) {
                Files.delete(restartFlag)

                val lv = refresher.getCurrentLocalVersion() ?: refresher.getLatestLocalVersion()
                launchApp(lv!!)
            }
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
            println("Update checker thread started.")

            while (!stop) {
                try {
                    if (!Files.exists(accountFile)) {
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

            println("Update checker thread stopped.")
        }

        if (localVer != null) {
            launchApp(localVer)
        } else {
            println("Local version not found, and cannot download from remote!")
        }

        stop = true
        checkUpdateThread.interrupt()
        checkUpdateThread.join()

        if (config.useGuiLauncher) {
            gui!!.destroy()
        }
    }
}