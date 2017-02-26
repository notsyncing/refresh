package io.github.notsyncing.refresh.server.client

import io.github.notsyncing.lightfur.entity.EntityModel

class ClientPhaseModel : EntityModel(table = "client_phases") {
    var accountId: String by field(column = "account_id", primaryKey = true)

    var phase: Int by field(column = "phase")
}