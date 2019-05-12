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

package moe.kyubey.akatsuki.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import lavalink.client.player.IPlayer
import lavalink.client.player.event.PlayerEventListenerAdapter
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.utils.Http
import net.dv8tion.jda.core.EmbedBuilder
import org.json.JSONObject
import java.awt.Color
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.timerTask

class TrackScheduler(private val player: IPlayer, private val manager: GuildMusicManager) : PlayerEventListenerAdapter() {
    val queue = LinkedBlockingQueue<AudioTrack>()

    fun add(track: AudioTrack) {
        if (player.playingTrack == null) {
            player.playTrack(track)
        } else {
            queue.offer(track)
        }
    }

    fun next() {
        if (queue.peek() != null) {
            player.playTrack(queue.poll())
        }
    }

    fun shuffle() {
        val tracks = queue.shuffled()

        queue.clear()

        queue += tracks
    }

    override fun onTrackStart(player: IPlayer, track: AudioTrack) {
        val nextTrack = queue.peek()
        val embed = EmbedBuilder()

        embed.setTitle("Now playing: ${track.info.title}")
        embed.setColor(Color.CYAN)

        if (nextTrack != null) {
            embed.setFooter("Next: ${nextTrack.info.title}", null)
        }

        manager.textChannel.sendMessage(embed.build()).queue()
    }

    override fun onTrackEnd(player: IPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            val nextTrack = queue.peek()
            val embed = EmbedBuilder()

            embed.setTitle("Track finished: ${track.info.title}")
            embed.setColor(Color.CYAN)

            if (nextTrack != null) {
                embed.setFooter("Next: ${nextTrack.info.title}", null)
            }

            if (manager.autoplay && track.info.uri.indexOf("youtube") > -1) {
                val qs = "?key=${Akatsuki.config.api.google}&part=snippet&maxResults=10&type=video&relatedToVideoId=${track.info.identifier}"

                Http.get("https://www.googleapis.com/youtube/v3/search$qs").thenAccept { res ->
                    val id = JSONObject(res.body()!!.string())
                            .getJSONArray("items")
                            .getJSONObject(0)
                            .getJSONObject("id")
                            .getString("videoId")

                    MusicManager.playerManager.loadItem("https://youtube.com/watch?v=$id", object : AudioLoadResultHandler {
                        override fun loadFailed(exception: FriendlyException)
                                = manager.textChannel.sendMessage("[autoplay] Failed to add song to queue: ${exception.message}").queue()
                        override fun noMatches() = manager.textChannel.sendMessage("[autoplay] YouTube url is (probably) invalid!").queue()
                        override fun trackLoaded(track: AudioTrack) = manager.scheduler.add(track)
                        override fun playlistLoaded(playlist: AudioPlaylist) = trackLoaded(playlist.tracks.first())
                    })
                }
            } else {
                MusicManager.inactivityScheduler.schedule(timerTask {
                    if (player.playingTrack != null || !manager.textChannel.guild.audioManager.isConnected) {
                        return@timerTask
                    }

                    manager.textChannel.sendMessage("Left voicechannel because of inactivity").queue()
                    MusicManager.leave(manager.textChannel.guild.id)
                }, 300000L)
            }
            manager.textChannel.sendMessage(embed.build()).queue()
            next()
        }
    }

    override fun onTrackException(player: IPlayer, track: AudioTrack, exception: Exception)
            = manager.textChannel.sendMessage("Error occurred while playing music: ${exception.message}").queue()
}