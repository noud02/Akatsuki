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

package moe.kyubey.akatsuki.utils

import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.extensions.searchMembers
import moe.kyubey.akatsuki.extensions.searchRoles
import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseMathLib

object LuaSandbox {
    fun eval(script: String, args: List<String>, ctx: Context) {
        val serverGlobals = Globals()
        serverGlobals.load(JseBaseLib())
        serverGlobals.load(PackageLib())
        serverGlobals.load(StringLib())
        serverGlobals.load(JseMathLib())

        LoadState.install(serverGlobals)
        LuaC.install(serverGlobals)

        val globals = Globals()
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(JseMathLib())
        globals.load(DebugLib())
        val sethook = globals["debug"]["sethook"]
        globals["debug"] = LuaValue.NIL
        globals["print"] = LuaValue.NIL

        val discordLib = LuaValue.tableOf()
        val contextLib = LuaValue.tableOf()

        discordLib["add_role"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
                if (arg1 == null) {
                    throw Exception("Member argument wasn't given!")
                }

                if (arg2 == null) {
                    throw Exception("Role argument wasn't given!")
                }

                if (!arg1.islong() && !arg1.isstring()) {
                    throw Exception("Invalid member argument type!")
                }

                if (!arg2.islong() && !arg1.isstring()) {
                    throw Exception("Invalid role argument type!")
                }

                val member =
                        if (arg1.islong()) {
                            ctx.guild!!.getMemberById(arg1.tolong())
                                    ?: throw Exception("Invalid member!")
                        } else {
                            ctx.guild!!.searchMembers(arg1.tojstring()).firstOrNull()
                                    ?: throw Exception("Couldn't find that member!")
                        }

                val role =
                        if (arg2.islong()) {
                            ctx.guild.getRoleById(arg2.tolong())
                                    ?: throw Exception("Invalid role!")
                        } else {
                            ctx.guild.searchRoles(arg2.tojstring()).firstOrNull()
                                    ?: throw Exception("Couldn't find that role!")
                        }

                ctx.guild.controller.addSingleRoleToMember(member, role)

                return LuaValue.NIL
            }
        }

        discordLib["remove_role"] = object : ThreeArgFunction() {
            override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue {
                if (arg1 == null) {
                    throw Exception("Member argument wasn't given!")
                }

                if (arg2 == null) {
                    throw Exception("Role argument wasn't given!")
                }

                if (!arg1.islong() && !arg1.isstring()) {
                    throw Exception("Invalid member argument type!")
                }

                if (!arg2.islong() && !arg2.isstring()) {
                    throw Exception("Invalid role argument type!")
                }

                val member =
                        if (arg1.islong()) {
                            ctx.guild!!.getMemberById(arg1.tolong())
                                    ?: throw Exception("Invalid member!")
                        } else {
                            ctx.guild!!.searchMembers(arg1.tojstring()).firstOrNull()
                                    ?: throw Exception("Couldn't find that member!")
                        }

                val role =
                        if (arg2.islong()) {
                            ctx.guild.getRoleById(arg2.tolong())
                                    ?: throw Exception("Invalid role!")
                        } else {
                            ctx.guild.searchRoles(arg2.tojstring()).firstOrNull()
                                    ?: throw Exception("Couldn't find that role!")
                        }

                val rem = ctx.guild.controller.removeSingleRoleFromMember(member, role)

                if (arg3 != null) {
                    rem.reason(arg3.tojstring())
                }

                rem.queue()

                return LuaValue.NIL
            }
        }

        discordLib["get_user"] = object : OneArgFunction() {
            override fun call(arg: LuaValue?): LuaValue {
                if (arg == null) {
                    throw Exception("Query not specified!")
                }

                if (!arg.islong() && !arg.isstring()) {
                    throw Exception("Invalid query type!")
                }

                val data = LuaValue.tableOf()

                val user = ctx.guild!!.searchMembers(arg.tojstring()).firstOrNull()
                        ?: throw Exception("Couldn't find that user!")

                data["username"] = LuaValue.valueOf(user.user.name)
                data["discriminator"] = LuaValue.valueOf(user.user.discriminator)
                data["avatar_url"] = LuaValue.valueOf(user.user.avatarUrl)
                data["nick"] = if (user.nickname != null) LuaValue.valueOf(user.nickname) else LuaValue.NIL
                data["mention"] = LuaValue.valueOf(user.asMention)
                data["owner"] = LuaValue.valueOf(user.isOwner)
                data["id"] = LuaValue.valueOf(user.user.id)

                return data
            }
        }

        contextLib["user"] = LuaValue.tableOf().apply {
            set("username", LuaValue.valueOf(ctx.author.name))
            set("discriminator", LuaValue.valueOf(ctx.author.discriminator))
            set("avatar_url", LuaValue.valueOf(ctx.author.avatarUrl))
            set("nick", if (ctx.member != null && ctx.member.nickname != null) LuaValue.valueOf(ctx.member.nickname) else LuaValue.NIL)
            set("mention", LuaValue.valueOf(ctx.author.asMention))
            set("id", LuaValue.valueOf(ctx.author.id))
        }

        contextLib["args"] = LuaValue.listOf(args.map { LuaValue.valueOf(it) }.toTypedArray())

        contextLib["send"] = object : OneArgFunction() {
            override fun call(arg: LuaValue?): LuaValue {
                if (arg == null) {
                    throw Exception("Text argument is nil!")
                }

                ctx.event.channel.sendMessage(arg.tojstring()).queue()

                return LuaValue.NIL
            }
        }

        globals["ctx"] = contextLib
        globals["discord"] = discordLib

        try {
            val chunk = serverGlobals.load(script, "main", globals)
            val thread = LuaThread(globals, chunk)

            sethook.invoke(LuaValue.varargsOf(arrayOf(thread, object : ZeroArgFunction() {
                override fun call(): LuaValue {
                    throw Exception("Script hit resource limit!")
                }
            }, LuaValue.EMPTYSTRING, LuaValue.valueOf(20))))

            val result = thread.resume(LuaValue.NIL)

            if (!result.arg(2).isnil()) {
                ctx.send(result.arg(2).tojstring())
            }
        } catch (e: Exception) {
            ctx.send("```diff\n- $e```")
        }
    }
}