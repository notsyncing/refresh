package io.github.notsyncing.refresh.gradle

import org.gradle.jvm.tasks.Jar

class RefreshPluginExtension {
    def String name = ""
    def String version = ""
    def String updateServer = ""
    def String cmdLine = ""

    def String launcherVersion = ""
    def Jar appJar = null
}