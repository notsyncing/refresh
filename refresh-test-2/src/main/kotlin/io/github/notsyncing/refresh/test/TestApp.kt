package io.github.notsyncing.refresh.test

import io.github.notsyncing.refresh.app.client.RefreshClient
import io.github.notsyncing.refresh.firmware.stm32.Stm32RefreshClient

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

        val stm32 = RefreshClient.get<Stm32RefreshClient>()
        stm32.flash("/dev/cu.usbserial", 115200, "firmware/fw_f2_h1.hex")

        while (true) {
            Thread.sleep(1000)
        }
    }
}