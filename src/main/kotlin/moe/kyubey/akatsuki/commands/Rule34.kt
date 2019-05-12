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

import io.sentry.Sentry
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.utils.Http
import moe.kyubey.akatsuki.utils.I18n
import net.dv8tion.jda.core.EmbedBuilder
import okhttp3.HttpUrl
import org.json.XML
import java.awt.Color

@Load
@Argument("tags", "string")
class Rule34 : Command() {
    override val nsfw = true
    override val desc = "Search for (nsfw) images on rule34"

    override fun run(ctx: Context) {
        val tags = ctx.args["tags"] as String

        if (tags.indexOf("loli") > -1) {
            return ctx.send(I18n.parse(ctx.lang.getString("loli_is_illegal"), mapOf("username" to ctx.author.name)))
        }

        Http.get(HttpUrl.Builder().apply {
            scheme("https")
            host("rule34.xxx")
            addPathSegment("index.php")
            addQueryParameter("page", "dapi")
            addQueryParameter("s", "post")
            addQueryParameter("q", "index")
            addQueryParameter("tags", tags)
        }.build()).thenAccept { res ->
            val posts = XML
                    .toJSONObject(res.body()!!.string())
                    .getJSONObject("posts")
                    .optJSONArray("post") ?: return@thenAccept ctx.send(
                    I18n.parse(
                            ctx.lang.getString("no_images_found"),
                            mapOf("username" to ctx.author.name)
                    )
            )

            if (posts.count() == 0) {
                return@thenAccept ctx.send(
                        I18n.parse(
                                ctx.lang.getString("no_images_found"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            val post = posts.getJSONObject(Math.floor(Math.random() * posts.length()).toInt())

            if (post.getString("tags").indexOf("loli") > -1) {
                return@thenAccept ctx.send(
                        I18n.parse(
                                ctx.lang.getString("loli_is_illegal"),
                                mapOf("username" to ctx.author.name)
                        )
                )
            }

            val embed = EmbedBuilder().apply {
                setImage(post.getString("file_url"))
                setColor(Color(170, 229, 16))
            }

            ctx.send(embed.build())
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get post from rule34", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}