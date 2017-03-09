package io.github.notsyncing.refresh.app.client

import com.alibaba.fastjson.JSON
import io.github.notsyncing.refresh.app.RefreshConfig
import io.github.notsyncing.refresh.app.Refresher
import io.github.notsyncing.refresh.app.unique.UUIDProvider
import io.github.notsyncing.refresh.common.Version
import java.nio.file.Files
import java.nio.file.Paths

open class RefreshClient {
    companion object {
        val instance = RefreshClient()
    }

    private val config: RefreshConfig
    protected val refresher: Refresher

    init {
        val f = Paths.get("../../refresh.json")
        val s = String(Files.readAllBytes(f))
        val allConfig = JSON.parseObject(s)
        config = allConfig.getObject("app", RefreshConfig::class.java)
        refresher = Refresher(this::config, UUIDProvider())
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

    fun getDownloadedLatestVersion(): Version {
        return refresher.getLatestLocalVersion() ?: Version.empty
    }
}