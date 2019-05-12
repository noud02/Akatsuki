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
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.Http
import net.dv8tion.jda.core.EmbedBuilder
import okhttp3.HttpUrl
import org.json.JSONObject

@Load
@Argument("term", "string")
class Urban : Command() {
    override val desc = "Search on the urban dictionary!"

    override fun run(ctx: Context) {
        Http.get(HttpUrl.Builder().apply {
            scheme("https")
            host("api.urbandictionary.com")
            addPathSegment("v0")
            addPathSegment("define")
            addQueryParameter("term", ctx.args["term"] as String)
        }.build()).thenAccept { res ->
            val json = JSONObject(res.body()!!.string())

            if (json.getString("result_type") == "no_results") {
                return@thenAccept ctx.send(ctx.lang.getString("no_results"))
            }

            val list = json.getJSONArray("list")

            if (list.count() == 0) {
                return@thenAccept ctx.send(ctx.lang.getString("no_results"))
            }

            val item = list.getJSONObject(0)

            val embed = EmbedBuilder().apply {
                setAuthor(item.getString("author"))
                setTitle(item.getString("word"), item.getString("permalink"))
                descriptionBuilder.append(item.getString("definition"))
                descriptionBuilder.append("\n\n${item.getString("example")}")
                setFooter("${item.getInt("thumbs_up")} \uD83D\uDC4D | ${item.getInt("thumbs_down")} \uD83D\uDC4E", null)
            }

            ctx.send(embed.build())
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get definition from urban dictionary", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}