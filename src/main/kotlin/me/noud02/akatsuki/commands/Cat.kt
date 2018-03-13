/*
 *   Copyright (c) 2017-2018 Noud Kerver
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

package me.noud02.akatsuki.commands

import io.sentry.Sentry
import me.noud02.akatsuki.entities.Command
import me.noud02.akatsuki.entities.Context
import me.noud02.akatsuki.annotations.Load
import me.noud02.akatsuki.utils.Http
import org.json.JSONObject

@Load
class Cat : Command() {
    override val desc = "Get a random cat"

    override fun run(ctx: Context) {
        // TODO switch back when random.cat works again
        /*Http.get("https://random.cat/meow").thenAccept { res ->
            val json = JSONObject(res.body()!!.string())

            ctx.send(json.getString("file"))
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get a random cat from random.cat", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }*/

        Http.get("https://nekos.life/api/v2/img/meow").thenAccept { res ->
            val json = JSONObject(res.body()!!.string())

            ctx.send(json.getString("url"))
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get a random cat from nekos.life", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}