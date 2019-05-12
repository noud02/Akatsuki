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

import me.aurieh.ares.exposed.async.asyncTransaction
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.EventListener
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.db.schema.Logs
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.EmbedBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

@Load
class Snipe : Command() {
    override val desc = "Snipe the latest deleted message."
    override val guildOnly = true

    override fun run(ctx: Context) {
        if (!ctx.storedGuild!!.logs) {
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("logs_not_enabled"),
                            mapOf("username" to ctx.author.name)
                    )
            )
        }

        asyncTransaction(Akatsuki.pool) {
            val snipe: Long? = EventListener.snipes.remove(ctx.channel.idLong)

            if (snipe != null) {
                val log = Logs.select {
                    Logs.guildId.eq(ctx.guild!!.idLong) and Logs.messageId.eq(snipe)
                }.first()

                val embed = EmbedBuilder().apply {
                    setAuthor("${log[Logs.authorName]}#${log[Logs.authorDiscrim]}", null, log[Logs.authorAvatar])
                    val regex = "(https?)?:?(//)?discord(app)?.?(gg|io|me|com)?/(\\w+:?\\w*@)?(\\S+)(:[0-9]+)?(/|/([\\w#!:.?+=&%@!-/]))?".toRegex()
                    descriptionBuilder.append(log[Logs.content].replace(regex, "[INVITE REDACTED]"))
                    setFooter(
                            I18n.parse(
                                    ctx.lang.getString("sniped_by"),
                                    mapOf("user" to "${ctx.author.name}#${ctx.author.discriminator}")
                            ),
                            null
                    )
                }

                ctx.send(embed.build())
            } else {
                ctx.send(ctx.lang.getString("no_snipes"))
            }
        }.execute()
    }
}