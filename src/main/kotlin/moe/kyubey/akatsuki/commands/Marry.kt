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
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.db.DatabaseWrapper
import moe.kyubey.akatsuki.db.schema.Users
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import org.jetbrains.exposed.sql.update

@Load
@Argument("user", "user")
class Marry : Command() {
    override val guildOnly = true
    override val desc = "Marry people."

    override fun run(ctx: Context) {
        val member = ctx.args["user"] as Member

        if (ctx.storedUser.marriedUserId != null) {
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("already_married"),
                            mapOf(
                                    "username" to ctx.author.name,
                                    "user" to (ctx.jda.getUserById(ctx.storedUser.marriedUserId)?.name
                                            ?: "an unknown person")
                            )
                    )
            )
        }

        if (member.user.id == ctx.author.id) {
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("cant_marry_self"),
                            mapOf("username" to ctx.author.name)
                    )
            )
        }

        if (member.user.isBot) {
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("cant_marry_bot"),
                            mapOf("username" to ctx.author.name)
                    )
            )
        }

        val dbUser = DatabaseWrapper.getUserSafe(member.user).get()

        if (dbUser.marriedUserId != null) {
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("user_already_married"),
                            mapOf(
                                    "username" to ctx.author.name,
                                    "user" to member.user.name,
                                    "user2" to (ctx.jda.getUserById(dbUser.marriedUserId)?.name ?: "an unknown person")
                            )
                    )
            )
        }

        val yesEmote = "\u2705"
        val noEmote = "\u274E"

        ctx.channel.sendMessage(
                I18n.parse(
                        ctx.lang.getString("propose_marriage"),
                        mapOf(
                                "user" to member.asMention,
                                "username" to ctx.author.name
                        )
                )
        ).queue { msg ->
            msg.addReaction(yesEmote).queue()
            msg.addReaction(noEmote).queue()

            EventListener.waiter.await<MessageReactionAddEvent>(1, 60000L) {
                if (it.user.id == member.user.id && it.messageId == msg.id) {
                    when (it.reactionEmote.name) {
                        yesEmote -> {
                            asyncTransaction(Akatsuki.pool) {
                                Users.update({
                                    Users.id.eq(ctx.author.idLong)
                                }) {
                                    it[marriedUserId] = member.user.idLong
                                }

                                Users.update({
                                    Users.id.eq(member.user.idLong)
                                }) {
                                    it[marriedUserId] = ctx.author.idLong
                                }

                                ctx.send(
                                        I18n.parse(
                                                ctx.lang.getString("marriage_accepted"),
                                                mapOf(
                                                        "username" to ctx.author.name,
                                                        "user" to member.user.name
                                                )
                                        )
                                )
                            }.execute()
                        }

                        noEmote -> {
                            ctx.send(
                                    I18n.parse(
                                            ctx.lang.getString("marriage_declined"),
                                            mapOf("username" to member.user.name)
                                    )
                            )
                        }

                        else -> return@await false
                    }

                    true
                } else {
                    false
                }
            }
        }
    }
}