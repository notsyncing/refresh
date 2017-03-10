package io.github.notsyncing.refresh.firmware.stm32

import io.github.notsyncing.refresh.app.Refresher
import io.github.notsyncing.refresh.app.client.RefreshSubClient
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files

class Stm32RefreshClient(refresher: Refresher) : RefreshSubClient(refresher) {
    fun flash(port: String, baudrate: Int, filePath: String) {
        val dir = refresher.appCurrentVersionDir ?: throw FileNotFoundException("App current version not exists!")
        val firmwareFile = dir.resolve(filePath)

        if (!Files.exists(firmwareFile)) {
            throw FileNotFoundException("Firmware file $filePath not found in app current version directory $dir!")
        }

        val p = ProcessBuilder()
                .directory(dir.toFile())
                .command("stm32flash", "-w", firmwareFile.toString(), "-v", "-R", "-b", baudrate.toString(), port)
                .inheritIO()
                .start()

        val r = p.waitFor()

        if (r != 0) {
            throw IOException("stm32flash returned non-zero value $r!")
        }
    }
}