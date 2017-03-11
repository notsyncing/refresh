package io.github.notsyncing.refresh.server.reporting

import io.github.notsyncing.manifold.di.EarlyProvide
import io.github.notsyncing.manifold.di.ProvideAsSingleton
import io.github.notsyncing.refresh.common.enums.OperationResult
import kotlinx.coroutines.experimental.future.future
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@ProvideAsSingleton
@EarlyProvide
class ReportingManager {
    companion object {
        var reportFileStoragePath = Paths.get(".", "reports")
    }

    init {
        if (!Files.exists(reportFileStoragePath)) {
            Files.createDirectories(reportFileStoragePath)
        }
    }

    fun storeReportedFile(machineId: String, filename: String, file: Path) = future<OperationResult> {
        val dir = reportFileStoragePath.resolve(machineId)

        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        Files.copy(file, dir.resolve(filename))

        OperationResult.Success
    }
}