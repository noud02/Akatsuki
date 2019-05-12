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

import moe.kyubey.akatsuki.annotations.Alias
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.entities.AsyncCommand
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.extensions.await
import moe.kyubey.akatsuki.utils.I18n
import kotlin.math.min

@Load
@Argument("messages", "number", true)
@Alias("clear", "cls")
class Clean : AsyncCommand() {
    override val desc = "Clean the last 10 messages sent by me"
    override val guildOnly = true
    override val cooldown = 15

    override suspend fun asyncRun(ctx: Context) {
        val msgs = ctx.channel.getHistoryAround(ctx.msg, 100).await()

        val botmsgs = msgs.retrievedHistory.filter { it.author.id == ctx.selfMember!!.user.id }
        val sub = botmsgs.subList(0, min(botmsgs.size, min(ctx.args.getOrDefault("messages", 10) as Int, 10)))
        sub.forEach { it.delete().await() }


        ctx.send(
                I18n.parse(
                        ctx.lang.getString("cleaned_messages"),
                        mapOf("num" to sub.size)
                )
        )
    }
}