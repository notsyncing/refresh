package io.github.notsyncing.refresh.common.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun String?.sha256(): String? {
    var strResult: String? = null

    if (this != null && this.isNotEmpty()) {
        try {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(this.toByteArray())

            val byteBuffer = messageDigest.digest()

            val strHexString = StringBuffer()

            for (i in byteBuffer.indices) {
                val hex = Integer.toHexString(0xff and byteBuffer[i].toInt())
                if (hex.length == 1) {
                    strHexString.append('0')
                }
                strHexString.append(hex)
            }

            strResult = strHexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }

    return strResult
}

private const val SALT = "HeLlO_i_Am_SaLt"

fun String?.sha256Salted(): String? {
    val stage1 = this.sha256()
    val stage2 = "$stage1${SALT}".sha256()

    return stage2
}

private val HEX_CHARS = "0123456789abcdef".toCharArray()

fun ByteArray.toHexString(): String {
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}