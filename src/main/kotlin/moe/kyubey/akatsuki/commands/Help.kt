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

import moe.kyubey.akatsuki.EventListener
import moe.kyubey.akatsuki.annotations.Alias
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.*
import moe.kyubey.akatsuki.utils.I18n

@Load
@Alias("--help", "-h")
@Argument("command", "string", true)
class Help : Command() {
    override val desc = "Sends you help!"

    override fun run(ctx: Context) {
        if ("command" in ctx.args) {
            val cmd = ctx.args["command"] as String
            if (cmd !in EventListener.cmdHandler.commands) {
                ctx.send(
                        I18n.parse(
                                ctx.lang.getString("command_not_found"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            } else {
                ctx.send(EventListener.cmdHandler.help(cmd))
            }
        } else {
            val commands = EventListener.cmdHandler.commands
                    .filter { !it.value.ownerOnly }
                    .toSortedMap()
                    .map {
                        "\t${it.key}" + " ".repeat(20 - it.key.length) + it.value.desc
                    }

            val text = "Flags:\n\n\t-h, --help${" ".repeat(10)}Get help on a command!\n\nCommands:\n\n${commands.joinToString("\n")}"
            val partSize = 40
            val parts = mutableListOf<String>()
            val lines = text.split("\n")
            var part = ""

            for (line in lines) {
                if (part.split("\n").size >= partSize) {
                    parts.add(part)
                    part = ""
                }

                part += "$line\n"
            }

            if (part.isNotBlank() && part.split("\n").size < partSize) {
                parts.add(part)
            }

            ctx.author.openPrivateChannel().queue({ channel ->
                channel.sendMessage("Join my support server! https://kyubey.moe/support").queue({
                    for (partt in parts) {
                        channel.sendMessage("```$partt```").queue()
                    }

                    ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("help_message"),
                                    mapOf("username" to ctx.author.name)
                            )
                    )
                }) { err ->
                    ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("error"),
                                    mapOf("error" to err)
                            )
                    )
                }
            }) { err ->
                ctx.send(
                        I18n.parse(
                                ctx.lang.getString("error"),
                                mapOf("error" to err)
                        )
                )
            }
        }
    }
}