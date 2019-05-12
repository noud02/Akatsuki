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

import com.joestelmach.natty.Parser
import me.aurieh.ares.exposed.async.asyncTransaction
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.annotations.Alias
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.db.schema.Reminders
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.I18n
import org.jetbrains.exposed.sql.insert

// @Load
@Argument("reminder", "string")
@Alias("remind")
class RemindMe : Command() {
    override val desc = "Set reminders!"

    override fun run(ctx: Context) {
        val parser = Parser()
        val parsed = parser.parse(ctx.args["reminder"] as String).getOrNull(0)
                ?: return ctx.send(
                        I18n.parse(
                                ctx.lang.getString("reminder_err"),
                                mapOf("username" to ctx.author.name)
                        )
                )

        val what = parsed.fullText.replace(parsed.text, "").trim()
        val date = parsed.dates.getOrNull(0)
                ?: return ctx.send(
                        I18n.parse(
                                ctx.lang.getString("reminder_specify_date"),
                                mapOf("username" to ctx.author.name)
                        )
                )

        asyncTransaction(Akatsuki.pool) {
            Reminders.insert {
                it[userId] = ctx.author.idLong
                it[channelId] = ctx.channel.idLong
                it[timestamp] = date.time
                it[reminder] = what
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("reminder_set"),
                            mapOf(
                                    "what" to what,
                                    "date" to date
                            )
                    )
            )
        }.execute()
    }
}