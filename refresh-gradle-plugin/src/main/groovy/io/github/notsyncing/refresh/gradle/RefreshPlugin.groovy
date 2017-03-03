package io.github.notsyncing.refresh.gradle

import groovy.json.JsonBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission

class RefreshPlugin implements Plugin<Project> {
    private void downloadLauncher(Project project, Path toDir) {
        project.configurations {
            copyMaven
        }

        def launcherVer = project.refreshPackage.launcherVersion

        project.dependencies {
            copyMaven "io.github.notsyncing.refresh:refresh-app-launcher:$launcherVer"
        }

        project.copy {
            from project.configurations.copyMaven.singleFile
            into toDir.toFile()
        }
    }

    private void createConfigFile(Project project, Path toFile) {
        def config = new JsonBuilder()
        config(app: [name: project.refreshPackage.name, updateServer: project.refreshPackage.updateServer,
                     cmdLine: project.refreshPackage.cmdLine])

        Files.newBufferedWriter(toFile, StandardOpenOption.CREATE).withWriter {
            config.writeTo(it)
        }
    }

    private void createStartFile(Project project, Path toFile) {
        def launcherVer = project.refreshPackage.launcherVersion

        Files.newBufferedWriter(toFile, StandardOpenOption.CREATE).withWriter {
            it.write("#!/bin/sh\n")
            it.write("\n")
            it.write("java -cp refresh-app-launcher-${launcherVer}.jar io.github.notsyncing.refresh.app.RefreshAppLauncher")
        }

        Files.setPosixFilePermissions(toFile, [
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OTHERS_READ
        ] as Set)
    }

    @Override
    void apply(Project project) {
        project.extensions.create("refreshPackage", RefreshPluginExtension)

        project.task("makeNetworkInstaller") << {
            def tempDir = project.buildDir.toPath().resolve("refresh").resolve("network")

            if (Files.exists(tempDir)) {
                FileUtils.deleteRecursive(tempDir)
            }

            Files.createDirectories(tempDir)

            downloadLauncher(project, tempDir)
            createConfigFile(project, tempDir.resolve("refresh.json"))
            createStartFile(project, tempDir.resolve("start.sh"))
        }

        def makeAppPackageTask = project.task("makeAppPackage") << {
            def tempDir = project.buildDir.toPath().resolve("refresh").resolve("package")

            if (Files.exists(tempDir)) {
                FileUtils.deleteRecursive(tempDir)
            }

            Files.createDirectories(tempDir)

            downloadLauncher(project, tempDir)
            createConfigFile(project, tempDir.resolve("refresh.json"))
            createStartFile(project, tempDir.resolve("start.sh"))

            def appDir = tempDir.resolve(project.refreshPackage.name).resolve(project.refreshPackage.version)
            Files.createDirectories(appDir)

            project.copy {
                from project.refreshPackage.appJar.archivePath
                into appDir.toFile()
            }

            def s = tempDir.parent.resolve("${project.refreshPackage.name}-${project.refreshPackage.version}.zip")
                    .toAbsolutePath()

            if (Files.exists(s)) {
                Files.delete(s)
            }

            FileUtils.pack(tempDir, s)
        }

        project.afterEvaluate {
            makeAppPackageTask.dependsOn(project.refreshPackage.appJar)
        }
    }
}
