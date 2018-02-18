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

package me.noud02.akatsuki.db

import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.Akatsuki
import me.noud02.akatsuki.EventListener
import me.noud02.akatsuki.db.schema.Guilds
import me.noud02.akatsuki.db.schema.Logs
import me.noud02.akatsuki.db.schema.Users
import me.noud02.akatsuki.extensions.log
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

data class DBGuild(
        val id: Long,
        val name: String,
        val lang: String,
        val prefixes: List<String>,
        val forceLang: Boolean,
        val starboard: Boolean,
        val starboardChannel: Long?,
        val logs: Boolean,
        val modlogs: Boolean,
        val modlogChannel: Long?,
        val rolemeRoles: Map<String, Long>,
        val welcome: Boolean,
        val welcomeChannel: Long?,
        val welcomeMessage: String,
        val leaveMessage: String,
        val ignoredChannels: List<Long>,
        val levelMessages: Boolean,
        val mutedRole: Long?,
        val antiInvite: Boolean,
        val antiInviteBypassRoles: List<Long>,
        val antiInviteBypassUsers: List<Long>
)

data class DBUser(
        val id: Long,
        val username: String,
        val discriminator: String,
        val lang: String,
        val marriedUserId: Long?
)

data class DBStar(
        val messageId: Long,
        val channelId: Long,
        val guildId: Long,
        val starId: Long,
        val stargazers: List<Long>,
        val content: String,
        val attachments: List<String>
)

data class DBContract(
        val userId: Long,
        val wish: String,
        val date: DateTime
)

object DatabaseWrapper {
    private val pool: ExecutorService = Akatsuki.instance.pool

    fun getGuild(guild: Guild) = getGuild(guild.idLong)

    fun getGuild(id: Long) = asyncTransaction(pool) {
        val guild = Guilds.select { Guilds.id.eq(id) }.firstOrNull()

        if (guild == null)
            throw Exception("Guild not found")
        else
            return@asyncTransaction DBGuild(
                    guild[Guilds.id],
                    guild[Guilds.name],
                    guild[Guilds.lang],
                    guild[Guilds.prefixes].toList(),
                    guild[Guilds.forceLang],
                    guild[Guilds.starboard],
                    guild[Guilds.starboardChannel],
                    guild[Guilds.logs],
                    guild[Guilds.modlogs],
                    guild[Guilds.modlogChannel],
                    guild[Guilds.rolemeRoles],
                    guild[Guilds.welcome],
                    guild[Guilds.welcomeChannel],
                    guild[Guilds.welcomeMessage],
                    guild[Guilds.leaveMessage],
                    guild[Guilds.ignoredChannels].toList(),
                    guild[Guilds.levelMessages],
                    guild[Guilds.mutedRole],
                    guild[Guilds.antiInvite],
                    guild[Guilds.antiInviteBypassRoles].toList(),
                    guild[Guilds.antiInviteBypassUsers].toList()
            )
    }.execute()

    fun newGuild(guild: Guild) = asyncTransaction(pool) {
        val selection = Guilds.select {
            Guilds.id.eq(guild.idLong)
        }

        if (selection.empty())
            Guilds.insert {
                it[id] = guild.idLong
                it[lang] = "en_US"
                it[forceLang] = false
                it[prefixes] = arrayOf()
                it[name] = guild.name
                it[starboard] = false
                it[starboardChannel] = guild.textChannels.firstOrNull { it.name.toLowerCase() == "starboard" }?.idLong
                it[logs] = false
                it[modlogs] = false
                it[modlogChannel] = guild.textChannels.firstOrNull { it.name.toLowerCase() == "modlogs" }?.idLong
                it[rolemeRoles] = mapOf()
                it[welcome] = false
                it[welcomeChannel] = guild.textChannels.firstOrNull { it.name.toLowerCase() == "welcome" }?.idLong
                it[welcomeMessage] = "Welcome %USER% to %SERVER%!"
                it[leaveMessage] = "%USER% \uD83D\uDC4B"
                it[ignoredChannels] = arrayOf()
                it[levelMessages] = false
                it[mutedRole] = guild.roles.firstOrNull { it.name.toLowerCase() == "muted" }?.idLong
                it[antiInvite] = false
                it[antiInviteBypassRoles] = arrayOf()
                it[antiInviteBypassUsers] = arrayOf()
            }
    }.execute()

    fun remGuild(guild: Guild) = remGuild(guild.idLong)

    fun remGuild(id: Long) = asyncTransaction(pool) {
        Guilds.deleteWhere {
            Guilds.id.eq(id)
        }
    }.execute()

    fun getUser(user: User) = getUser(user.idLong)

    fun getUser(member: Member) = getUser(member.user.idLong)

    fun getUser(id: Long) = asyncTransaction(pool) {
        val user = Users.select { Users.id.eq(id) }.firstOrNull()

        if (user == null)
            throw Exception("User not found")
        else
            return@asyncTransaction DBUser(
                    user[Users.id],
                    user[Users.username],
                    user[Users.discriminator],
                    user[Users.lang],
                    user[Users.marriedUserId]
            )
    }.execute()

    fun newUser(member: Member) = newUser(member.user)

    fun newUser(user: User) = asyncTransaction(pool) {
        val selection = Users.select {
            Users.id.eq(user.idLong)
        }

        if (selection.empty())
            Users.insert {
                it[id] = user.idLong
                it[username] = user.name
                it[discriminator] = user.discriminator
                it[lang] = "en_US"
                it[marriedUserId] = null
            }
    }.execute()

    fun getGuildSafe(guild: Guild): CompletableFuture<DBGuild> {
        val fut = CompletableFuture<DBGuild>()

        getGuild(guild)
                .thenAccept { fut.complete(it) }
                .thenApply {}
                .exceptionally {
                    newGuild(guild)
                            .thenAccept {
                                getGuild(guild)
                                        .thenAccept {
                                            fut.complete(it)
                                        }
                            }
                }

        return fut
    }

    fun getUserSafe(member: Member) = getUserSafe(member.user)

    fun getUserSafe(user: User): CompletableFuture<DBUser> {
        val fut = CompletableFuture<DBUser>()

        getUser(user)
                .thenAccept { fut.complete(it) }
                .thenApply {}
                .exceptionally {
                    newUser(user)
                            .thenAccept {
                                getUser(user)
                                        .thenAccept {
                                            fut.complete(it)
                                        }
                            }
                }

        return fut
    }

    /*fun getUserSafe(user: User): DBUser = try {
        getUser(user)
    } catch (e: Exception) {
        newUser(user)
        getUser(user)
    }*/

    fun logEvent(event: GenericMessageEvent) = asyncTransaction(pool) {
        when (event) {
            is MessageDeleteEvent -> {
                val log = Logs.select {
                    Logs.messageId.eq(event.messageIdLong)
                }.firstOrNull()

                if (log != null) {
                    EventListener.instance.snipes[event.channel.idLong] = log[Logs.messageId]

                    Logs.insert {
                        it[Logs.event] = "DELETE"
                        it[messageId] = log[Logs.messageId]
                        it[content] = log[Logs.content]
                        it[attachments] = log[Logs.attachments]
                        it[embeds] = log[Logs.embeds]
                        it[timestamp] = log[Logs.timestamp]
                        it[authorId] = log[Logs.authorId]
                        it[authorName] = log[Logs.authorName]
                        it[authorDiscrim] = log[Logs.authorDiscrim]
                        it[authorAvatar] = log[Logs.authorAvatar]
                        it[authorNick] = log[Logs.authorNick]
                        it[guildId] = log[Logs.guildId]
                        it[guildName] = log[Logs.guildName]
                        it[channelId] = log[Logs.channelId]
                        it[channelName] = log[Logs.channelName]
                    }
                }
            }
            is MessageReceivedEvent -> event.message.log()
            is MessageUpdateEvent -> event.message.log("UPDATE")
            else -> throw Exception("Not a valid event to log")
        }
    }.execute().get()
}