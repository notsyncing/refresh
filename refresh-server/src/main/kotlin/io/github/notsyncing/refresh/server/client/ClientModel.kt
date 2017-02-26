package io.github.notsyncing.refresh.server.client

import io.github.notsyncing.lightfur.entity.EntityModel
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.Version
import java.time.LocalDateTime

class ClientModel : EntityModel(table = "clients") {
    var accountId: String? by field(column = "account_id")

    var machineId: String by field(column = "machine_id", primaryKey = true)

    var accountName: String? by field(column = "account_name")

    var currentVersion: String? by field(column = "current_version")

    var additionalData: String? by field(column = "additional_data")

    var lastSeen: LocalDateTime? by field(column = "last_seen")

    fun toClient(): Client {
        return Client(accountId ?: "", machineId, accountName ?: "",
                Version.parse(currentVersion ?: "") ?: Version.empty, additionalData ?: "")
    }
}