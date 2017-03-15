package io.github.notsyncing.refresh.reporting

import com.mashape.unirest.http.Unirest
import io.github.notsyncing.refresh.app.Refresher
import io.github.notsyncing.refresh.app.client.RefreshSubClient
import io.github.notsyncing.refresh.app.utils.asStringAsyncFuture
import io.github.notsyncing.refresh.common.enums.OperationResult
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ReportingRefreshClient(refresher: Refresher) : RefreshSubClient(refresher) {
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val reportWorkers = Executors.newScheduledThreadPool(1) {
        val t = Thread(it)
        t.isDaemon = true
        t.priority = Thread.MIN_PRIORITY
        t
    }

    fun reportFile(file: Path) = future<OperationResult> {
        val fn = file.fileName.toString()

        val r = Unirest.post(refresher.updateServerUrl("ReportingService/reportFile"))
                .field("machine_id", refresher.machineId)
                .field("file", file.toFile())
                .asStringAsyncFuture()
                .await()

        if (r == OperationResult.Success.ordinal.toString()) {
            val lastReportedData = timeFormatter.format(LocalDateTime.now())
            val lastReportedFlag = refresher.appDir.resolve(".last_reported")

            Files.write(lastReportedFlag, lastReportedData.toByteArray(), StandardOpenOption.CREATE)

            return@future OperationResult.Success
        } else {
            return@future OperationResult.Failed
        }
    }

    fun getLastReportedTime(): LocalDateTime? {
        val lastReportedFlag = refresher.appDir.resolve(".last_reported")

        if (!Files.exists(lastReportedFlag)) {
            return null
        }

        val lastReportedData = String(Files.readAllBytes(lastReportedFlag))

        return LocalDateTime.parse(lastReportedData, timeFormatter)
    }

    fun reportFilePeriodic(period: Long, timeUnit: TimeUnit, fileGetter: () -> Path?,
                           reportCallback: (OperationResult, Throwable?) -> Unit) {
        reportWorkers.scheduleAtFixedRate({
            println("Begin periodic reporting.")

            val f = fileGetter()

            if (f == null) {
                println("Nothing to report.")
                return@scheduleAtFixedRate
            }

            println("Reporting file: $f")

            reportFile(f)
                    .thenAccept {
                        println("Periodic report result: $it")

                        reportCallback(it, null)
                    }
                    .exceptionally {
                        println("Periodic report failed!")
                        it.printStackTrace()

                        reportCallback(OperationResult.Failed, it)

                        null
                    }
        }, 0, period, timeUnit)
    }
}