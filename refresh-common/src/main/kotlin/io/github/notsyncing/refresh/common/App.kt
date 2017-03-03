package io.github.notsyncing.refresh.common

class App(var name: String,
          var versions: MutableList<Version> = mutableListOf(),
          var versionPhases: MutableMap<Version, Int> = mutableMapOf()) {
    constructor() : this("")

    override fun toString(): String {
        return name
    }
}