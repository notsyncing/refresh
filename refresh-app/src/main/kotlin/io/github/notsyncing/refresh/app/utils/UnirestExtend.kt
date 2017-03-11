package io.github.notsyncing.refresh.app.utils

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.async.Callback
import com.mashape.unirest.http.exceptions.UnirestException
import com.mashape.unirest.request.BaseRequest
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

fun BaseRequest.asStringAsyncFuture(): CompletableFuture<String> {
    val f = CompletableFuture<String>()

    this.asStringAsync(object : Callback<String> {
        override fun completed(response: HttpResponse<String>?) {
            f.complete(response?.body)
        }

        override fun failed(e: UnirestException?) {
            f.completeExceptionally(e)
        }

        override fun cancelled() {
            f.completeExceptionally(CancellationException())
        }
    })

    return f
}