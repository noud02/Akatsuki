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
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.Http
import net.dv8tion.jda.core.entities.Member
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject

@Load
@Argument("user", "user")
class ShitWaifu : Command() {
    override val desc = "Your waifu is shit lol"

    override fun run(ctx: Context) {
        val member = ctx.args["user"] as Member

        Http.post(
                "https://api.weeb.sh/auto-image/waifu-insult",
                RequestBody.create(MediaType.parse("application/json"), JSONObject(mapOf("avatar" to member.user.avatarUrl)).toString())
        ) {
            addHeader("Authorization", "Wolke ${Akatsuki.config.api.weebsh}")
        }.thenAccept { res ->
            ctx.channel.sendFile(res.body()!!.bytes(), "shitwaifu.png").queue()
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to generate shitwaifu image", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}