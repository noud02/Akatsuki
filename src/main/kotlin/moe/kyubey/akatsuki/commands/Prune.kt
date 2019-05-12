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

import moe.kyubey.akatsuki.annotations.*
import moe.kyubey.akatsuki.entities.AsyncCommand
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.extensions.await
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.Permission
import kotlin.math.min

@Load
@Argument("messages", "number")
@Flags(
        Flag("bots", 'b', "Only clean messages sent by a bot")
)
@Perm(Permission.MESSAGE_MANAGE)
class Prune : AsyncCommand() {
    override val desc = "Prune messages."

    override suspend fun asyncRun(ctx: Context) {
        val history = ctx.channel.iterableHistory.await()
        val toClean = min(50, ctx.args["messages"] as Int)
        var messages = 0

        (0 until toClean)
                .map { history[it] }
                .filterNot {
                    (ctx.flags.argMap.containsKey("bots") || ctx.flags.argMap.containsKey("b")) && !it.author.isBot
                }
                .forEach {
                    it.delete().await()
                    messages++
                }

        ctx.send(
                I18n.parse(
                        ctx.lang.getString("pruned_messages"),
                        mapOf("num" to messages)
                )
        )
    }
}