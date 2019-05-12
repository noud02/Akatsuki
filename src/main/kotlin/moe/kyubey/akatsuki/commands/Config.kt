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
import moe.kyubey.akatsuki.annotations.*
import moe.kyubey.akatsuki.db.schema.Guilds
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.update

@Argument("option", "string")
class EnableOption : Command() {
    override val desc = "Enable a setting."
    override val guildOnly = true

    private val options = listOf(
            "forceLang",
            "starboard",
            "logs",
            "modlogs",
            "welcome",
            "levelMessages",
            "antiInvite"
    )

    override fun run(ctx: Context) {
        val opt = (ctx.args["option"] as String).toLowerCase()

        val humanOptions = mutableMapOf<String, String>()

        for (option in options) {
            val regex = "([A-Z])".toRegex()
            val letter = regex.find(option)?.groupValues?.get(0)

            if (letter != null)
                humanOptions[option.replace(regex, " ${letter.toLowerCase()}")] = option

            humanOptions[option.toLowerCase()] = option
        }

        if (opt !in humanOptions) {
            return ctx.send("Option not found!") // TODO translation
        }

        asyncTransaction(Akatsuki.pool) {
            Guilds.update({
                Guilds.id.eq(ctx.guild!!.idLong)
            }) {
                val col = columns.first { it.name == humanOptions[opt] } as Column<Boolean>

                it[col] = true
            }
        }.execute()

        ctx.send("Enabled $opt!") // TODO translation
    }
}

@Argument("option", "string")
class DisableOption : Command() {
    override val desc = "Disable a setting."
    override val guildOnly = true

    private val options = listOf(
            "forceLang",
            "starboard",
            "logs",
            "modlogs",
            "welcome",
            "levelMessages",
            "antiInvite"
    )

    override fun run(ctx: Context) {
        val opt = (ctx.args["option"] as String).toLowerCase()

        val humanOptions = mutableMapOf<String, String>()

        for (option in options) {
            val regex = "([A-Z])".toRegex()
            val letter = regex.find(option)?.groupValues?.get(0)

            if (letter != null)
                humanOptions[option.replace(regex, " ${letter.toLowerCase()}")] = option

            humanOptions[option.toLowerCase()] = option
        }

        if (opt !in humanOptions) {
            return ctx.send("Option not found!") // TODO translation
        }

        asyncTransaction(Akatsuki.pool) {
            Guilds.update({
                Guilds.id.eq(ctx.guild!!.idLong)
            }) {
                val col = columns.first { it.name == humanOptions[opt] }as Column<Boolean>

                it[col] = false
            }
        }.execute()

        ctx.send("Disabled $opt!") // TODO translation
    }
}

@Arguments(
        Argument("option", "string"),
        Argument("channel", "textchannel")
)
class SetChannelOption : Command() {
    override val desc = "Set an option's channel."
    override val guildOnly = true

    private val options = listOf(
            "starboardChannel",
            "modlogChannel",
            "welcomeChannel"
    )

    override fun run(ctx: Context) {
        val opt = (ctx.args["option"] as String).toLowerCase()
        val channel = ctx.args["channel"] as TextChannel

        if (opt !in options.map(String::toLowerCase))
            return ctx.send("Option not found!") // TODO translation

        asyncTransaction(Akatsuki.pool) {
            Guilds.update({
                Guilds.id.eq(ctx.guild!!.idLong)
            }) {
                val col = columns.first { it.name.toLowerCase() == opt } as Column<Long>

                it[col] = channel.idLong
            }
        }.execute()

        ctx.send("Channel for $opt is now ${channel.asMention}!") // TODO translation
    }
}

@Arguments(
        Argument("option", "string"),
        Argument("role", "role")
)
class SetRoleOption : Command() {
    override val desc = "Set an option's role."
    override val guildOnly = true

    private val options = listOf(
            "mutedRole"
    )

    override fun run(ctx: Context) {
        val opt = (ctx.args["option"] as String).toLowerCase()
        val role = ctx.args["role"] as Role

        if (opt !in options.map(String::toLowerCase)) {
            return ctx.send("Option not found!") // TODO translation
        }

        asyncTransaction(Akatsuki.pool) {
            Guilds.update({
                Guilds.id.eq(ctx.guild!!.idLong)
            }) {
                val col = columns.first { it.name.toLowerCase() == opt } as Column<Long>

                it[col] = role.idLong
            }
        }.execute()

        ctx.send("Role for $opt is now ${role.name}!") // TODO translation
    }
}

@Arguments(
        Argument("option", "string"),
        Argument("string", "string")
)
class SetStringOption : Command() {
    override val desc = "Set an option's text."

    private val options = listOf(
            "welcomeMessage",
            "leaveMessage"
    )

    override fun run(ctx: Context) {
        val opt = (ctx.args["option"] as String).toLowerCase()
        val string = ctx.args["string"] as String

        if (opt !in options.map(String::toLowerCase)) {
            return ctx.send("Option not found!") // TODO translation
        }

        asyncTransaction(Akatsuki.pool) {
            Guilds.update({
                Guilds.id.eq(ctx.guild!!.idLong)
            }) {
                val col = columns.first { it.name.toLowerCase() == opt } as Column<String>

                it[col] = string
            }
        }.execute()

        ctx.send("$opt is now $string!") // TODO translation
    }
}

@Load
@Alias("cfg", "conf", "settings")
class Config : Command() {
    override val guildOnly = true
    override val desc = "View the current config!"

    init {
        addSubcommand(EnableOption(), "enable")
        addSubcommand(DisableOption(), "disable")
        addSubcommand(SetChannelOption(), "setchannel")
        addSubcommand(SetRoleOption(), "setrole")
        addSubcommand(SetStringOption(), "setstring")
    }

    override fun run(ctx: Context) {
        val embed = EmbedBuilder().apply {
            setTitle("Settings")
            addField(
                    "General",
                    "**prefixes:** ${if (ctx.storedGuild!!.prefixes.isNotEmpty()) ctx.storedGuild.prefixes.joinToString { "`$it`" } else "none"}\n" +
                            "**forceLang:** ${if (ctx.storedGuild.forceLang) "enabled" else "disabled"}\n" +
                            "**logs:** ${if (ctx.storedGuild.logs) "enabled" else "disabled"}\n" +
                            "**mutedRole:** ${ctx.guild!!.getRoleById(ctx.storedGuild.mutedRole ?: 0L)?.asMention ?: "none"}\n" +
                            "**levelMessages:** ${if (ctx.storedGuild.levelMessages) "enabled" else "disabled"}\n" +
                            "**antiInvite:** ${if (ctx.storedGuild.antiInvite) "enabled" else "disabled"}\n" +
                            "**ignoredChannels:** ${if (ctx.storedGuild.ignoredChannels.isNotEmpty()) ctx.storedGuild.ignoredChannels.joinToString { ctx.guild.getTextChannelById(it)?.asMention ?: "#invalid-channel" } else "none"}",
                    true
            )
            addField(
                    "Modlogs",
                    "**modlogs:** ${if (ctx.storedGuild.modlogs) "enabled" else "disabled"}\n" +
                            "**modlogChannel:** ${ctx.guild.getTextChannelById(ctx.storedGuild.modlogChannel ?: 0L)?.asMention ?: "none"}",
                    true
            )
            addField(
                    "Starboard",
                    "**starboard:** ${if (ctx.storedGuild.starboard) "enabled" else "disabled"}\n" +
                            "**starboardChannel:** ${ctx.guild.getTextChannelById(ctx.storedGuild.starboardChannel ?: 0L)?.asMention ?: "none"}",
                    true
            )
            addField(
                    "Welcomer",
                    "**welcome:** ${if (ctx.storedGuild.welcome) "enabled" else "disabled"}\n" +
                            "**welcomeChannel:** ${ctx.guild.getTextChannelById(ctx.storedGuild.welcomeChannel ?: 0L)?.asMention ?: "none"}\n" +
                            "**welcomeMessage:** ${ctx.storedGuild.welcomeMessage}\n" +
                            "**leaveMessage:** ${ctx.storedGuild.leaveMessage}",
                    true
            )
        }

        ctx.send(embed.build())
    }
}