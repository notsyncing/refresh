package io.github.notsyncing.refresh.gradle

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtils {
    static void copyRecursive(Path sourceDir, Path targetDir) {
        def copyVisitor = new FileVisitor<Path>() {
            boolean isFirst = true
            Path ptr = null

            @Override
            FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Move ptr forward
                if (!this.isFirst) {
                    // .. but not for the first time since ptr is already in there
                    def target = this.ptr.resolve(dir.getName(dir.nameCount - 1))
                    this.ptr = target
                }
                Files.copy(dir, this.ptr, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
                this.isFirst = false
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                def target = this.ptr.resolve(file.fileName)
                Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult visitFileFailed(Path file, IOException exc) {
                throw exc
            }

            @Override
            FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                def target = this.ptr.parent
                // Move ptr backwards
                this.ptr = target
                return FileVisitResult.CONTINUE
            }
        }

        copyVisitor.ptr = targetDir
        Files.walkFileTree(sourceDir, copyVisitor)
    }

    static void deleteRecursive(Path dir) {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult postVisitDirectory(Path d, IOException exc) {
                Files.delete(d)
                return FileVisitResult.CONTINUE
            }
        })

        Files.deleteIfExists(dir)
    }

    static void pack(Path sourceDirPath, Path zipFilePath) throws IOException {
        Path p = Files.createFile(zipFilePath)
        new ZipOutputStream(Files.newOutputStream(p)).withCloseable { zs ->
            Files.walk(sourceDirPath)
                    .filter { !Files.isDirectory(it) }
                    .forEach { path ->
                def zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                try {
                    zs.putNextEntry(zipEntry)
                    zs.write(Files.readAllBytes(path))
                    zs.closeEntry()
                } catch (Exception e) {
                    System.err.println(e)
                }
            }
        }
    }
}