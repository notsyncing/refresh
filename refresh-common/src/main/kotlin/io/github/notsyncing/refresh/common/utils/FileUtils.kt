package io.github.notsyncing.refresh.common.utils

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun copyRecursive(sourceDir: Path, targetDir: Path) {
    abstract class MyFileVisitor : FileVisitor<Path> {
        var isFirst = true
        var ptr: Path? = null
    }

    val copyVisitor = object : MyFileVisitor() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            // Move ptr forward
            if (!isFirst) {
                // .. but not for the first time since ptr is already in there
                val target = ptr!!.resolve(dir.getName(dir.nameCount - 1).toString())
                ptr = target
            }
            Files.copy(dir, ptr, StandardCopyOption.REPLACE_EXISTING)
            isFirst = false
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            val target = ptr!!.resolve(file.fileName.toString())
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
            throw exc
        }

        override fun postVisitDirectory(dir: Path, exc: IOException): FileVisitResult {
            val target = ptr!!.parent
            // Move ptr backwards
            ptr = target
            return FileVisitResult.CONTINUE
        }
    }

    copyVisitor.ptr = targetDir
    Files.walkFileTree(sourceDir, copyVisitor)
}

fun deleteRecursive(dir: Path) {
    Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.delete(file)
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            Files.delete(dir)
            return FileVisitResult.CONTINUE
        }
    })

    Files.deleteIfExists(dir)
}

fun pack(sourceDirPath: Path, zipFilePath: Path) {
    val p = Files.createFile(zipFilePath)

    ZipOutputStream(Files.newOutputStream(p)).use { zs ->
        Files.walk(sourceDirPath)
                .filter { !Files.isDirectory(it) }
                .forEach { path ->
                    val zipEntry = ZipEntry(sourceDirPath.relativize(path).toString());
                    try {
                        zs.putNextEntry(zipEntry)
                        zs.write(Files.readAllBytes(path))
                        zs.closeEntry()
                    } catch (e: Exception) {
                        System.err.println(e)
                    }
                }
    }
}

fun hash(file: Path, type: String): String {
    Files.newInputStream(file).use {
        val buffer = ByteArray(1024)
        val complete = MessageDigest.getInstance(type)
        var numRead: Int

        do {
            numRead = it.read(buffer)
            if (numRead > 0) {
                complete.update(buffer, 0, numRead)
            }
        } while (numRead != -1)

        return complete.digest().toHexString()
    }
}