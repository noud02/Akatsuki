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
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.*
import moe.kyubey.akatsuki.utils.I18n
import moe.kyubey.akatsuki.db.schema.Users
import org.jetbrains.exposed.sql.update

@Alias("change")
@Argument("lang", "string")
class SetLocale : Command() {
    override val name = "set"
    override val desc = "Set your language"

    override fun run(ctx: Context) {
        asyncTransaction(Akatsuki.pool) {
            val language = I18n.langToCode(ctx.args["lang"] as String)

            Users.update({
                Users.id.eq(ctx.author.idLong)
            }) {
                it[lang] = language
            }

            ctx.send(
                    I18n.parse(
                            ctx.lang.getString("language_set"),
                            mapOf(
                                    "language" to java.util.Locale(language.split("_")[0], language.split("_")[1]).displayLanguage
                            )
                    )
            )
        }.execute()
    }
}

@Load
class Locale : Command() {
    override val desc = "View or change your language"

    init {
        addSubcommand(SetLocale())
    }

    override fun run(ctx: Context) = ctx.send(
            I18n.parse(
                    ctx.lang.getString("language_check"),
                    mapOf("language" to ctx.lang.locale.displayLanguage)
            )
    )
}