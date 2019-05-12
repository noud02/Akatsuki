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

package moe.kyubey.akatsuki.commands

import io.sentry.Sentry
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.entities.ImageCommand
import moe.kyubey.akatsuki.utils.Http
import okhttp3.*
import java.io.File

@Load
@Argument("image", "url", true)
class Waaw : ImageCommand() {
    override val desc = "Waaw images."

    override fun imageRun(ctx: Context, file: ByteArray) {
        val body = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            addFormDataPart(
                    "image",
                    "image",
                    RequestBody.create(MediaType.parse("image/png"), file)
            )
        }.build()

        Http.post(HttpUrl.Builder().apply {
            scheme(if (Akatsuki.config.backend.ssl) "https" else "http")
            host(Akatsuki.config.backend.host)
            port(Akatsuki.config.backend.port)
            addPathSegment("api")
            addPathSegment("waaw")
        }.build(), body).thenAccept { res ->
            ctx.channel.sendFile(res.body()!!.bytes(), "waaw.png", null).queue()
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to generate waaw'd image", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}