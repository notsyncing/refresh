package io.github.notsyncing.refresh.common

data class Client(val accountIdentifier: String,
                  val machineIdentifier: String,
                  val accountName: String,
                  val currentVersion: Version,
                  val additionalData: String) {
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