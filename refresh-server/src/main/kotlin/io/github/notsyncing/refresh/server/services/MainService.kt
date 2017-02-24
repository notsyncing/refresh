package io.github.notsyncing.refresh.server.services

import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Route
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet
import io.github.notsyncing.cowherd.service.CowherdService

class MainService : CowherdService() {
    @Exported
    @HttpGet
    @Route("/", entry = true)
    fun index(): String {
        return "Refresh server is alive!"
    }
}