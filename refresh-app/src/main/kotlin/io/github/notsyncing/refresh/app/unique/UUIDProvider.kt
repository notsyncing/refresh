package io.github.notsyncing.refresh.app.unique

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

class UUIDProvider : UniqueProvider() {
    override fun provide(): String {
        var f = Paths.get("../../unique.uuid")

        if (!Files.exists(f)) {
            f = Paths.get("unique.uuid")

            if (!Files.exists(f)) {
                val uuid = UUID.randomUUID().toString()

                Files.write(f, uuid.toByteArray(), StandardOpenOption.CREATE)

                return uuid
            }
        }

        return String(Files.readAllBytes(f))
    }
}