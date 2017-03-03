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

        System.exit(1)
    }
}