package io.github.notsyncing.refresh.cli.commands

import com.mashape.unirest.http.Unirest
import java.nio.file.Path

abstract class CommandBase {
    companion object {
        var host: String = "http://localhost:48622"
        var token: String = ""
    }

    protected fun api(url: String): String {
        return "$host/api/$url"
    }

    protected fun get(url: String, params: List<Pair<String, Any?>>? = null): String? {
        val resp = Unirest.get(api(url))
                .apply {
                    params?.forEach { (k, v) -> this.queryString(k, v) }

                    if (token.isNotEmpty()) {
                        this.header("Cookie", "token=$token")
                    }
                }
                .asString()

        if (resp.status != 200) {
            throw RuntimeException("Error when getting $url: ${resp.status} ${resp.statusText}")
        }

        return resp.body
    }

    protected fun post(url: String, params: List<Pair<String, Any?>>? = null,
                       files: List<Pair<String, Path>>? = null): String? {
        val resp = Unirest.post(api(url))
                .apply {
                    params?.forEach { (k, v) -> this.queryString(k, v) }

                    files?.forEach { (field, path) -> this.field(field, path.toFile()) }

                    if (token.isNotEmpty()) {
                        this.header("Cookie", "token=$token")
                    }
                }
                .asString()

        if (resp.status != 200) {
            throw RuntimeException("Error when posting $url: ${resp.status} ${resp.statusText}")
        }

        return resp.body
    }
}