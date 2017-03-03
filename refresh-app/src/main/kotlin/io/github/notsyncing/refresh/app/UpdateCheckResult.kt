package io.github.notsyncing.refresh.app

import io.github.notsyncing.refresh.common.Version

data class UpdateCheckResult(val remoteVersion: Version,
                             val localVersion: Version) {
    fun hasUpdate() = (remoteVersion != Version.empty) && (remoteVersion != localVersion)

    fun isUpgrade() = (remoteVersion != Version.empty) && (remoteVersion > localVersion)

    fun isDowngrade() = (remoteVersion != Version.empty) && (remoteVersion < localVersion)
}