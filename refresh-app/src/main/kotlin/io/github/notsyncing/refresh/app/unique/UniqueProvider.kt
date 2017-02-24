package io.github.notsyncing.refresh.app.unique

abstract class UniqueProvider {
    abstract fun provide(): String
}