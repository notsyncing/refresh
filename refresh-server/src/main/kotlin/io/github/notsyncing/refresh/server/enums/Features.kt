package io.github.notsyncing.refresh.server.enums

object Features {
    const val CreateAppVersion = "refresh.app.version.create"
    const val DeleteApp = "refresh.app.delete"
    const val DeleteAppVersion = "refresh.app.version.delete"
    const val GetAppClientLatestVersion = "refresh.app.version.get_client_latest"
    const val GetAppLatestVersion = "refresh.app.version.get_latest"
    const val GetAppList = "refresh.app.get_list"
    const val GetAppVersion = "refresh.app.version.get_list"
    const val GetAppPhasedVersion = "refresh.app.version.get_phased_list"
    const val GetAppClientVersion = "refresh.app.version.get_client_list"
    const val ReloadApp = "refresh.app.reload"
    const val SetAppVersionPhase = "refresh.app.version.set_phase"

    const val GetClientList = "refresh.client.get_list"
    const val GetClientUpdatePhase = "refresh.client.get_phase"
    const val SetClientUpdatePhase = "refresh.client.set_phase"
}