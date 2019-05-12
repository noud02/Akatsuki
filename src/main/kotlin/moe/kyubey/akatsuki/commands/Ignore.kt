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
import moe.kyubey.akatsuki.db.schema.Guilds
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import org.jetbrains.exposed.sql.update

@Load
@Argument("channel", "textchannel", true)
@Perm(Permission.MANAGE_SERVER)
class Ignore : Command() {
    override val guildOnly = true
    override val desc = "Have the bot ignore channels"

    override fun run(ctx: Context) {
        val channel = ctx.args.getOrDefault("channel", ctx.channel) as TextChannel

        if (ctx.storedGuild!!.ignoredChannels.contains(channel.idLong)) {
            return ctx.send(ctx.lang.getString("channel_already_ignored"))
        }

        asyncTransaction(Akatsuki.pool) {
            Guilds.update({
                Guilds.id.eq(ctx.guild!!.idLong)
            }) {
                it[ignoredChannels] = ctx.storedGuild.ignoredChannels.plus(channel.idLong).toTypedArray()
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("ignored_channel"),
                            mapOf("channel" to channel.asMention)
                    )
            )
        }.execute()
    }
}