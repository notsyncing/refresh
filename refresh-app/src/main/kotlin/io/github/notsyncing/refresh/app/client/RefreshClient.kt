package io.github.notsyncing.refresh.app.client

import io.github.notsyncing.refresh.app.Refresher
import io.github.notsyncing.refresh.common.Version
import java.nio.file.Files
import java.nio.file.Paths

open class RefreshClient(val refresher: Refresher) {
    companion object {
        lateinit var instance: RefreshClient

        inline fun <reified T: RefreshClient> makeRefreshClient(): T {
            return T::class.java.constructors[0].newInstance(instance.refresher) as T
        }
    }

    fun setAccount(id: String, name: String, tryCount: Int = 5) {
        if (tryCount <= 0) {
            return
        }

        val p = Paths.get(".account")

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