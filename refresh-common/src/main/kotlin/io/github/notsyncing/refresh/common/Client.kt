package io.github.notsyncing.refresh.common

data class Client(var accountIdentifier: String,
                  var machineIdentifier: String,
                  var accountName: String,
                  var currentVersion: Version,
                  var additionalData: String) {
    constructor() : this("", "", "", Version.empty, "") {

    }

    fun modifyAccount(newId: String): Client {
        return Client(newId, machineIdentifier, accountName, currentVersion, additionalData)
    }

    fun modifyAccount(newId: String, newName: String): Client {
        return Client(newId, machineIdentifier, newName, currentVersion, additionalData)
    }

    fun modifyVersion(newVersion: Version): Client {
        return Client(accountIdentifier, machineIdentifier, accountName, newVersion, additionalData)
    }
}