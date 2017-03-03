package io.github.notsyncing.refresh.common

class PhasedVersion(major: Int, minor: Int, patch: Int, build: Int, var phase: Int) : Version(major, minor, patch, build) {
    constructor() : this(0, 0, 0, 0, 0)

    override fun toString(): String {
        return super.toString() + "@$phase"
    }
}