/*
 *  Copyright (c) 2017 Noud Kerver
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

package me.noud02.akatsuki.commands

import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.Akatsuki
import me.noud02.akatsuki.annotations.Alias
import me.noud02.akatsuki.annotations.Argument
import me.noud02.akatsuki.annotations.Arguments
import me.noud02.akatsuki.annotations.Load
import me.noud02.akatsuki.db.schema.Tags
import me.noud02.akatsuki.entities.Command
import me.noud02.akatsuki.entities.Context
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

@Load
@Arguments(
        Argument("name", "string"),
        Argument("content", "string")
)
@Alias("createpasta", "createahh")
class CreateTag : Command() {
    override val desc = "Create tags"

    override fun run(ctx: Context) {
        val name = ctx.args["name"] as String
        val content = ctx.args["content"] as String

        asyncTransaction(Akatsuki.instance.pool) {
            if (Tags.select { Tags.tagName.eq(name) }.firstOrNull() != null)
                return@asyncTransaction ctx.send("That tag already exists!")

            Tags.insert {
                it[tagName] = name
                it[ownerId] = ctx.author.idLong
                it[tagContent] = content
            }

            ctx.send("Tag with name '$name' created!")
        }.execute()
    }
}