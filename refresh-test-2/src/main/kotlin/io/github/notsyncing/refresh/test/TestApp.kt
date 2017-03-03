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
        println("I'm test app version 2!")

        RefreshClient.instance.setAccount("1354", "testUser")

        while (true) {
            Thread.sleep(1000)
        }
    }
}