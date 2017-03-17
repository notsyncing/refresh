package io.github.notsyncing.refresh.common

import java.io.IOException
import java.nio.file.Path

object XDelta {
    private fun makeWithXDelta(fromFile: Path, toFile: Path, patchFile: Path) {
        println("Starting xdelta: from $fromFile to $toFile result $patchFile")

        val xdelta = ProcessBuilder()
                .command("xdelta", "delta", fromFile.toAbsolutePath().toString(),
                        toFile.toAbsolutePath().toString(),
                        patchFile.toAbsolutePath().toString())
                .inheritIO()
                .start()

        val r = xdelta.waitFor()

        println("xdelta returned $r")

        if (r != 1) {
            throw IOException("xdelta returned $r")
        }
    }

    private fun makeWithXDelta3(fromFile: Path, toFile: Path, patchFile: Path) {
        println("Starting xdelta3: from $fromFile to $toFile result $patchFile")

        val xdelta = ProcessBuilder()
                .command("xdelta3", "-e", "-s", fromFile.toAbsolutePath().toString(),
                        toFile.toAbsolutePath().toString(),
                        patchFile.toAbsolutePath().toString())
                .inheritIO()
                .start()

        val r = xdelta.waitFor()

        println("xdelta3 returned $r")

        if (r != 1) {
            throw IOException("xdelta3 returned $r")
        }
    }

    fun make(fromFile: Path, toFile: Path, patchFile: Path) {
        makeWithXDelta3(fromFile, toFile, patchFile)
    }

    private fun patchWithXDelta(fromFile: Path, patchFile: Path, toFile: Path) {
        println("Starting xdelta: apply patch $patchFile on $fromFile result $toFile")

        val xdelta = ProcessBuilder()
                .command("xdelta", "patch", patchFile.toAbsolutePath().toString(),
                        fromFile.toAbsolutePath().toString(),
                        toFile.toAbsolutePath().toString())
                .inheritIO()
                .start()

        val r = xdelta.waitFor()

        println("xdelta returned $r")

        if (r != 0) {
            throw IOException("xdelta returned $r")
        }
    }

    private fun patchWithXDelta3(fromFile: Path, patchFile: Path, toFile: Path) {
        println("Starting xdelta3: apply patch $patchFile on $fromFile result $toFile")

        val xdelta = ProcessBuilder()
                .command("xdelta3", "-d", "-s", fromFile.toAbsolutePath().toString(),
                        patchFile.toAbsolutePath().toString(),
                        toFile.toAbsolutePath().toString())
                .inheritIO()
                .start()

        val r = xdelta.waitFor()

        println("xdelta3 returned $r")

        if (r != 0) {
            throw IOException("xdelta3 returned $r")
        }
    }

    fun patch(fromFile: Path, patchFile: Path, toFile: Path) {
        patchWithXDelta3(fromFile, patchFile, toFile)
    }
}