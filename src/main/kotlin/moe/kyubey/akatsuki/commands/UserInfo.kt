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

import moe.kyubey.akatsuki.annotations.Alias
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Member
import java.time.format.DateTimeFormatter

@Load
@Alias("user")
@Argument("user", "user", true)
class UserInfo : Command() {
    override val desc = "Get info on a user."
    override val guildOnly = true

    override fun run(ctx: Context) {
        val member = ctx.args["user"] as? Member ?: ctx.member!!
        val embed = EmbedBuilder().apply {
            val statusEmote = if (member.game != null && member.game.type == Game.GameType.STREAMING) {
                "<:streaming:313956277132853248>"
            } else {
                when (member.onlineStatus.name) {
                    "ONLINE" -> "<:online:313956277808005120>"
                    "OFFLINE" -> "<:offline:313956277237710868>"
                    "IDLE" -> "<:away:313956277220802560>"
                    else -> "<:invisible:313956277107556352>"
                }
            }

            setTitle("$statusEmote ${member.user.name}#${member.user.discriminator}${if (!member.nickname.isNullOrEmpty()) " (${member.nickname})" else ""}")

            setThumbnail(member.user.avatarUrl)

            // TODO add translations for these
            descriptionBuilder.append("**ID:** ${member.user.id}\n")
            descriptionBuilder.append("**Highest role:** ${member.roles.sortedBy { it.position }.last()?.name ?: "none"}\n")
            descriptionBuilder.append("**Playing:** ${member.game?.name ?: "nothing"}")

            setFooter(
                    "Joined Discord on ${
                    member.user.creationTime.format(DateTimeFormatter.RFC_1123_DATE_TIME)
                    }, ${ctx.guild!!.name} on ${
                    member.joinDate.format(DateTimeFormatter.RFC_1123_DATE_TIME)
                    }",
                    null
            )
        }
        
        ctx.send(embed.build())
    }
}