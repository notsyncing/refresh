package io.github.notsyncing.refresh.server.user

import io.github.notsyncing.refresh.common.utils.sha256Salted
import java.io.InvalidObjectException

class User(val username: String,
           var password: String = "",
           val groups: MutableList<String> = mutableListOf()) {
    companion object {
        fun fromDataString(data: String): User {
            val ss = data.split(":")

            if (ss.size != 3) {
                throw InvalidObjectException("Wrong segment count when parsing user data $data")
            }

            val groups = ss[2].split(",").toMutableList()

            return User(ss[0], ss[1], groups)
        }
    }

    fun hashPassword(password: String): User {
        this.password = password.sha256Salted() ?: ""
        return this
    }

    fun toDataString() = "$username:$password:${groups.joinToString(",")}"
}