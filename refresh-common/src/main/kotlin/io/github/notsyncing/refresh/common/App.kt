package io.github.notsyncing.refresh.common

class App(val name: String,
          val versions: MutableList<Version> = mutableListOf(),
          val versionPhases: MutableMap<Version, Int> = mutableMapOf()) {
    override fun toString(): String {
        return name
    }
}