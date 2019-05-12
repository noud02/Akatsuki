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
import moe.kyubey.akatsuki.EventListener
import moe.kyubey.akatsuki.annotations.Alias
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.*
import moe.kyubey.akatsuki.utils.Http
import moe.kyubey.akatsuki.utils.I18n
import moe.kyubey.akatsuki.utils.ItemPicker
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import okhttp3.HttpUrl
import org.json.JSONObject
import java.awt.Color

@Load
@Argument("query", "string")
@Alias("yt")
class Youtube : Command() {
    override val desc = "Search for videos on YouTube"
    override val guildOnly = true

    override fun run(ctx: Context) {
        val picker = ItemPicker(EventListener.waiter, ctx.member as Member, ctx.guild as Guild)
        val search = ctx.args["query"] as String

        Http.get(HttpUrl.Builder().apply {
            scheme("https")
            host("www.googleapis.com")
            addPathSegment("youtube")
            addPathSegment("v3")
            addPathSegment("search")
            addQueryParameter("key", Akatsuki.config.api.google)
            addQueryParameter("part", "snippet")
            addQueryParameter("maxResults", "10")
            addQueryParameter("type", "video")
            addQueryParameter("q", search)
        }.build()).thenAccept { res ->
            val body = res.body()!!.string()

            val obj = JSONObject(body)

            if (!obj.has("items")) {
                return@thenAccept ctx.send(
                        I18n.parse(
                                ctx.lang.getString("video_search_fail"),
                                mapOf(
                                        "user" to ctx.author.name,
                                        "search" to search
                                )
                        )
                )
            }

            val items = obj.getJSONArray("items")

            if (items.length() == 0) {
                return@thenAccept ctx.send(
                        I18n.parse(
                                ctx.lang.getString("video_search_fail"),
                                mapOf(
                                        "user" to ctx.author.name,
                                        "search" to search
                                )
                        )
                )
            }

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)

                val id = item
                        .getJSONObject("id")
                        .getString("videoId")

                val snippet = item.getJSONObject("snippet")

                val title = snippet.getString("title")
                val thumb = snippet
                        .getJSONObject("thumbnails")
                        .getJSONObject("medium")
                        .getString("url")

                val desc = snippet.getString("description")
                val channel = snippet.getString("channelTitle")

                picker.addItem(PickerItem(id, title, desc, channel, thumb, url = "https://youtu.be/$id"))
            }

            picker.color = Color(255, 0, 0)

            picker.build(ctx.channel)
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get info from YouTube", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}