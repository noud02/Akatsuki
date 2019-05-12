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
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.utils.Http
import net.dv8tion.jda.core.EmbedBuilder
import org.json.JSONObject

@Load
class Info : Command() {
    override val desc = "Get info on the bot."

    override fun run(ctx: Context) {
        Http.get("https://api.github.com/repos/Kyuubey/Akatsuki").thenAccept { res ->
            val embed = EmbedBuilder().apply {

                val body = res.body()!!.string()
                val json = JSONObject(body)
                val stars = json.getInt("stargazers_count")
                val issues = json.getInt("open_issues")
                val forks = json.getInt("forks_count")

                setTitle(ctx.jda.selfUser.name)

                descriptionBuilder.append("This bot runs on [Akatsuki](https://github.com/Kyuubey/Akatsuki), an open-source Discord bot written in Kotlin.\n")
                descriptionBuilder.append("\n${Akatsuki.config.description}\n\n")
                descriptionBuilder.append("[Bugs](https://github.com/Kyuubey/Akatsuki/issues) | [GitHub](https://github.com/Kyuubey/Akatsuki)\n")
                descriptionBuilder.append("\n\u2B50 $stars | \u26A0 $issues | \uD83C\uDF74 $forks\n")
                res.close()
            }

            ctx.send(embed.build())
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get github repo info", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}
