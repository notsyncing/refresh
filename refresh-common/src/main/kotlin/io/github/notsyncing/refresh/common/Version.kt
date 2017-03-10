package io.github.notsyncing.refresh.common

open class Version(var major: Int, var minor: Int, var patch: Int, var build: Int) : Comparable<Version> {
    companion object {
        val empty = Version(0, 0, 0, 0)

        fun parse(ver: String): Version? {
            val v = ver.trim(' ', '\r', '\n', '\t')
            val ss = v.split(".")

            if (ss.size != 3) {
                return null
            }

            val ss2: List<String>

            if (ss[2].contains("-")) {
                ss2 = ss[2].split("-")

                if (ss2.size != 2) {
                    return null
                }
            } else {
                ss2 = listOf(ss[2], "0")
            }

            return Version(ss[0].toInt(), ss[1].toInt(), ss2[0].toInt(), ss2[1].toInt())
        }
    }

    constructor(major: Int, minor: Int, patch: Int) : this(major, minor, patch, 0)

    constructor() : this(0, 0, 0)

    override fun compareTo(other: Version): Int {
        if (this.major > other.major) {
            return 1
        } else if (this.major < other.major) {
            return -1
        }

        if (this.minor > other.major) {
            return 1
        } else if (this.minor < other.minor) {
            return -1
        }

        if (this.patch > other.patch) {
            return 1
        } else if (this.patch < other.patch) {
            return -1
        }

        if (this.build > other.build) {
            return 1
        } else if (this.build < other.build) {
            return -1
        }

        return 0
    }

    override fun toString(): String {
        if (build > 0) {
            return "$major.$minor.$patch-$build"
        } else {
            return "$major.$minor.$patch"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Version) {
            return super.equals(other)
        }

        return (this.major == other.major) && (this.minor == other.minor) && (this.patch == other.patch)
                && (this.build == other.build)
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}