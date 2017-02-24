package io.github.notsyncing.refresh.server.scenes.app

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.Version
import io.github.notsyncing.refresh.server.app.AppManager
import io.github.notsyncing.refresh.server.client.ClientManager
import io.github.notsyncing.refresh.server.enums.Features
import io.github.notsyncing.refresh.server.enums.RefreshFeatureGroups
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

@Feature(Features.GetAppClientVersion, groups = arrayOf(RefreshFeatureGroups.RefreshApp))
class GetAppClientVersionsScene(private val appName: String,
                          private val clientData: Client,
                          private val top: Int = 0) : ManifoldScene<List<Version>>() {
    @AutoProvide
    lateinit var appManager: AppManager

    @AutoProvide
    lateinit var clientManager: ClientManager

    constructor() : this("", Client("", "", "", Version.empty, ""))

    override fun stage() = future {
        clientManager.updateClientData(clientData).await()

        val phase = clientManager.getClientUpdatePhase(clientData.accountIdentifier).await()

        return@future appManager.getAppVersions(appName, clientData, phase, top)
    }
}