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

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.Logger
import net.dv8tion.jda.core.entities.VoiceChannel
import java.util.*
import kotlin.reflect.jvm.jvmName

object MusicManager {
    private val logger = Logger(this::class.jvmName)

    val playerManager = DefaultAudioPlayerManager()
    val musicManagers = mutableMapOf<String, GuildMusicManager>()
    val inactivityScheduler = Timer(true)

    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    fun join(ctx: Context): GuildMusicManager {
        logger.info("New voice connection in guild ${ctx.guild!!.name}!")
        val manager = GuildMusicManager(ctx.event.textChannel, ctx.member!!.voiceState.channel as VoiceChannel)
        musicManagers[ctx.guild.id] = manager
        val link = Akatsuki.lavalink.getLink(ctx.guild)
        link.connect(ctx.member.voiceState?.channel)
        link.changeNode(Akatsuki.lavalink.loadBalancer.determineBestSocket(ctx.guild.idLong))
        logger.info("Connecting to ${link.node?.remoteUri}")

        return manager
    }

    fun leave(guild: String): Boolean {
        logger.info("Voice connection ended in guild with id $guild!")
        val manager = musicManagers[guild] ?: return false
        manager.player.stopTrack()
        manager.scheduler.queue.clear()
        Akatsuki.lavalink.getLink(guild).destroy()
        musicManagers.remove(guild)
        return true
    }
}