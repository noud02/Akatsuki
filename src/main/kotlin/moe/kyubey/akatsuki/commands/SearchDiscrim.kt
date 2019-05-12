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
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.*
import moe.kyubey.akatsuki.utils.I18n

@Load
@Alias("discrim")
@Argument("discrim", "string", true)
class SearchDiscrim : Command() {
    override val desc = "Search for users with a specific discriminator."

    override fun run(ctx: Context) {
        val discrim = ctx.args["discrim"] as? String ?: ctx.author.discriminator
        val users = ctx.jda.users.filter { it.discriminator == discrim }
        val list = users.subList(0, Math.min(5, users.size)).joinToString("\n") { "\t${it.name} (${it.id})" }
        
        ctx.send(
                I18n.parse(
                        ctx.lang.getString("users_with_discrim"),
                        mapOf("discriminator" to discrim)
                )
                        + "\n$list"
        )
    }
}