/*
 *   Copyright (c) 2017-2018 Noud Kerver
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

package me.noud02.akatsuki.commands

import me.noud02.akatsuki.Akatsuki
import me.noud02.akatsuki.annotations.Alias
import me.noud02.akatsuki.entities.Context
import me.noud02.akatsuki.annotations.Load
import me.noud02.akatsuki.entities.ThreadedCommand
import me.noud02.akatsuki.music.MusicManager
import me.noud02.akatsuki.utils.Http
import me.noud02.akatsuki.utils.I18n
import net.dv8tion.jda.core.EmbedBuilder
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.*

@Load
@Alias("np")
class NowPlaying : ThreadedCommand() {
    override val desc = "Get the current song"
    override val guildOnly = true

    override fun threadedRun(ctx: Context) {
        val manager = MusicManager.musicManagers[ctx.guild?.id]
                ?: return ctx.send(
                I18n.parse(
                        ctx.lang.getString("not_connected"),
                        mapOf("username" to ctx.author.name)
                )
        )

        if (manager.player.playingTrack == null) {
            return ctx.send(ctx.lang.getString("not_playing"))
        }

        val embed = EmbedBuilder().apply {
            setAuthor(ctx.lang.getString("now_playing"), null, null)
            setTitle(manager.player.playingTrack.info.title)

            val durMillis = manager.player.playingTrack.duration
            val durSecs = (durMillis / 1000) % 60
            val durMins = (durMillis / 60000) % 60
            val durHours = (durMillis / 3600000) % 24

            val duration = "%02d:%02d:%02d".format(durHours, durMins, durSecs)

            val posMillis = manager.player.trackPosition
            val posSecs = (posMillis / 1000) % 60
            val posMins = (posMillis / 60000) % 60
            val posHours = (posMillis / 3600000) % 24

            val position = "%02d:%02d:%02d".format(posHours, posMins, posSecs)

            val emote = if (manager.player.isPaused) "\u23F8" else "\u25B6"

            descriptionBuilder.append("$position/$duration $emote")
            setColor(Color.CYAN)
        }

        if (manager.scheduler.queue.isNotEmpty()) {
            embed.setFooter(
                    I18n.parse(
                            ctx.lang.getString("next"),
                            mapOf("song" to manager.scheduler.queue.peek().info.title)
                    ),
                    null
            )
        } else if (manager.autoplay && manager.player.playingTrack.info.uri.indexOf("youtube") > -1) {
            val res = Http.okhttp.newCall(Request.Builder().apply { // TODO change this to use Http.get
                url(HttpUrl.Builder().apply {
                    scheme("https")
                    host("www.googleapis.com")
                    addPathSegment("youtube")
                    addPathSegment("v3")
                    addPathSegment("search")
                    addQueryParameter("key", Akatsuki.config.api.google)
                    addQueryParameter("part", "snippet")
                    addQueryParameter("maxResults", "10")
                    addQueryParameter("type", "video")
                    addQueryParameter("relatedToVideoId", manager.player.playingTrack.info.identifier)
                }.build())
            }.build()).execute()

            val title = JSONObject(res.body()!!.string())
                    .getJSONArray("items")
                    .getJSONObject(0)
                    .getJSONObject("snippet")
                    .getString("title")

            embed.setFooter(
                    I18n.parse(
                            ctx.lang.getString("next"),
                            mapOf("song" to "$title (autoplay)")
                    ),
                    null
            )
            res.close()
        }

        ctx.send(embed.build())
    }
}