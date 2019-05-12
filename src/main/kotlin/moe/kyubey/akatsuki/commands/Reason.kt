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
import moe.kyubey.akatsuki.annotations.Arguments
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.annotations.Perm
import moe.kyubey.akatsuki.db.schema.Modlogs
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.Permission
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

@Load
@Perm(Permission.MANAGE_SERVER)
@Arguments(
        Argument("case", "string"),
        Argument("reason", "string")
)
class Reason : Command() {
    override val guildOnly = true
    override val desc = "Give a reason for a case in modlogs."

    override fun run(ctx: Context) {
        if (ctx.storedGuild!!.modlogChannel == null) {
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("no_modlog_channel"),
                            mapOf("username" to ctx.author.name)
                    )
            )
        }

        val reasonArg = ctx.args["reason"] as String

        if (reasonArg.length > 512) {
            return ctx.send(
                    I18n.parse(
                            ctx.lang.getString("reason_too_long"),
                            mapOf("username" to ctx.author.name)
                    )
            )
        }

        val caseArg = (ctx.args["case"] as String).toLowerCase()

        asyncTransaction(Akatsuki.pool) {
            val cases = Modlogs.select { Modlogs.guildId eq ctx.guild!!.idLong }
            val caseIds: List<Int> = when (caseArg) {
                "l" -> listOf(cases.count())
                else -> {
                    if (caseArg.toIntOrNull() == null) {
                        if (!caseArg.matches("\\d+\\.\\.\\d+".toRegex())) {
                            return@asyncTransaction
                        }

                        val first = caseArg.split("..")[0].toInt()
                        val second = caseArg.split("..")[1].toInt()

                        if (first > second) {
                            return@asyncTransaction ctx.send(
                                    I18n.parse(
                                            ctx.lang.getString("case_num_err"),
                                            mapOf("username" to ctx.author.name)
                                    )
                            )
                        }

                        var list = listOf<Int>()

                        for (i in first..second) {
                            list += i
                        }

                        list
                    } else
                        listOf(caseArg.toInt())
                }
            }

            for (id in caseIds) {
                Modlogs.update({ Modlogs.guildId.eq(ctx.guild!!.idLong) and Modlogs.caseId.eq(id) }) { it[reason] = reasonArg }
                val log = cases.firstOrNull { it[Modlogs.caseId] == id }

                if (log != null) {
                    ctx.guild!!
                            .getTextChannelById(ctx.storedGuild.modlogChannel ?: return@asyncTransaction)
                            .getMessageById(log[Modlogs.messageId])
                            .queue({
                                it.editMessage(
                                        it.contentRaw.replace(
                                                "\\*\\*Reason\\*\\*: .+\n\\*\\*Responsible moderator\\*\\*: .+".toRegex(),
                                                "**Reason**: $reasonArg\n" +
                                                        "**Responsible moderator**: ${ctx.author.name}#${ctx.author.discriminator} (${ctx.author.id})"
                                        )
                                ).queue()
                            }) {
                                ctx.sendError(it)
                                it.printStackTrace()
                            }
                }
            }

            ctx.send("\uD83D\uDC4C")
        }.execute()
    }
}