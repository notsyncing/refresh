package io.github.notsyncing.refresh.common

class PhasedVersion(major: Int, minor: Int, patch: Int, build: Int, val phase: Int) : Version(major, minor, patch, build) {
}