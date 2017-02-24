package io.github.notsyncing.refresh.server.client

import io.github.notsyncing.lightfur.entity.dsl.EntityDSL
import io.github.notsyncing.lightfur.entity.eq
import io.github.notsyncing.manifold.di.EarlyProvide
import io.github.notsyncing.manifold.di.ProvideAsSingleton
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.enums.OperationResult
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@ProvideAsSingleton
@EarlyProvide
class ClientManager {
    fun updateClientData(clientData: Client): CompletableFuture<OperationResult> {
        val m = ClientModel()
        m.accountId = clientData.accountIdentifier
        m.machineId = clientData.machineIdentifier
        m.accountName = clientData.accountName
        m.currentVersion = clientData.currentVersion.toString()
        m.additionalData = clientData.additionalData
        m.lastSeen = LocalDateTime.now()

        return EntityDSL.insert(m)
                .values()
                .updateWhenExists(m.F(m::machineId)) {
                    it.set()
                            .where { m.F(m::machineId) eq m.machineId }
                }
                .execute()
                .thenApply { (_, c) -> if (c > 0) OperationResult.Success else OperationResult.Failed }
    }

    fun getClientUpdatePhase(accountId: String): CompletableFuture<Int> {
        if (accountId.isEmpty()) {
            return CompletableFuture.completedFuture(-1)
        }

        val m = ClientPhaseModel()

        return EntityDSL.select(m)
                .from()
                .where { m.F(m::accountId) eq accountId }
                .execute()
                .thenApply { (l, _) -> if (l.isEmpty()) -2 else l[0].phase }
    }

    fun setClientUpdatePhase(accountId: String, phase: Int): CompletableFuture<OperationResult> {
        val m = ClientPhaseModel()
        m.accountId = accountId
        m.phase = phase

        return EntityDSL.insert(m)
                .values()
                .updateWhenExists(m.F(m::accountId)) {
                    it.set()
                            .where { m.F(m::accountId) eq accountId }
                }
                .execute()
                .thenApply { (_, c) -> if (c > 0) OperationResult.Success else OperationResult.Failed }
    }

    fun getClientList(): CompletableFuture<List<Client>> {
        val m = ClientModel()

        return EntityDSL.select(m)
                .from()
                .execute()
                .thenApply { (l, _) -> l.map { it.toClient() } }
    }
}