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

import com.joestelmach.natty.Parser
import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.Akatsuki
import me.noud02.akatsuki.annotations.Alias
import me.noud02.akatsuki.annotations.Argument
import me.noud02.akatsuki.annotations.Load
import me.noud02.akatsuki.db.schema.Reminders
import me.noud02.akatsuki.entities.Command
import me.noud02.akatsuki.entities.Context
import org.jetbrains.exposed.sql.insert

@Load
@Argument("reminder", "string")
@Alias("remind")
class RemindMe : Command() {
    override fun run(ctx: Context) {
        val parser = Parser()
        val parsed = parser.parse(ctx.args["reminder"] as String).getOrNull(0) ?: return ctx.send("I didn't get that, mind saying that again?")
        val what = parsed.fullText.replace(parsed.text, "").trim()
        val date = parsed.dates.getOrNull(0) ?: return ctx.send("Please specify a date!")

        asyncTransaction(Akatsuki.instance.pool) {
            Reminders.insert {
                it[userId] = ctx.author.idLong
                it[channelId] = ctx.channel.idLong
                it[timestamp] = date.time / 1000
                it[reminder] = what
            }

            ctx.send("OK! I'll remind you $what at $date")
        }.execute()
    }
}