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

package me.noud02.akatsuki.commands

import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.Akatsuki
import me.noud02.akatsuki.EventListener
import me.noud02.akatsuki.annotations.Argument
import me.noud02.akatsuki.annotations.Arguments
import me.noud02.akatsuki.annotations.Load
import me.noud02.akatsuki.annotations.Perm
import me.noud02.akatsuki.db.schema.Restrictions
import me.noud02.akatsuki.entities.Command
import me.noud02.akatsuki.entities.Context
import me.noud02.akatsuki.utils.I18n
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import org.jetbrains.exposed.sql.insert

@Arguments(
        Argument("command", "string"),
        Argument("user", "user"),
        Argument("reason", "string", true)
)
class RestrictGlobally : Command() {
    override val desc = "Restrict usage of commands globally."
    override val guildOnly = true
    override val ownerOnly = true

    override fun run(ctx: Context) {
        val cmd = ctx.args["command"] as String
        val mem = ctx.args["user"] as Member

        if (cmd !in EventListener.cmdHandler.commands && cmd != "all")
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("command_not_found"),
                            mapOf("username" to ctx.author.name)
                    )
            )

        asyncTransaction(Akatsuki.pool) {
            Restrictions.insert {
                it[guildId] = null
                it[userId] = mem.user.idLong
                it[everyone] = false
                it[global] = true
                it[command] = cmd
                it[reason] = ctx.args.getOrDefault(
                        "reason",
                        "Globally restricted by ${ctx.author.name}#${ctx.author.discriminator}"
                ) as String
            }

            ctx.send("Restricted global usage of command $cmd for user ${mem.user.name}#${mem.user.discriminator}!") // TODO translation
        }.execute()
    }
}

@Load
@Perm(Permission.MANAGE_SERVER)
@Arguments(
        Argument("command", "string"),
        Argument("user", "user", true)
)
class Restrict : Command() {
    override val desc = "Restrict usage of commands."
    override val guildOnly = true

    init {
        addSubcommand(RestrictGlobally(), "global")
    }

    override fun run(ctx: Context) {
        val cmd = ctx.args["command"] as String

        if (cmd !in EventListener.cmdHandler.commands && cmd != "all")
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("command_not_found"),
                            mapOf("username" to ctx.author.name)
                    )
            )

        asyncTransaction(Akatsuki.pool) {
            Restrictions.insert {
                it[guildId] = ctx.guild!!.idLong
                if ("user" in ctx.args) {
                    val mem = ctx.args["user"] as Member

                    it[userId] = mem.user.idLong
                    it[everyone] = false
                } else {
                    it[everyone] = true
                }

                it[global] = false
                it[command] = cmd
                it[reason] = "Restricted by ${ctx.author.name}#${ctx.author.discriminator}"
            }

            if ("user" in ctx.args) {
                val mem = ctx.args["user"] as Member

                ctx.send("Restricted usage of command $cmd for ${mem.user.name}#${mem.user.discriminator}!") // TODO translation
            } else {
                ctx.send("Restricted usage of command $cmd!") // TODO translation
            }
        }.execute()
    }
}