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
import moe.kyubey.akatsuki.EventListener
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.db.schema.Contracts
import moe.kyubey.akatsuki.db.schema.Items
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.extensions.createContract
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.select

@Argument("user", "user", true)
class ViewContract : Command() {
    override val guildOnly = true
    override val desc = "View someone's contract!"

    override fun run(ctx: Context) {
        val member = ctx.args.getOrDefault("user", ctx.member!!) as Member

        asyncTransaction(Akatsuki.pool) {
            val contract = Contracts.select { Contracts.userId.eq(member.user.idLong) }.firstOrNull()
                    ?: return@asyncTransaction ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("user_no_contract"),
                                    mapOf(
                                            "username" to ctx.author.name,
                                            "user" to member.user.name
                                    )
                            )
                    )

            ctx.send(EmbedBuilder().apply {
                setTitle(
                        I18n.parse(
                                ctx.lang.getString("contract_title"),
                                mapOf("username" to member.user.name)
                        )
                )
                descriptionBuilder.append("*${contract[Contracts.wish]}*")

                val corruption = contract[Contracts.corruption]
                val xp = contract[Contracts.experience]
                val level = contract[Contracts.level]

                val xpNeeded = level.toFloat() * 500f * (level.toFloat() / 3f)
                val progress = xp.toFloat() / xpNeeded * 10f

                val itemMap = mutableMapOf<String, Int>()

                contract[Contracts.inventory].forEach {
                    val item = Items.select { Items.id.eq(it) }.firstOrNull() ?: return@forEach
                    val content = item[Items.content]

                    if (itemMap.containsKey(content))
                        itemMap[content] = itemMap[content]!! + 1
                    else
                        itemMap[content] = 1
                }

                // TODO add translations for these
                addField(
                        "Stats",
                        """**Rank:** ${contract[Contracts.level]}
                            |**Progress:** [${"#".repeat(progress.toInt())}${"-".repeat(10 - progress.toInt())}] ${progress.toInt() * 10}%
                            |**Balance:** ${contract[Contracts.balance]}$
                         """.trimMargin(),
                        true
                )

                addField(
                        "Corruption",
                        "[${"#".repeat(if (corruption != 0) corruption / 10 else 0)}${"-".repeat(10 - corruption)}] $corruption%",
                        true
                )

                addField(
                        "Inventory",
                        itemMap.map { "${it.value}x ${it.key}" }.joinToString(", "),
                        false
                )

                setFooter(contract[Contracts.date].toString(), null)
            }.build())
        }.execute()
    }
}

@Load
class Contract : Command() {
    override val desc = "\"I will make you a magical girl!\""

    init {
        addSubcommand(ViewContract(), "view")
    }

    override fun run(ctx: Context) {
        asyncTransaction(Akatsuki.pool) {
            if (!Contracts.select { Contracts.userId.eq(ctx.author.idLong) }.empty())
                return@asyncTransaction ctx.send(
                        I18n.parse(
                                ctx.lang.getString("user_has_contract"),
                                mapOf("username" to ctx.author.name)
                        )
                )

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("ask_wish"),
                            mapOf("username" to ctx.author.name)
                    )
            )

            EventListener.waiter.await<MessageReceivedEvent>(1, 60000L) {
                if (it.author.id == ctx.author.id && it.channel.id == ctx.channel.id) {
                    val ans = it.message.contentRaw

                    if (ans.toLowerCase().startsWith("cancel"))
                        return@await true

                    ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("ask_sure_mg"),
                                    mapOf("username" to ctx.author.name)
                            )
                    )

                    EventListener.waiter.await<MessageReceivedEvent>(1, 60000L) {
                        if (it.author.id == ctx.author.id && it.channel.id == ctx.channel.id) {
                            val ans2 = it.message.contentRaw

                            @Suppress("LABEL_NAME_CLASH")
                            if (!ans2.toLowerCase().startsWith("y"))
                                return@await true

                            ctx.author.createContract(ans)

                            ctx.send(
                                    I18n.parse(
                                            ctx.lang.getString("mg_wish_granted"),
                                            mapOf("username" to ctx.author.name)
                                    )
                            )

                            true
                        } else
                            false
                    }

                    true
                } else
                    false
            }
        }.execute()
    }
}