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
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.annotations.Perm
import moe.kyubey.akatsuki.entities.*
import moe.kyubey.akatsuki.db.schema.Guilds
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.Permission
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

@Perm(Permission.MANAGE_SERVER)
@Argument("prefix", "string")
class AddPrefix : Command() {
    override val guildOnly = true
    override val desc = "Add a prefix"

    override fun run(ctx: Context) {
        val prefix = ctx.args["prefix"] as String

        asyncTransaction(Akatsuki.pool) {
            val guild = Guilds.select {
                Guilds.id.eq(ctx.guild!!.idLong)
            }.first()

            try {
                Guilds.update({
                    Guilds.id.eq(ctx.guild!!.idLong)
                }) {
                    it[prefixes] = guild[Guilds.prefixes] + prefix
                }
                ctx.send(
                        I18n.parse(
                                ctx.lang.getString("prefix_added"),
                                mapOf("prefix" to prefix)
                        )
                )
            } catch (e: Throwable) {
                ctx.sendError(e)
            }
        }.execute()
    }
}

@Perm(Permission.MANAGE_SERVER)
@Argument("prefix", "string")
class RemPrefix : Command() {
    override val guildOnly = true
    override val desc = "Remove a prefix"

    override fun run(ctx: Context) {
        val prefix = ctx.args["prefix"] as String

        asyncTransaction(Akatsuki.pool) {
            if (ctx.storedGuild!!.prefixes.isEmpty()) {
                return@asyncTransaction ctx.send(ctx.lang.getString("remove_no_prefix"))
            }

            try {
                Guilds.update({
                    Guilds.id.eq(ctx.guild!!.idLong)
                }) {
                    val list = ctx.storedGuild.prefixes.toMutableList()
                    list.remove(prefix)
                    it[prefixes] = list.toTypedArray()
                }
                ctx.send(
                        I18n.parse(
                                ctx.lang.getString("prefix_removed"),
                                mapOf("prefix" to prefix)
                        )
                )
            } catch (e: Throwable) {
                ctx.sendError(e)
            }
        }.execute()
    }
}

@Load
class Prefix : Command() {
    override val guildOnly = true
    override val desc = "Add, view or delete the guild's prefixes"

    init {
        addSubcommand(AddPrefix(), "add")
        addSubcommand(RemPrefix(), "remove")
    }

    override fun run(ctx: Context)
            = ctx.send(
            I18n.parse(
                    ctx.lang.getString("current_prefixes"),
                    mapOf("prefixes" to if (ctx.storedGuild!!.prefixes.isEmpty()) "none" else ctx.storedGuild.prefixes.joinToString(", "))
            )
    )
}