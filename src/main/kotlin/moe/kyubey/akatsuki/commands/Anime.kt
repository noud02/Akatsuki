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

@file:Suppress("DEPRECATION")

package moe.kyubey.akatsuki.commands

import io.sentry.Sentry
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.Http
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.EmbedBuilder
import okhttp3.*
import org.apache.commons.lang3.StringEscapeUtils
import org.json.XML
import java.awt.Color

@Load
@Argument("anime", "string")
class Anime : Command() {
    override val desc = "Search for anime on MyAnimeList"

    override fun run(ctx: Context) {
        Http.get(HttpUrl.Builder().apply {
            scheme("https")
            host("myanimelist.net")
            addPathSegments("api/anime/search.xml")
            addQueryParameter("q", ctx.args["anime"] as String)
        }.build()) {
            val mal = Akatsuki.config.api.myanimelist
            val cred = Credentials.basic(mal.split(":")[0], mal.split(":")[1])

            addHeader("Authorization", cred)
        }.thenAccept { res ->
            val obj = XML
                    .toJSONObject(res.body()!!.string())

            if (!obj.has("anime")) {
                return@thenAccept ctx.send(
                        I18n.parse(
                                ctx.lang.getString("anime_not_found"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            val json = obj.getJSONObject("anime")

            if (!json.has("entry")) {
                return@thenAccept ctx.send(
                        I18n.parse(
                                ctx.lang.getString("anime_not_found"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            val entry = json.optJSONObject("entry")
                    ?: json.getJSONArray("entry").optJSONObject(0)
                    ?: return@thenAccept ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("anime_not_found"),
                                    mapOf("username"  to ctx.author.name)
                            )
                    )

            val embed = EmbedBuilder().apply {
                setTitle(entry.getString("title"))

                val score = entry.getDouble("score")
                val episodes = entry.getInt("episodes")
                val type = when (entry.getString("type").toLowerCase()) {
                    "tv" -> "\uD83D\uDCFA"

                    "movie" -> "\uD83C\uDF7F"
                    else -> "?"
                }
                val status = entry.getString("status")
                val startDateRaw = entry.getString("start_date")
                val startDate = if (startDateRaw != "0000-00-00") startDateRaw else "unknown"
                val endDateRaw = entry.getString("end_date")
                val endDate = if (endDateRaw != "0000-00-00") endDateRaw else "unknown"

                descriptionBuilder.append("$score \u2606 | $episodes $type $status | $startDate -> $endDate")

                addField(
                        "Synopsis",
                        StringEscapeUtils
                                .unescapeHtml4(entry.getString("synopsis"))
                                .replace("<br />", "\n"),
                        false
                )
                setColor(Color(46, 81, 162))
                setImage(entry.getString("image"))
            }

            ctx.send(embed.build())
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get anime info", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}