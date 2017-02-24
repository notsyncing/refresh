package io.github.notsyncing.refresh.common.utils

import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

fun isUrlReachable(url: String): Boolean {
    try {
        val u = URL(url)
        val conn = u.openConnection()
        conn.connect()
        return true
    } catch (e: MalformedURLException) {
        throw RuntimeException(e)
    } catch (e: IOException) {
        return false
    }
}