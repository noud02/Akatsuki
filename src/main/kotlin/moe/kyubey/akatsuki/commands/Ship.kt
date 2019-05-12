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
import moe.kyubey.akatsuki.annotations.Arguments
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.entities.ThreadedCommand
import moe.kyubey.akatsuki.utils.Http
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.entities.Member
import okhttp3.*
import java.io.File
import java.io.FileOutputStream

@Load
@Arguments(
        Argument("user1", "user"),
        Argument("user2", "user")
)
class Ship : ThreadedCommand() {
    override val desc = "Ship people with eachother"

    override fun threadedRun(ctx: Context) {
        val user1 = ctx.args["user1"] as Member
        val user2 = ctx.args["user2"] as Member

        val first = user1.user.name
        val sec = user2.user.name

        val ship = first.substring(0, Math.floor(first.length / 2.0).toInt()) + sec.substring(Math.floor(sec.length / 2.0).toInt())

        Http.get(user1.user.avatarUrl).thenAccept { res1 ->
            Http.get(user2.user.avatarUrl).thenAccept { res2 ->
                Http.post(HttpUrl.Builder().apply {
                    scheme(if (Akatsuki.config.backend.ssl) "https" else "http")
                    host(Akatsuki.config.backend.host)
                    port(Akatsuki.config.backend.port)
                    addPathSegment("api")
                    addPathSegment("ship")
                }.build(), MultipartBody.Builder().apply {
                    setType(MultipartBody.FORM)
                    addFormDataPart(
                            "user1",
                            "avatar.png",
                            RequestBody.create(MediaType.parse("image/png"), res1.body()!!.bytes())
                    )
                    addFormDataPart(
                            "user2",
                            "avatar2.png",
                            RequestBody.create(MediaType.parse("image/png"), res2.body()!!.bytes())
                    )
                }.build()).thenAccept { res ->
                    ctx.channel
                            .sendMessage(
                                    I18n.parse(
                                            ctx.lang.getString("happy_shipping"),
                                            mapOf("shipname" to ship)
                                    )
                            )
                            .addFile(res.body()!!.bytes(), "ship.png")
                            .queue()
                    res.close()
                }.thenApply {}.exceptionally {
                    ctx.logger.error("Error while trying to generate ship image", it)
                    ctx.sendError(it)
                    Sentry.capture(it)
                }
                res1.close()
                res2.close()
            }
        }
    }
}