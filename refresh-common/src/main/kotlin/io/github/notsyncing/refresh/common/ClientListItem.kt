package io.github.notsyncing.refresh.common

import com.alibaba.fastjson.annotation.JSONField
import java.time.LocalDateTime

data class ClientListItem(@field:JSONField(name = "accountId") var accountIdentifier: String,
                          @field:JSONField(name = "machineId") var machineIdentifier: String,
                          var accountName: String,
                          var currentVersion: String,
                          var additionalData: String,
                          var lastSeen: LocalDateTime) {
    constructor() : this("", "", "", Version.empty.toString(), "", LocalDateTime.now()) {

    }
}