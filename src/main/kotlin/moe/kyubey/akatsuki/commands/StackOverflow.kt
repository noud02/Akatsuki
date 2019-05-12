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
@Argument("query", "string")
class StackOverflow : Command() {
    override val desc = "Search for answers on StackOverflow"

    override fun run(ctx: Context) {
        val query = ctx.args["query"] as String

        Http.get(HttpUrl.Builder().apply {
            scheme("https")
            host("api.stackexchange.com")
            addPathSegments("2.2/search/advanced")
            addQueryParameter("order", "asc")
            addQueryParameter("sort", "relevance")
            addQueryParameter("site", "stackoverflow")
            addQueryParameter("q", query)
        }.build()).thenAccept { res ->
            val json = JSONObject(res.body()!!.string())

            val items = json.getJSONArray("items")

            if (items.count() == 0) {
                return@thenAccept ctx.send(ctx.lang.getString("no_results"))
            }

            val item = items.getJSONObject(Math.floor(Math.random() * items.count()).toInt())

            val embed = EmbedBuilder().apply {
                val owner = item.getJSONObject("owner")
                val tags = item.getJSONArray("tags")
                val answered = item.getBoolean("is_answered")

                setTitle(item.getString("title"), item.getString("link"))
                setAuthor(
                        owner.getString("display_name"),
                        owner.getString("link"),
                        owner.getString("profile_image")
                )
                setColor(if (answered) 0x4CAF50 else 0xF44336)

                descriptionBuilder.append("**Tags**: ${tags.joinToString { it.toString() }}\n")

                if (answered && item.has("accepted_answer_id")) {
                    descriptionBuilder.append("\n[Answer](https://stackoverflow.com/a/${item.getInt("accepted_answer_id")})")
                }
            }

            ctx.send(embed.build())

            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get posts from StackOverflow", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}