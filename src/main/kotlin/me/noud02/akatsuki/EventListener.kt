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

package me.noud02.akatsuki

import gnu.trove.map.hash.TLongLongHashMap
import io.sentry.Sentry
import me.aurieh.ares.core.entities.EventWaiter
import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.db.DatabaseWrapper
import me.noud02.akatsuki.db.schema.*
import me.noud02.akatsuki.extensions.UTF8Control
import me.noud02.akatsuki.extensions.addStar
import me.noud02.akatsuki.extensions.log
import me.noud02.akatsuki.extensions.removeStar
import me.noud02.akatsuki.music.MusicManager
import me.noud02.akatsuki.utils.Http
import me.noud02.akatsuki.utils.I18n
import me.noud02.akatsuki.utils.Logger
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import okhttp3.MediaType
import okhttp3.RequestBody
import org.jetbrains.exposed.sql.*
import org.json.JSONObject
import java.awt.Color
import java.util.*
import java.util.Date
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.reflect.jvm.jvmName

class EventListener : ListenerAdapter() {
    private val timer = Timer(true)

    override fun onGenericEvent(event: Event) = waiter.emit(event)

    override fun onReady(event: ReadyEvent) {
        logger.info("Ready!")

        if (Akatsuki.jda == null) {
            if (Akatsuki.shardManager.shards.all { it.status == JDA.Status.CONNECTED || it.status == JDA.Status.LOADING_SUBSYSTEMS }) {
                startPresenceTimer()
                // startReminderChecker()
                updateStats()
            }
        } else {
            startPresenceTimer()
            updateStats()
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.guild != null) {
            DatabaseWrapper.getGuildSafe(event.guild).thenAccept { stored ->
                if (stored.logs) {
                    event.message.log()
                }

                if (event.author.isBot) {
                    return@thenAccept
                }

                DatabaseWrapper.getUserSafe(event.member).thenAccept { user ->
                    try {
                        cmdHandler.handleMessage(event, user, stored)
                    } catch (e: Exception) {
                        logger.error("Error while trying to handle message", e)
                        Sentry.capture(e)
                    }

                    val locale = Locale(user.lang.split("_")[0], user.lang.split("_")[1])
                    val bundle = ResourceBundle.getBundle("i18n.Kyubey", locale, UTF8Control())

                    if (stored.antiInvite) {
                        val regex = "(https?)?:?(//)?discord(app)?.?(gg|io|me|com)?/(\\w+:?\\w*@)?(\\S+)(:[0-9]+)?(/|/([\\w#!:.?+=&%@!-/]))?".toRegex()

                        if (event.member.roles.isEmpty() && regex.containsMatchIn(event.message.contentRaw)) {
                            event.message.delete().queue({
                                event.channel.sendMessage(
                                        I18n.parse(
                                                bundle.getString("no_ads"),
                                                mapOf("user" to event.author.asMention)
                                        )
                                ).queue()
                            }) {
                                event.channel.sendMessage(
                                        I18n.parse(
                                                bundle.getString("error"),
                                                mapOf("error" to it)
                                        )
                                ).queue()
                                logger.error("Error while trying to delete ad", it)
                                Sentry.capture(it)
                            }
                        }
                    }

                    asyncTransaction(Akatsuki.pool) {
                        val contract = Contracts.select { Contracts.userId.eq(event.author.idLong) }.firstOrNull() ?: return@asyncTransaction
                        val curLevel = contract[Contracts.level]
                        val xp = contract[Contracts.experience]

                        val xpNeeded = curLevel.toFloat() * 500f * (curLevel.toFloat() / 3f)

                        if (xp >= xpNeeded) {
                            Contracts.update({
                                Contracts.userId.eq(event.author.idLong)
                            }) {
                                it[level] = curLevel + 1
                                it[experience] = 0
                                it[balance] = contract[Contracts.balance] + 2500
                            }

                            if (stored.levelMessages) {
                                event.channel.sendMessage(EmbedBuilder().apply {
                                    setTitle("${event.author.name}, you are now rank ${curLevel + 1}!") // TODO translation
                                    setColor(Color.CYAN)
                                    descriptionBuilder.append("+2500$\n")

                                    // TODO add random items on levelup
                                }.build()).queue()
                            }
                        }
                    }.execute().exceptionally {
                        logger.error("Error while trying to levelup user ${event.author.name}#${event.author.discriminator} (${event.author.id}", it)
                        Sentry.capture(it)
                    }
                }
            }
        } else {
            if (event.author.isBot) {
                return
            }

            DatabaseWrapper.getUserSafe(event.author).thenAccept { user ->
                try {
                    cmdHandler.handleMessage(event, user)
                } catch (e: Exception) {
                    logger.error("Error while trying to handle message", e)
                    Sentry.capture(e)
                }
            }
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (event.guild != null) {
            DatabaseWrapper.getGuildSafe(event.guild).thenAccept { guild ->
                if (guild.logs) {
                    DatabaseWrapper.logEvent(event)
                }
            }
        }
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (event.guild != null) {
            DatabaseWrapper.getGuildSafe(event.guild).thenAccept { guild ->
                if (guild.logs) {
                    event.message.log("UPDATE")
                }
            }
        }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        logger.info("New guild: ${event.guild.name} (${event.guild.id})")

        updateStats()
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        logger.info("Left guild: ${event.guild.name} (${event.guild.id}")

        asyncTransaction(Akatsuki.pool) {
            Roles.deleteWhere { Roles.guildId.eq(event.guild.idLong) }
            Modlogs.deleteWhere { Modlogs.guildId.eq(event.guild.idLong) }
            Restrictions.deleteWhere { Restrictions.guildId.eq(event.guild.idLong) }
            Scripts.deleteWhere { Scripts.guildId.eq(event.guild.idLong) }
            Starboard.deleteWhere { Starboard.guildId.eq(event.guild.idLong) }
            Guilds.deleteWhere { Guilds.id.eq(event.guild.idLong) }
        }.execute().thenApply {}.exceptionally {
            logger.error("Error while trying to remove database entries for guild with id ${event.guild.id}", it)
            Sentry.capture(it)
        }

        updateStats()
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.guild != null && event.reaction.reactionEmote.name == "\u2b50") {
            DatabaseWrapper.getGuildSafe(event.guild).thenAccept { guild ->
                if (!guild.starboard) {
                    return@thenAccept
                }

                event.channel.getMessageById(event.messageId).queue { msg ->
                    event.guild.addStar(msg, event.user)
                }
            }
        }
    }

    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        if (event.guild != null && event.reaction.reactionEmote.name == "\u2b50") {
            DatabaseWrapper.getGuildSafe(event.guild).thenAccept { guild ->
                if (!guild.starboard) {
                    return@thenAccept
                }

                event.channel.getMessageById(event.messageId).queue { msg ->
                    event.guild.removeStar(msg, event.user)
                }
            }
        }
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (event.member.user.id == event.jda.selfUser.id
                || !event.channelLeft.members.any { it.user.id == event.jda.selfUser.id }
                || event.channelLeft.members.size > 1) {
            return
        }

        MusicManager.leave(event.guild.id)
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        asyncTransaction(Akatsuki.pool) {
            val roles = Roles.select {
                Roles.userId.eq(event.user.idLong) and Roles.guildId.eq(event.guild.idLong)
            }

            for (role in roles) {
                val id = role[Roles.roleId]
                val r = event.guild.getRoleById(id)

                if (r != null) {
                    event.guild.controller.addSingleRoleToMember(event.member, r).queue()
                }
            }
        }.execute().thenApply {}.exceptionally {
            it.printStackTrace()
        }

        DatabaseWrapper.getGuildSafe(event.guild).thenAccept { storedGuild ->
            val channel = event.guild.getTextChannelById(storedGuild.welcomeChannel ?: return@thenAccept) ?: return@thenAccept

            if (storedGuild.welcome && storedGuild.welcomeMessage.isNotBlank()) {
                channel.sendMessage(
                        storedGuild.welcomeMessage
                                .replace("%USER%", event.user.asMention)
                                .replace("%USERNAME%", event.user.name)
                                .replace("%SERVER%", event.guild.name)
                                .replace("%MEMBERNUM%", (event.guild.members.indexOf(event.member) + 1).toString())
                ).queue()
            }
        }
    }

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        DatabaseWrapper.getGuildSafe(event.guild).thenAccept { storedGuild ->
            asyncTransaction(Akatsuki.pool) {
                if (!storedGuild.modlogs) {
                    return@asyncTransaction
                }

                val modlogs = Modlogs.select { Modlogs.guildId.eq(event.guild.idLong) }
                val modlogChannel = event.guild.getTextChannelById(storedGuild.modlogChannel ?: return@asyncTransaction) ?: return@asyncTransaction
                val audit = event.guild.auditLogs.type(ActionType.KICK).limit(2).firstOrNull { it.targetId == event.user.id } ?: return@asyncTransaction
                val case = modlogs.count() + 1

                val msg = modlogChannel.sendMessage("""
                **Kick** | Case $case
                **User**: ${event.user.name}#${event.user.discriminator} (${event.user.id})
                **Reason**: ${audit.reason ?: "`Responsible moderator, please use the reason command to set this reason`"}
                **Responsible moderator**: ${audit.user.name}#${audit.user.discriminator} (${audit.user.id})
            """.trimIndent()).complete()

                Modlogs.insert {
                    it[messageId] = msg.idLong
                    it[modId] = audit.user.idLong
                    it[guildId] = event.guild.idLong
                    it[targetId] = audit.targetIdLong
                    it[caseId] = case
                    it[type] = "KICK"
                    it[reason] = audit.reason
                }
            }.execute()

            val channel = event.guild.getTextChannelById(storedGuild.welcomeChannel ?: return@thenAccept) ?: return@thenAccept

            if (storedGuild.welcome && storedGuild.leaveMessage.isNotBlank()) {
                channel.sendMessage(
                        storedGuild.leaveMessage
                                .replace("%USER%", event.user.asMention)
                                .replace("%USERNAME%", event.user.name)
                                .replace("%SERVER%", event.guild.name)
                ).queue()
            }
        }
    }

    override fun onGuildUnban(event: GuildUnbanEvent) {
        DatabaseWrapper.getGuildSafe(event.guild).thenAccept { storedGuild ->
            asyncTransaction(Akatsuki.pool) {
                if (!storedGuild.modlogs) {
                    return@asyncTransaction
                }

                val modlogs = Modlogs.select { Modlogs.guildId.eq(event.guild.idLong) }
                val modlogChannel = event.guild.getTextChannelById(storedGuild.modlogChannel ?: return@asyncTransaction) ?: return@asyncTransaction
                val audit = event.guild.auditLogs.type(ActionType.UNBAN).first { it.targetId == event.user.id }
                val case = modlogs.count() + 1

                val msg = modlogChannel.sendMessage("""
                **Unban** | Case $case
                **User**: ${event.user.name}#${event.user.discriminator} (${event.user.id})
                **Reason**: ${audit.reason ?: "`Responsible moderator, please use the reason command to set this reason`"}
                **Responsible moderator**: ${audit.user.name}#${audit.user.discriminator} (${audit.user.id})
            """.trimIndent()).complete()

                Modlogs.insert {
                    it[messageId] = msg.idLong
                    it[modId] = audit.user.idLong
                    it[guildId] = event.guild.idLong
                    it[targetId] = audit.targetIdLong
                    it[caseId] = case
                    it[type] = "UNBAN"
                    it[reason] = audit.reason
                }
            }.execute()
        }
    }

    override fun onGuildBan(event: GuildBanEvent) {
        DatabaseWrapper.getGuildSafe(event.guild).thenAccept { storedGuild ->
            asyncTransaction(Akatsuki.pool) {
                if (!storedGuild.modlogs) {
                    return@asyncTransaction
                }

                val modlogs = Modlogs.select { Modlogs.guildId.eq(event.guild.idLong) }
                val modlogChannel = event.guild.getTextChannelById(storedGuild.modlogChannel ?: return@asyncTransaction) ?: return@asyncTransaction
                val audit = event.guild.auditLogs.type(ActionType.BAN).first { it.targetId == event.user.id }
                val case = modlogs.count() + 1

                val msg = modlogChannel.sendMessage("""
                **Ban** | Case $case
                **User**: ${event.user.name}#${event.user.discriminator} (${event.user.id})
                **Reason**: ${audit.reason ?: "`Responsible moderator, please use the reason command to set this reason`"}
                **Responsible moderator**: ${audit.user.name}#${audit.user.discriminator} (${audit.user.id})
            """.trimIndent()).complete()

                Modlogs.insert {
                    it[messageId] = msg.idLong
                    it[modId] = audit.user.idLong
                    it[guildId] = event.guild.idLong
                    it[targetId] = audit.targetIdLong
                    it[caseId] = case
                    it[type] = "BAN"
                    it[reason] = audit.reason
                }
            }.execute()
        }
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        DatabaseWrapper.getGuildSafe(event.guild).thenAccept { storedGuild ->
            asyncTransaction(Akatsuki.pool) {
                if (!storedGuild.modlogs
                        || !event.roles.contains(event.guild.getRoleById(storedGuild.mutedRole ?: return@asyncTransaction) ?: return@asyncTransaction))
                    return@asyncTransaction

                val modlogs = Modlogs.select { Modlogs.guildId.eq(event.guild.idLong) }
                val modlogChannel = event.guild.getTextChannelById(storedGuild.modlogChannel ?: return@asyncTransaction) ?: return@asyncTransaction
                val audit = event.guild.auditLogs.type(ActionType.MEMBER_ROLE_UPDATE).firstOrNull { it.targetId == event.user.id } ?: return@asyncTransaction

                Roles.insert {
                    it[userId] = event.user.idLong
                    it[guildId] = event.guild.idLong
                    it[roleId] = event.roles.first().idLong
                }

                val case = modlogs.count() + 1

                val msg = modlogChannel.sendMessage("""
                **Mute** | Case $case
                **User**: ${event.user.name}#${event.user.discriminator} (${event.user.id})
                **Reason**: ${audit.reason ?: "`Responsible moderator, please use the reason command to set this reason`"}
                **Responsible moderator**: ${audit.user.name}#${audit.user.discriminator} (${audit.user.id})
            """.trimIndent()).complete()

                Modlogs.insert {
                    it[messageId] = msg.idLong
                    it[modId] = audit.user.idLong
                    it[guildId] = event.guild.idLong
                    it[targetId] = audit.targetIdLong
                    it[caseId] = case
                    it[type] = "MUTE"
                    it[reason] = audit.reason
                }
            }.execute()
        }
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        DatabaseWrapper.getGuildSafe(event.guild).thenAccept { storedGuild ->
            asyncTransaction(Akatsuki.pool) {
                if (!storedGuild.modlogs
                        || !event.roles.contains(event.guild.getRoleById(storedGuild.mutedRole ?: return@asyncTransaction) ?: return@asyncTransaction))
                    return@asyncTransaction

                val modlogs = Modlogs.select { Modlogs.guildId.eq(event.guild.idLong) }
                val modlogChannel = event.guild.getTextChannelById(storedGuild.modlogChannel ?: return@asyncTransaction) ?: return@asyncTransaction
                val audit = event.guild.auditLogs.type(ActionType.MEMBER_ROLE_UPDATE).firstOrNull { it.targetId == event.user.id } ?: return@asyncTransaction

                Roles.deleteWhere {
                    Roles.guildId.eq(event.guild.idLong) and Roles.userId.eq(event.user.idLong) and Roles.roleId.eq(event.roles.first().idLong)
                }

                val case = modlogs.count() + 1

                val msg = modlogChannel.sendMessage("""
                **Unmute** | Case $case
                **User**: ${event.user.name}#${event.user.discriminator} (${event.user.id})
                **Reason**: ${audit.reason ?: "`Responsible moderator, please use the reason command to set this reason`"}
                **Responsible moderator**: ${audit.user.name}#${audit.user.discriminator} (${audit.user.id})
            """.trimIndent()).complete()

                Modlogs.insert {
                    it[messageId] = msg.idLong
                    it[modId] = audit.user.idLong
                    it[guildId] = event.guild.idLong
                    it[targetId] = audit.targetIdLong
                    it[caseId] = case
                    it[type] = "UNMUTE"
                    it[reason] = audit.reason
                }
            }.execute()
        }
    }

    private fun startPresenceTimer() {
        timer.scheduleAtFixedRate(Date(), 120000L) {
            val presence = Akatsuki.config.presences[Math.floor(Math.random() * Akatsuki.config.presences.size).toInt()]
            val gameType = when(presence.type) {
                "streaming" -> Game.GameType.STREAMING

                "listening" -> Game.GameType.LISTENING

                "watching" -> Game.GameType.WATCHING

                "default" -> Game.GameType.DEFAULT
                "playing" -> Game.GameType.DEFAULT
                else -> Game.GameType.DEFAULT
            }

            if (Akatsuki.jda != null) {
                Akatsuki.jda!!.presence.setPresence(Game.of(gameType, presence.text), false)
            } else {
                Akatsuki.shardManager.setGame(Game.of(gameType, presence.text))
            }
        }
    }

    private fun startReminderChecker(checkDelay: Long = 1000L) {
        timer.scheduleAtFixedRate(Date(), checkDelay) {
            val now = System.currentTimeMillis()

            asyncTransaction(Akatsuki.pool) {
                val results = Reminders.select {
                    Reminders.timestamp.less(now) or Reminders.timestamp.eq(now)
                }

                results.forEach {
                    val user = DatabaseWrapper.getUser(it[Reminders.userId]).get()
                    val locale = Locale(user.lang.split("_")[0], user.lang.split("_")[1])
                    val bundle = ResourceBundle.getBundle("i18n.Kyubey", locale, UTF8Control())
                    val channel = if (Akatsuki.jda != null) {
                        Akatsuki.jda!!.getTextChannelById(it[Reminders.channelId])
                    } else {
                        Akatsuki.shardManager.getTextChannelById(it[Reminders.channelId])
                    }

                    channel?.sendMessage(
                            I18n.parse(
                                    bundle.getString("reminder"),
                                    mapOf(
                                            "user" to "<@${it[Reminders.userId]}>",
                                            "reminder" to it[Reminders.reminder]
                                    )
                            )
                    )?.queue()

                    Reminders.deleteWhere {
                        Reminders.userId
                                .eq(it[Reminders.userId])
                                .and(Reminders.reminder.eq(it[Reminders.reminder]))
                                .and(Reminders.timestamp.eq(it[Reminders.timestamp]))
                    }
                }
            }.execute()
        }
    }

    companion object {
        val logger = Logger(EventListener::class.jvmName)
        val snipes = TLongLongHashMap()
        val cmdHandler = CommandHandler()
        val waiter = EventWaiter()

        fun updateStats() {
            val jsonType = MediaType.parse("application/json")

            if (Akatsuki.jda != null) {
                val json = mapOf(
                        "server_count" to Akatsuki.jda!!.guilds.size
                )
                val body = RequestBody.create(jsonType, JSONObject(json).toString())

                if (Akatsuki.config.api.discordbots.isNotEmpty()) {
                    Http.post("https://bots.discord.pw/api/bots/${Akatsuki.jda!!.selfUser.id}/stats", body) {
                        addHeader("Authorization", Akatsuki.config.api.discordbots)
                    }.thenAccept {
                        logger.info("Updated stats on bots.discord.pw")
                        it.close()
                    }.thenApply {}.exceptionally {
                        logger.error("Error while trying to update stats on bots.discord.pw", it)
                    }
                }

                if (Akatsuki.config.api.discordbotsorg.isNotEmpty()) {
                    Http.post("https://discordbots.org/api/bots/${Akatsuki.jda!!.selfUser.id}/stats", body) {
                        addHeader("Authorization", Akatsuki.config.api.discordbotsorg)
                    }.thenAccept {
                        logger.info("Updated stats on discordbots.org")
                        it.close()
                    }.thenApply {}.exceptionally {
                        logger.error("Error while trying to update stats on discordbots.org", it)
                    }
                }
            } else {
                for (shard in Akatsuki.shardManager.shards) {
                    val json = mapOf(
                            "server_count" to shard.guilds.size,
                            "shard_id" to shard.shardInfo.shardId,
                            "shard_count" to Akatsuki.shardManager.shardsTotal
                    )
                    val body = RequestBody.create(jsonType, JSONObject(json).toString())

                    if (Akatsuki.config.api.discordbots.isNotEmpty()) {
                        Http.post("https://bots.discord.pw/api/bots/${shard.selfUser.id}/stats", body) {
                            addHeader("Authorization", Akatsuki.config.api.discordbots)
                        }.thenAccept {
                            logger.info("Updated stats on bots.discord.pw")
                            it.close()
                        }.thenApply {}.exceptionally {
                            logger.error("Error while trying to update stats on bots.discord.pw", it)
                        }
                    }

                    if (Akatsuki.config.api.discordbotsorg.isNotEmpty()) {
                        Http.post("https://discordbots.org/api/bots/${shard.selfUser.id}/stats", body) {
                            addHeader("Authorization", Akatsuki.config.api.discordbotsorg)
                        }.thenAccept {
                            logger.info("Updated stats on discordbots.org")
                            it.close()
                        }.thenApply {}.exceptionally {
                            logger.error("Error while trying to update stats on discordbots.org", it)
                        }
                    }
                }
            }
        }
    }
}