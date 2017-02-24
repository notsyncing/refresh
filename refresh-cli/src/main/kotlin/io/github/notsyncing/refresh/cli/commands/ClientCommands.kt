package io.github.notsyncing.refresh.cli.commands

import com.alibaba.fastjson.JSON
import com.mashape.unirest.http.Unirest
import io.github.notsyncing.refresh.common.Client
import io.github.notsyncing.refresh.common.enums.OperationResult

class ClientCommands : CommandBase() {
    @Command
    fun clientList() {
        val r = Unirest.get(api("ClientService/getClientList"))
                .asString()
                .body

        val list = JSON.parseArray(r, Client::class.java)

        list.forEach { println("${it.accountIdentifier} ${it.accountName} ${it.machineIdentifier} ${it.currentVersion}") }
    }

    @Command
    fun clientUpdatePhase(clientAccountId: String) {
        val r = Unirest.get(api("ClientService/getClientUpdatePhase"))
                .queryString("client", clientAccountId)
                .asString()
                .body

        println("$clientAccountId: $r")
    }

    @Command
    fun setClientUpdatePhase(clientAccountId: String, phase: Int) {
        val r = Unirest.post(api("ClientService/setClientUpdatePhase"))
                .queryString(mapOf("client" to clientAccountId, "phase" to phase))
                .asString()
                .body

        if (r == OperationResult.Success.ordinal.toString()) {
            println("Set client update phase succeeded.")
        } else {
            println("Server returned wrong result: $r")
        }
    }
}