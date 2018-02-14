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

import me.noud02.akatsuki.annotations.Argument
import me.noud02.akatsuki.annotations.Arguments
import me.noud02.akatsuki.annotations.Load
import me.noud02.akatsuki.annotations.Perm
import me.noud02.akatsuki.entities.Command
import me.noud02.akatsuki.entities.Context
import me.noud02.akatsuki.utils.I18n
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.exceptions.PermissionException

@Load
@Perm(Permission.KICK_MEMBERS)
@Arguments(
        Argument("user", "user"),
        Argument("reason", "string", true)
)
class Unmute : Command() {
    override val guildOnly = true
    override val desc = "Unmute people."

    override fun run(ctx: Context) {
        if (ctx.storedGuild!!.mutedRole == null)
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("no_muted_role"),
                            mapOf(
                                    "username" to ctx.author.name
                            )
                    )
            )

        val user = ctx.args["user"] as Member
        val role = ctx.guild!!.getRoleById(ctx.storedGuild.mutedRole!!)
                ?: return ctx.send(
                I18n.parse(
                        ctx.lang.getString("muted_role_deleted"),
                        mapOf(
                                "username" to ctx.author.name
                        )
                )
        )

        if (!user.roles.contains(role))
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("not_muted"),
                            mapOf("username" to ctx.author.name)
                    )
            )

        ctx.guild.controller
                .removeSingleRoleFromMember(user, role)
                .reason("[ ${ctx.author.name}#${ctx.author.discriminator} ] ${ctx.args.getOrDefault("reason", "none")}")
                .queue({
                    ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("unmuted_user"),
                                    mapOf("username" to user.user.name)
                            )
                    )
                }) {
                    if (it is PermissionException)
                        ctx.send(
                                I18n.parse(
                                        ctx.lang.getString("perm_cant_unmute"),
                                        mapOf(
                                                "username" to user.user.name,
                                                "permission" to I18n.permission(ctx.lang, it.permission.name)
                                        )
                                )
                        )
                    else
                        ctx.sendError(it)
                }
    }
}