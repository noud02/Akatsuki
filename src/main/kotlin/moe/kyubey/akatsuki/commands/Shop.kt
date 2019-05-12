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
import moe.kyubey.akatsuki.db.schema.Contracts
import moe.kyubey.akatsuki.db.schema.Items
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import net.dv8tion.jda.core.EmbedBuilder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import kotlin.math.min

@Arguments(
        Argument("id", "string"),
        Argument("type", "string"),
        Argument("price", "number"),
        Argument("item", "string"),
        Argument("description", "string", true)
)
class AddItem : Command() {
    override val ownerOnly = true

    override fun run(ctx: Context) {
        val itemId = ctx.args["id"] as String
        val itemType = ctx.args["type"] as String
        val itemPrice = ctx.args["price"] as Int
        val itemContent = ctx.args["item"] as String
        val itemDesc = ctx.args.getOrDefault("description", "") as String

        asyncTransaction(Akatsuki.pool) {
            if (Items.select { Items.id.eq(itemId) }.firstOrNull() != null) {
                return@asyncTransaction ctx.send("Item with that ID already exists!") // TODO add translation
            }

            Items.insert {
                it[id] = itemId
                it[type] = itemType
                it[price] = itemPrice
                it[content] = itemContent
                it[description] = itemDesc
            }

            ctx.send("Added item **$itemContent (#$itemId)**") // TODO add translation
        }.execute()
    }
}

@Arguments(
        Argument("item", "string"),
        Argument("amount", "number", true)
)
class BuyItem : Command() {
    override val desc = "Buy items from the shop."

    override fun run(ctx: Context) {
        val id = ctx.args["item"] as String
        val amount = ctx.args.getOrDefault("amount", 1) as Int

        asyncTransaction(Akatsuki.pool) {
            val contract = Contracts.select { Contracts.userId.eq(ctx.author.idLong) }.firstOrNull()
                    ?: return@asyncTransaction ctx.send("You haven't made a contract with me yet!") // TODO add translation
            val item = Items.select { Items.id.eq(id) }.firstOrNull()
                    ?: return@asyncTransaction ctx.send("Couldn't find that item!") // TODO add translation

            val toPay = item[Items.price] * amount

            if (contract[Contracts.balance] < toPay) {
                return@asyncTransaction ctx.send("Insufficient balance!") // TODO add translation
            }

            val inv = contract[Contracts.inventory].toMutableList()

            for (i in 0 until amount) {
                inv += item[Items.id]
            }

            Contracts.update({
                Contracts.userId.eq(ctx.author.idLong)
            }) {
                it[balance] = contract[Contracts.balance] - toPay
                it[inventory] = inv.toTypedArray()
            }

            ctx.send("You bought **${amount}x ${item[Items.content]}** for **$toPay$**!") // TODO add translation
        }.execute()
    }
}

@Argument("item", "string")
class SearchItem : Command() {
    override val desc = "Search items in the shop."

    override fun run(ctx: Context) {
        val search = ctx.args["item"] as String

        asyncTransaction(Akatsuki.pool) {
            val matches = Items.selectAll().filter { it[Items.id].indexOf(search) > -1 }

            if (matches.isEmpty()) {
                return@asyncTransaction ctx.send("Couldn't find any items matching your query!") // TODO add translation
            }

            val items = matches.subList(0, min(matches.size, 50))

            ctx.send(EmbedBuilder().apply {
                setTitle("Items \uD83D\uDD0D") // TODO add translation
                descriptionBuilder.append(items.joinToString(", ") { "${it[Items.content]} [${it[Items.price]}$] (#${it[Items.id]})" })
            }.build())
        }.execute()
    }
}

@Load
class Shop : Command() {
    override val desc = "Buy things from the shop!"

    init {
        addSubcommand(AddItem(), "add")
        addSubcommand(BuyItem(), "buy")
        addSubcommand(SearchItem(), "search")
    }

    override fun run(ctx: Context) {
        asyncTransaction(Akatsuki.pool) {
            val items = Items.selectAll().limit(15)

            val embed = EmbedBuilder().apply {
                setTitle("Shop")
                descriptionBuilder.append("Buy items here!")

                addField(
                        "New items",
                        items.joinToString(", ") { "${it[Items.content]} [${it[Items.price]}$] (#${it[Items.id]})" },
                        false
                )
            }

            ctx.send(embed.build())
        }.execute()
    }
}