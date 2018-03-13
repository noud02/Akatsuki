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

package me.noud02.akatsuki.extensions

import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.Akatsuki
import me.noud02.akatsuki.db.DatabaseWrapper
import me.noud02.akatsuki.db.schema.Starboard
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.awt.Color

fun Guild.searchMembers(query: String): List<Member> = members.filter {
    "<@!${it.user.idLong}> ${it.asMention} ${it.effectiveName.toLowerCase()} ${it.user.name.toLowerCase()}#${it.user.discriminator} ${it.user.id}".indexOf(query.toLowerCase()) > -1
}

fun Guild.searchTextChannels(query: String): List<TextChannel> = textChannels.filter {
    "${it.asMention} ${it.name.toLowerCase()} ${it.id}".indexOf(query.toLowerCase()) > -1
}

fun Guild.searchRoles(query: String): List<Role> = roles.filter {
    "${it.asMention} ${it.name.toLowerCase()} ${it.id}".indexOf(query.toLowerCase()) > -1
}

fun getStarColor(stars: Int): Color {
    var c = stars / 13

    if (c > 1) {
        c = 1
    }

    return Color(255, (194 * c) + (253 * (1 - c)), (12 * c) + (247 * (1 - c)))
}

fun Guild.addStar(msg: Message, user: User) {
    if (msg.author.id == user.id) {
        return
    }

    DatabaseWrapper.getGuildSafe(this).thenAccept { guild ->
        val channel = getTextChannelById(guild.starboardChannel ?: return@thenAccept) ?: return@thenAccept

        asyncTransaction(Akatsuki.pool) {
            val stars = Starboard.select {
                Starboard.guildId.eq(idLong)
            }

            val star = stars.firstOrNull {
                it[Starboard.messageId] == msg.idLong
            }

            val embed = EmbedBuilder()

            if (star != null) {
                if (star[Starboard.stargazers].contains(user.idLong)) {
                    return@asyncTransaction
                }

                embed.apply {
                    setAuthor(msg.author.name, null, msg.author.avatarUrl)
                    setColor(getStarColor(star[Starboard.stargazers].size + 1))

                    if (msg.attachments.isNotEmpty()) {
                        setImage(msg.attachments.first().url)
                    }

                    descriptionBuilder.append(msg.contentRaw)
                }

                channel
                        .getMessageById(star[Starboard.starId])
                        .queue({
                            it
                                    .editMessage(embed.build())
                                    .content("\u2b50 **${star[Starboard.stargazers].size + 1}** <#${msg.channel.id}> ID: ${msg.id}")
                                    .queue()
                        })


                Starboard.update({
                    Starboard.messageId.eq(msg.idLong)
                }) {
                    it[stargazers] = star[Starboard.stargazers] + user.idLong
                }
            } else {
                embed.apply {
                    setAuthor(msg.author.name, null, msg.author.avatarUrl)
                    setColor(getStarColor(1))

                    if (msg.attachments.isNotEmpty()) {
                        setImage(msg.attachments.first().url)
                    }

                    descriptionBuilder.append(msg.contentRaw)
                }

                channel
                        .sendMessage(embed.build())
                        .content("\u2b50 <#${msg.channel.id}> ID: ${msg.id}")
                        .queue { starMsg ->
                            asyncTransaction(Akatsuki.pool) {
                                Starboard.insert {
                                    it[messageId] = msg.idLong
                                    it[guildId] = idLong
                                    it[channelId] = msg.channel.idLong
                                    it[starId] = starMsg.idLong
                                    it[stargazers] = arrayOf(user.idLong)
                                    it[content] = msg.contentRaw
                                    it[attachments] = msg.attachments.map { it.url }.toTypedArray()
                                }
                            }.execute()
                        }
            }

            return@asyncTransaction
        }.execute()
    }
}

fun Guild.removeStar(msg: Message, user: User) {
    DatabaseWrapper.getGuildSafe(this).thenAccept { guild ->
        val channel = getTextChannelById(guild.starboardChannel ?: return@thenAccept) ?: return@thenAccept

        asyncTransaction(Akatsuki.pool) {
            val stars = Starboard.select {
                Starboard.guildId.eq(idLong)
            }

            val star = stars.firstOrNull {
                it[Starboard.messageId] == msg.idLong
            }

            if (star != null && star[Starboard.stargazers].contains(user.idLong)) {
                val gazers = star[Starboard.stargazers].size - 1

                val embed = EmbedBuilder().apply {
                    setAuthor(msg.author.name, null, msg.author.avatarUrl)
                    setColor(getStarColor(gazers))
                    setDescription(descriptionBuilder.append(msg.contentRaw))
                }

                if (gazers == 0) {
                    channel
                            .getMessageById(star[Starboard.starId])
                            .queue({
                                it.delete().queue()
                            })

                    Starboard.deleteWhere {
                        Starboard.messageId.eq(msg.idLong)
                    }
                } else {
                    channel
                            .getMessageById(star[Starboard.starId])
                            .queue({
                                it
                                        .editMessage(embed.build())
                                        .content("\u2b50 ${if (gazers == 1) "" else "**$gazers**"} <#${msg.channel.id}> ID: ${msg.id}")
                                        .queue()
                            })

                    val gazerIds = star[Starboard.stargazers]

                    Starboard.update({
                        Starboard.messageId.eq(msg.idLong)
                    }) {
                        it[stargazers] = gazerIds.drop(gazerIds.indexOf(user.idLong)).toTypedArray()
                    }
                }
            }
        }.execute()
    }
}