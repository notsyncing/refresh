package io.github.notsyncing.refresh.app.client

import com.alibaba.fastjson.JSON
import io.github.notsyncing.refresh.app.RefreshConfig
import io.github.notsyncing.refresh.app.Refresher
import io.github.notsyncing.refresh.app.unique.UUIDProvider
import io.github.notsyncing.refresh.common.Version
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

class RefreshClient {
    companion object {
        val instance = RefreshClient()
        val subClients = mutableMapOf<Class<RefreshSubClient>, RefreshSubClient>()

        inline fun <reified T: RefreshSubClient> get(): T {
            var o = subClients[T::class.java as Class<RefreshSubClient>]

            if (o == null) {
                o = T::class.java.constructors[0].newInstance(instance.refresher) as RefreshSubClient
                subClients[T::class.java as Class<RefreshSubClient>] = o
            }

            return o as T
        }
    }

    private val config: RefreshConfig
    protected val refresher: Refresher

    init {
        val f = Paths.get("../../refresh.json")
        val s = String(Files.readAllBytes(f))
        val allConfig = JSON.parseObject(s)
        config = allConfig.getObject("app", RefreshConfig::class.java)
        refresher = Refresher(this::config, UUIDProvider(), true)

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            val startFlag = Paths.get("../../.started")
            Files.deleteIfExists(startFlag)
        })
    }

    fun setAccount(id: String, name: String, tryCount: Int = 5) {
        if (tryCount <= 0) {
            return
        }

        val p = Paths.get("../../.account")

        try {
            Files.write(p, listOf(id, name))
        } catch (e: Exception) {
            Thread.sleep(2000)
            setAccount(id, name, tryCount - 1)
        }
    }

    fun getCurrentVersion(): Version {
        return refresher.getCurrentLocalVersion() ?: Version.empty
    }

    fun getLatestVersion(): Version {
        return refresher.getCurrentRemoteVersion() ?: Version.empty
    }

    fun getLatestVersionAsync(): CompletableFuture<Version> {
        return CompletableFuture.supplyAsync { refresher.getCurrentRemoteVersion() }
    }

    fun getDownloadedLatestVersion(): Version {
        return refresher.getLatestLocalVersion() ?: Version.empty
    }

    fun markAsRestart() {
        val f = Paths.get("../../.restart")
        Files.write(f, "restart".toByteArray())
    }

    fun getCurrentDownloadingVersion(): Version? {
        return refresher.getCurrentDownloadingVersion()
    }

    fun markAsStarted() {
        val f = Paths.get("../../.started")
        Files.write(f, "started".toByteArray())
    }
}