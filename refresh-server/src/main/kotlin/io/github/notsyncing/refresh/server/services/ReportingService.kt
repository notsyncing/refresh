package io.github.notsyncing.refresh.server.services

import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpPost
import io.github.notsyncing.cowherd.models.UploadFileInfo
import io.github.notsyncing.cowherd.service.CowherdService
import io.github.notsyncing.refresh.common.enums.OperationResult
import io.github.notsyncing.refresh.server.reporting.ReportingManager
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

class ReportingService(private val rm: ReportingManager) : CowherdService() {
    @Exported
    @HttpPost
    fun reportFile(@Parameter("machine_id") machineId: String,
                   @Parameter("file") file: UploadFileInfo) = future<OperationResult> {
        rm.storeReportedFile(machineId, file.filename, file.file.toPath()).await()
    }
}