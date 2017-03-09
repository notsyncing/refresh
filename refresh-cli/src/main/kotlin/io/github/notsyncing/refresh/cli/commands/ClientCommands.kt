package io.github.notsyncing.refresh.cli.commands

import com.alibaba.fastjson.JSON
import io.github.notsyncing.refresh.common.ClientListItem
import io.github.notsyncing.refresh.common.enums.OperationResult

class ClientCommands : CommandBase() {
    @Command
    fun clientList() {
        val r = get(api("ClientService/getClientList"))

        val list = JSON.parseArray(r, ClientListItem::class.java)

        println("account name machine currentVersion lastSeen")

        list.forEach {
            println("${if (it.accountIdentifier.isEmpty()) "<NULL>" else it.accountIdentifier} " +
                    "${if (it.accountName.isEmpty()) "<NULL>" else it.accountName} " +
                    "${if (it.machineIdentifier.isEmpty()) "<NULL>" else it.machineIdentifier} " +
                    "${it.currentVersion} " +
                    "${it.lastSeen}")
        }

        println("total ${list.size} clients")
    }

    @Command
    fun clientUpdatePhase(clientAccountId: String) {
        val r = get(api("ClientService/getClientUpdatePhase"), listOf("client" to clientAccountId))

        println("$clientAccountId: $r")
    }

    @Command
    fun setClientUpdatePhase(clientAccountId: String, phase: String) {
        val r = post(api("ClientService/setClientUpdatePhase"),
                listOf("client" to clientAccountId, "phase" to phase))

        if (r == OperationResult.Success.ordinal.toString()) {
            println("Set client update phase succeeded.")
        } else {
            println("Server returned wrong result: $r")
        }
    }
}