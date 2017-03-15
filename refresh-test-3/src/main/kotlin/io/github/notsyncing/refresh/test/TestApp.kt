package io.github.notsyncing.refresh.test

import io.github.notsyncing.refresh.app.client.RefreshClient

class TestApp {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = TestApp()
            app.run()
        }
    }

    fun run() {
        println("I'm test app version 3!")

        RefreshClient.instance.setAccount("1354", "testUser")

        Thread.sleep(3000)

        RefreshClient.instance.markAsStarted()

        println("Really started!")

        //RefreshClient.instance.markAsRestart()

//        val reporter = RefreshClient.get<ReportingRefreshClient>()
//        val f = Files.createTempFile("refresh-test-", ".txt")
//        Files.write(f, "test report file".toByteArray())
//
//        reporter.reportFile(f).thenAccept {
//            Files.delete(f)
//        }
//
        while (true) {
            Thread.sleep(1000)
        }
    }
}