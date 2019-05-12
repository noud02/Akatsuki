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

import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.annotations.Argument
import moe.kyubey.akatsuki.annotations.Flag
import moe.kyubey.akatsuki.annotations.Flags
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import net.dv8tion.jda.core.EmbedBuilder
import org.apache.http.client.utils.URIBuilder
import java.awt.Color

@Load
@Argument("logs", "number", true)
@Flags(
        Flag("event", 'e', "Event type, can be DELETE, UPDATE or CREATE."),
        Flag("delete", 'd', "Sets event type to DELETE."),
        Flag("create", 'c', "Sets event type to CREATE."),
        Flag("update", 'u', "Sets event type to UPDATE.")
)
class Logs : Command() {
    override val desc = "Get message logs of the channel."

    override fun run(ctx: Context) {
        val num = ctx.args.getOrDefault("logs", 100)
        val url = URIBuilder().apply {
            scheme = if (Akatsuki.config.site.ssl) "https" else "http"
            host = Akatsuki.config.site.host
            if (Akatsuki.config.site.port != 80) {
                port = Akatsuki.config.site.port
            }
            path = "/logs/${ctx.channel.id}/${ctx.msg.creationTime.toInstant().toEpochMilli()}"

            when {
                ctx.flags.argMap.contains("event") || ctx.flags.argMap.contains("e") ->
                    addParameter(
                            "event",
                            ctx.flags.argMap.getOrDefault(
                                    "event",
                                    ctx.flags.argMap.getOrDefault(
                                            "e",
                                            "ALL"
                                    )
                            )
                    )

                ctx.flags.argMap.contains("delete") || ctx.flags.argMap.contains("d") ->
                    addParameter("event", "DELETE")

                ctx.flags.argMap.contains("create") || ctx.flags.argMap.contains("c") ->
                    addParameter("event", "CREATE")

                ctx.flags.argMap.contains("update") || ctx.flags.argMap.contains("u") ->
                    addParameter("event", "UPDATE")
            }

            addParameter("limit", num.toString())
        }
        val embed = EmbedBuilder().apply {
            setTitle("Logs for #${ctx.channel.name} ($num)", url.build().toString())
            setColor(Color.CYAN)
        }

        ctx.send(embed.build())
    }
}