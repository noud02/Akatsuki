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

package moe.kyubey.akatsuki.entities

import io.sentry.Sentry
import moe.kyubey.akatsuki.utils.Wolk
import moe.kyubey.akatsuki.utils.WolkType
import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color

abstract class WolkCommand : Command() {
    abstract val type: WolkType

    override fun run(ctx: Context) {
        Wolk.getByType(type).thenAccept { res ->
            ctx.send(EmbedBuilder().apply {
                setImage(res.url)
                setColor(Color.CYAN)
                setFooter("Powered by weeb.sh", null)
            }.build())
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get weebsh image", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}