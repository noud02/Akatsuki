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

import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.music.MusicManager
import net.dv8tion.jda.core.EmbedBuilder
import java.lang.management.ManagementFactory
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.*

@Load
class Stats : Command() {
    override val desc = "Get stats of the bot."

    override fun run(ctx: Context) {
        val rb = ManagementFactory.getRuntimeMXBean()
        val rtime = Runtime.getRuntime()

        val embed = EmbedBuilder().apply {
            if (ctx.jda.shardInfo != null) {
                setTitle("Shard [${ctx.jda.shardInfo.shardId + 1} / ${ctx.jda.shardInfo.shardTotal}]")
            }

            val millis = rb.uptime
            val secs = (millis / 1000) % 60
            val mins = (millis / 60000) % 60
            val hours = (millis / 3600000) % 24

            val uptime = "%02d:%02d:%02d".format(hours, mins, secs)

            descriptionBuilder.append("**Uptime:** $uptime\n")
            descriptionBuilder.append("**Memory Usage:** ${rtime.totalMemory() / (1024 * 1024)}MB\n")

            if (Akatsuki.jda != null) {
                descriptionBuilder.append("**Guilds:** ${ctx.jda.guilds.size}\n")
                descriptionBuilder.append("**Users:** ${ctx.jda.users.size}\n")
                descriptionBuilder.append("**Ping:** `${ctx.jda.ping}ms`\n")
                descriptionBuilder.append("**Voice connections:** ${MusicManager.musicManagers.size}\n")
            } else {
                descriptionBuilder.append("**Total Guilds:** ${Akatsuki.shardManager.guilds.size}\n")
                descriptionBuilder.append("**Total Users:** ${Akatsuki.shardManager.users.size}\n")
                descriptionBuilder.append("**Total Voice Connections:** ${MusicManager.musicManagers.size}\n")
                descriptionBuilder.append("**Average Ping:** `${Akatsuki.shardManager.averagePing}ms`\n")

                for (shard in Akatsuki.shardManager.shards.sortedBy { it.shardInfo.shardId }) {
                    val voiceConns = MusicManager.musicManagers.filter { shard.guilds.any { g -> g.id == it.key } }.size

                    addField(
                            "Shard ${shard.shardInfo.shardId + 1}",
                            "**Guilds:** ${shard.guilds.size}\n" +
                                    "**Users:** ${shard.users.size}\n" +
                                    "**Voice Connections:** $voiceConns\n" +
                                    "**Ping:** `${shard.ping}ms`",
                            true
                    )
                }
            }
        }

        ctx.send(embed.build())
    }
}