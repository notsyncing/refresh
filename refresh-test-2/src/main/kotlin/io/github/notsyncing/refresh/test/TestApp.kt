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

    private var stop = false

    fun run() {
        println("I'm test app version 2!")

        RefreshClient.instance.setAccount("1354", "testUser")
        RefreshClient.instance.markAsStarted()

//        val dev = "/dev/serial/by-id/usb-Prolific_Technology_Inc._USB-Serial_Controller-if00-port0"
//
//        println("Opening port...")
//
//        val port = SerialPort.getCommPort(dev)
//        port.baudRate = 115200
//        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING, 500, 0)
//        port.openPort()
//
//        println("Opened port")
//
//        val t = thread {
//            port.inputStream.bufferedReader().use {
//                while (!stop) {
//                    try {
//                        println("Data from controller: ${it.readLine()}")
//                    } catch (e: Exception) {
//
//                    }
//                }
//            }
//        }
//
//        println("Waiting for write ready...")
//
//        while (!port.isOpen) {
//            Thread.sleep(100)
//        }
//
//        println("Writing command...")
//
//        port.outputStream.bufferedWriter().use {
//            it.write("version ")
//        }
//
//        Thread.sleep(100)
//
//        port.outputStream.bufferedWriter().use {
//            it.write("reboot_bl ")
//        }
//
//        println("Wroted")
//
//        Thread.sleep(1000)
//
//        stop = true
//        port.closePort()
//        t.join()
//
//        println("Port closed, start flashing...")
//
//        val stm32 = RefreshClient.get<Stm32RefreshClient>()
//        stm32.flash(dev, 115200, "firmware/fw_f2_h1.hex")
//
//        println("Flash done. Reopening port...")
//
//        port.openPort()
//
//        println("Port reopened, writing command...")
//
//        port.outputStream.bufferedWriter().use {
//            it.write("version ")
//        }
//
//        println("Wroted")
//
//        CompletableFuture.runAsync {
//            port.inputStream.bufferedReader().use {
//                while (port.isOpen) {
//                    if (!it.ready()) {
//                        continue
//                    }
//
//                    println("Data from controller: ${it.readLine()}")
//                }
//            }
//        }
//
//        Thread.sleep(5000)
//
//        println("Closing port...")
//
//        port.closePort()
//
//        println("Port closed")

        while (true) {
            Thread.sleep(1000)
        }
    }
}