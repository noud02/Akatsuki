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
import moe.kyubey.akatsuki.annotations.Alias
import moe.kyubey.akatsuki.db.schema.Scripts
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.I18n
import moe.kyubey.akatsuki.utils.LuaSandbox
import net.dv8tion.jda.core.Permission
import org.jetbrains.exposed.sql.*

@Perm(Permission.MANAGE_SERVER)
@Argument("script", "string")
class TestCommand : Command() {
    override val desc = "Test lua scripts"
    override val guildOnly = true

    override fun run(ctx: Context) {
        val script = ctx.rawArgs.joinToString(" ")

        LuaSandbox.eval(script, ctx.rawArgs, ctx)
    }
}

@Perm(Permission.MANAGE_SERVER)
@Arguments(
        Argument("name", "string"),
        Argument("script", "string")
)
@Flag("owner", 'o', "Make the custom command server owner only.")
class AddCommand : Command() {
    override val desc = "Add custom scripts."
    override val guildOnly = true

    override fun run(ctx: Context) {
        val content = ctx.rawArgs.slice(1 until ctx.rawArgs.size).joinToString(" ")
        val name = ctx.args["name"] as String

        asyncTransaction(Akatsuki.pool) {
            val scriptsMatch = Scripts.select { Scripts.guildId.eq(ctx.guild!!.idLong).and(Scripts.scriptName.eq(name)) }

            if (!scriptsMatch.empty()) {
                return@asyncTransaction ctx.send(
                        I18n.parse(
                                ctx.lang.getString("script_exists"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            Scripts.insert {
                it[script] = content
                it[scriptName] = name
                it[ownerId] = ctx.author.idLong
                it[ownerOnly] = ctx.flags.argMap.containsKey("owner") || ctx.flags.argMap.containsKey("o")
                it[guildId] = ctx.guild!!.idLong
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("added_script"),
                            mapOf("name" to name)
                    )
            )
        }.execute()
    }
}

@Perms(
        Perm(Permission.ADMINISTRATOR, true),
        Perm(Permission.MANAGE_SERVER)
)
@Argument("name", "string")
class RemoveCommand : Command() {
    override val guildOnly = true
    override val desc = "Remove custom scripts."

    override fun run(ctx: Context) {
        val name = ctx.args["name"] as String
        val hasPerm = ctx.perms["ADMINISTRATOR"] as Boolean

        asyncTransaction(Akatsuki.pool) {
            val match = Scripts.select { Scripts.guildId.eq(ctx.guild!!.idLong).and(Scripts.scriptName.eq(name)) }.firstOrNull()
                    ?: return@asyncTransaction ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("script_not_found"),
                                    mapOf("username" to ctx.author.name)
                            )
                    )

            if (!hasPerm && match[Scripts.ownerId] != ctx.author.idLong) {
                return@asyncTransaction ctx.send(
                        I18n.parse(
                                ctx.lang.getString("cant_delete_script"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            Scripts.deleteWhere {
                Scripts.scriptName.eq(name).and(Scripts.guildId.eq(ctx.guild!!.idLong))
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("deleted_script"),
                            mapOf("name" to name)
                    )
            )
        }.execute()
    }
}

@Perms(
        Perm(Permission.MANAGE_SERVER),
        Perm(Permission.ADMINISTRATOR, true)
)
@Arguments(
        Argument("name", "string"),
        Argument("script", "string")
)
class EditCommand : Command() {
    override val desc = "Edit custom scripts."
    override val guildOnly = true

    override fun run(ctx: Context) {
        val name = ctx.args["name"] as String
        val hasPerm = ctx.perms["ADMINISTRATOR"] as Boolean

        asyncTransaction(Akatsuki.pool) {
            val match = Scripts.select { Scripts.guildId.eq(ctx.guild!!.idLong).and(Scripts.scriptName.eq(name)) }.firstOrNull()
                    ?: return@asyncTransaction ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("script_not_found"),
                                    mapOf("username" to ctx.author.name)
                            )
                    )

            if (!hasPerm && match[Scripts.ownerId] != ctx.author.idLong) {
                return@asyncTransaction ctx.send(
                        I18n.parse(
                                ctx.lang.getString("cant_edit_script"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            Scripts.update({
                Scripts.scriptName.eq(name).and(Scripts.guildId.eq(ctx.guild!!.idLong))
            }) {
                it[script] = ctx.args["script"] as String
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("edited_script"),
                            mapOf("name" to name)
                    )
            )
        }.execute()
    }
}

@Load
@Arguments(
        Argument("command", "string"),
        Argument("arguments", "string", true)
)
@Alias("cmd", "customcommand", "c", "s")
@Perm(Permission.ADMINISTRATOR, true)
class Script : Command() {
    override val desc = "Execute custom scripts. (https://github.com/Kyuubey/Akatsuki/blob/master/SCRIPT_DOCS.md)"
    override val guildOnly = true

    init {
        addSubcommand(TestCommand(), "test")
        addSubcommand(AddCommand(), "add")
        addSubcommand(RemoveCommand(), "remove")
        addSubcommand(EditCommand(), "edit")
    }

    override fun run(ctx: Context) {
        val cmd = ctx.args["command"] as String
        val hasPerm = ctx.perms["ADMINISTRATOR"] as Boolean

        asyncTransaction(Akatsuki.pool) {
            val command = Scripts.select { Scripts.guildId.eq(ctx.guild!!.idLong).and(Scripts.scriptName.eq(cmd)) }.firstOrNull()
                    ?: return@asyncTransaction ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("script_not_found"),
                                    mapOf("username" to ctx.author.name)
                            )
                    )

            if (command[Scripts.ownerOnly] && !hasPerm) {
                return@asyncTransaction ctx.send(
                        I18n.parse(
                                ctx.lang.getString("script_owner_only"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            LuaSandbox.eval(command[Scripts.script], ctx.rawArgs.slice(1 until ctx.rawArgs.size), ctx)
        }.execute()
    }
}
