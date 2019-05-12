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
import moe.kyubey.akatsuki.annotations.Alias
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Arguments
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.db.schema.Tags
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.I18n
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

@Load
@Arguments(
        Argument("name", "string"),
        Argument("content", "string")
)
@Alias("editpasta", "editahh")
class EditTag : Command() {
    override val desc = "Edit tags"

    override fun run(ctx: Context) {
        val name = ctx.args["name"] as String
        val content = ctx.args["content"] as String

        asyncTransaction(Akatsuki.pool) {
            val tag = Tags.select { Tags.tagName.eq(name) }.firstOrNull()
                    ?: return@asyncTransaction ctx.send(
                            I18n.parse(
                                    ctx.lang.getString("tag_not_found"),
                                    mapOf("username" to ctx.author.name)
                            )
                    )

            if (tag[Tags.ownerId] != ctx.author.idLong) {
                return@asyncTransaction ctx.send(
                        I18n.parse(
                                ctx.lang.getString("tag_not_owner"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            Tags.update({
                Tags.tagName.eq(name)
            }) {
                it[tagContent] = content
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("tag_edited"),
                            mapOf("name" to name)
                    )
            )
        }.execute()
    }
}