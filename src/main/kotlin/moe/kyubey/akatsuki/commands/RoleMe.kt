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
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import org.jetbrains.exposed.sql.update

@Perm(Permission.MANAGE_ROLES)
@Argument("role", "role")
class AddRole : Command() {
    override val desc = "Add user-assignable roles."
    override val guildOnly = true

    override fun run(ctx: Context) {
        val role = ctx.args["role"] as Role

        asyncTransaction(Akatsuki.pool) {
            Guilds.update({
                Guilds.id.eq(ctx.guild!!.idLong)
            }) {
                it[rolemeRoles] = ctx.storedGuild!!.rolemeRoles.plus(role.name.toLowerCase() to role.idLong)
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("roleme_added_role"),
                            mapOf("role" to role.name)
                    )
            )
        }.execute()
    }
}

@Perm(Permission.MANAGE_ROLES)
@Argument("role", "role")
class RemRole : Command() {
    override val desc = "Remove roles."
    override val guildOnly = true

    override fun run(ctx: Context) {
        val role = ctx.args["role"] as Role

        asyncTransaction(Akatsuki.pool) {
            Guilds.update({
                Guilds.id.eq(ctx.guild!!.idLong)
            }) {
                it[rolemeRoles] = ctx.storedGuild!!.rolemeRoles.minus(role.name.toLowerCase())
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("roleme_removed_role"),
                            mapOf("role" to role.name)
                    )
            )
        }.execute()
    }
}

class ListRoles : Command() {
    override val desc = "List all roles available."
    override val guildOnly = true

    override fun run(ctx: Context) = ctx.send(EmbedBuilder().apply {
        setTitle(ctx.lang.getString("roles"))
        ctx.storedGuild!!.rolemeRoles.forEach { descriptionBuilder.append("<@&${it.value}>\n") }
    }.build())
}

@Load
@Argument("role", "string")
@Flag("remove", 'r', "Removes the role")
class RoleMe : Command() {
    override val desc = "Assign roles to yourself!"
    override val guildOnly = true

    init {
        addSubcommand(AddRole(), "add")
        addSubcommand(RemRole(), "remove")
        addSubcommand(ListRoles(), "list")
    }

    override fun run(ctx: Context) {
        val roleName = (ctx.args["role"] as String).toLowerCase()
        val roleId = ctx.storedGuild!!.rolemeRoles[roleName]!!

        if (!ctx.selfMember!!.canInteract(ctx.member))
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("roleme_perm_cant_assign"),
                            mapOf("username" to ctx.author.name)
                    )
            )

        val role = if (Akatsuki.jda != null) {
            Akatsuki.jda!!.getRoleById(roleId)
        } else {
            Akatsuki.shardManager.getRoleById(roleId)
        }

        if (ctx.flags.argMap.containsKey("remove") || ctx.flags.argMap.containsKey("r")) {
            if (roleName !in ctx.storedGuild.rolemeRoles) {
                return ctx.send(
                        I18n.parse(
                                ctx.lang.getString("roleme_cant_remove"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            ctx.guild!!.controller.removeSingleRoleFromMember(ctx.member!!, role).reason("[ RoleMe ]").queue({
                ctx.send(
                        I18n.parse(
                                ctx.lang.getString("roleme_removed"),
                                mapOf("role" to role.name)
                        )
                )
            }) {
                ctx.sendError(it)
            }
        } else {
            if (roleName !in ctx.storedGuild.rolemeRoles) {
                return ctx.send("You can't assign that role to yourself!")
            }

            ctx.guild!!.controller.addSingleRoleToMember(ctx.member!!, role).reason("[ RoleMe ]").queue({
                ctx.send(
                        I18n.parse(
                                ctx.lang.getString("roleme_assigned"),
                                mapOf("role" to role.name)
                        )
                )
            }) {
                ctx.sendError(it)
            }
        }
    }
}