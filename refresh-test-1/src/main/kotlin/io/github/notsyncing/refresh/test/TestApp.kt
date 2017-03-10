package io.github.notsyncing.refresh.test

import com.fazecast.jSerialComm.SerialPort
import io.github.notsyncing.refresh.app.client.RefreshClient
import io.github.notsyncing.refresh.firmware.stm32.Stm32RefreshClient
import java.util.concurrent.CompletableFuture

class TestApp {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = TestApp()
            app.run()
        }
    }

    fun run() {
        println("I'm test app version 1!")

        RefreshClient.instance.setAccount("1354", "testUser")

        println("Opening port...")

        val port = SerialPort.getCommPort("/dev/cu.usbserial")
        port.openPort()

        println("Opened port")

        CompletableFuture.runAsync {
            port.inputStream.bufferedReader().use {
                while (port.isOpen) {
                    if (!it.ready()) {
                        continue
                    }

                    println("Data from controller: ${it.readLine()}")
                }
            }
        }

        println("Waiting for write ready...")

        while (!port.isOpen) {
            Thread.sleep(100)
        }

        println("Writing command...")

        port.outputStream.bufferedWriter().use {
            it.write("reboot_bl ")
        }

        println("Wroted")

        Thread.sleep(1000)

        port.closePort()

        println("Port closed, start flashing...")

        val stm32 = RefreshClient.get<Stm32RefreshClient>()
        stm32.flash("/dev/cu.usbserial", 115200, "firmware/fw_f1_h1.hex")

        println("Flash done. Reopening port...")

        port.openPort()

        println("Port reopened, waiting for response...")

        CompletableFuture.runAsync {
            port.inputStream.bufferedReader().use {
                while (port.isOpen) {
                    if (!it.ready()) {
                        continue
                    }

                    println("Data from controller: ${it.readLine()}")
                }
            }
        }

        Thread.sleep(5000)

        println("Closing port...")

        port.closePort()

        println("Port closed")

        while (true) {
            Thread.sleep(1000)
        }
    }
}