package io.github.notsyncing.refresh.reporting

import com.mashape.unirest.http.Unirest
import io.github.notsyncing.refresh.app.Refresher
import io.github.notsyncing.refresh.app.client.RefreshSubClient
import io.github.notsyncing.refresh.app.utils.asStringAsyncFuture
import io.github.notsyncing.refresh.common.enums.OperationResult
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.nio.file.Path

class ReportingRefreshClient(refresher: Refresher) : RefreshSubClient(refresher) {
    fun reportFile(file: Path) = future<OperationResult> {
        val fn = file.fileName.toString()

        val r = Unirest.post(refresher.updateServerUrl("ReportingService/reportFile"))
                .field("machine_id", refresher.machineId)
                .field("file", file.toFile())
                .asStringAsyncFuture()
                .await()

        if (r == OperationResult.Success.ordinal.toString()) {
            return@future OperationResult.Success
        } else {
            return@future OperationResult.Failed
        }
    }
}