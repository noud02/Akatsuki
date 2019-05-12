/*
 *   Copyright (c) 2017-2019 Yui
 *
 *   Permission is hereby granted, free of charge, to any person
 *   obtaining a copy of this software and associated documentation
 *   files (the "Software"), to deal in the Software without
 *   restriction, including without limitation the rights to use,
 *   copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the
 *   Software is furnished to do so, subject to the following
 *   conditions:
 *
 *   The above copyright notice and this permission notice shall be
 *   included in all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *   OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *   HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *   WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *   FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *   OTHER DEALINGS IN THE SOFTWARE.
 */

package moe.kyubey.akatsuki.utils

import okhttp3.*
import java.io.IOException
import java.util.concurrent.CompletableFuture

object Http {
    val okhttp = OkHttpClient()

    inline fun get(url: HttpUrl, block: Request.Builder.() -> Unit = {}): CompletableFuture<Response> {
        val fut = CompletableFuture<Response>()

        okhttp.newCall(Request.Builder().apply {
            get()
            url(url)

            block()
        }.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                fut.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                fut.complete(response)
            }
        })

        return fut
    }

    inline fun get(url: String, block: Request.Builder.() -> Unit = {}): CompletableFuture<Response> {
        val httpUrl = HttpUrl.parse(url) ?: return CompletableFuture<Response>().apply { completeExceptionally(Exception("Invalid URL")) }

        return get(httpUrl, block)
    }

    inline fun post(url: HttpUrl, body: RequestBody, block: Request.Builder.() -> Unit = {}): CompletableFuture<Response> {
        val fut = CompletableFuture<Response>()

        okhttp.newCall(Request.Builder().apply {
            post(body)
            url(url)

            block()
        }.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                fut.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                fut.complete(response)
            }
        })

        return fut
    }

    inline fun post(url: String, body: RequestBody, block: Request.Builder.() -> Unit = {}): CompletableFuture<Response> {
        val httpUrl = HttpUrl.parse(url) ?: return CompletableFuture<Response>().apply { completeExceptionally(Exception("Invalid URL")) }

        return post(httpUrl, body, block)
    }
}