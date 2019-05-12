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

package moe.kyubey.akatsuki.entities

import me.aurieh.ares.utils.ArgParser
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.db.DBGuild
import moe.kyubey.akatsuki.db.DBUser
import moe.kyubey.akatsuki.utils.I18n
import moe.kyubey.akatsuki.utils.Logger
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.InputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.reflect.jvm.jvmName

class Context(
        val event: MessageReceivedEvent,
        val cmd: Command,
        val args: MutableMap<String, Any>,
        val rawArgs: List<String>,
        val flags: ArgParser.ParsedResult,
        val perms: MutableMap<String, Boolean>,
        val lang: ResourceBundle,
        val storedUser: DBUser,
        val storedGuild: DBGuild?
) {
    val logger = Logger(cmd::class.jvmName)

    val jda: JDA = event.jda
    val guild: Guild? = event.guild
    val author: User = event.author
    val channel: MessageChannel = event.channel
    val msg: Message = event.message
    val member: Member? = event.member
    val selfMember: Member? = event.guild?.selfMember

    fun send(arg: String) = event.channel.sendMessage(arg).queue()
    fun send(arg: MessageEmbed) = event.channel.sendMessage(arg).queue()

    fun sendCode(lang: String, arg: Any) = event.channel.sendMessage("```$lang\n$arg```").queue()

    fun sendError(e: Throwable) = event.channel.sendMessage(
            I18n.parse(
                    lang.getString("error"),
                    mapOf("error" to e)
            )
    ).queue()

    fun getLastImage(): CompletableFuture<InputStream?> {
        val fut = CompletableFuture<InputStream?>()

        channel.history.retrievePast(25).queue({
            val history = it.filter { it.attachments.isNotEmpty() && it.attachments[0].isImage }

            if (history.isEmpty()) {
                fut.complete(null)
            } else {
                fut.complete(history[0].attachments[0].inputStream)
            }
        }) {
            fut.complete(null)
        }

        return fut
    }
}