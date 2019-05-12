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
import moe.kyubey.akatsuki.entities.Command
import moe.kyubey.akatsuki.entities.Context
import moe.kyubey.akatsuki.annotations.Load
import moe.kyubey.akatsuki.utils.Http
import org.json.JSONObject

@Load
class Awwnime : Command() {
    override val desc = "Get a random post from /r/awwnime"

    override fun run(ctx: Context) {
        Http.get("https://www.reddit.com/r/awwnime/random.json").thenAccept { res ->
            val json = JSONObject(res.body()!!.string())

            val posts = json
                    .getJSONObject("data")
                    .getJSONArray("children")

            val post = posts
                    .getJSONObject(Math.floor(Math.random() * posts.count()).toInt())
                    .getJSONObject("data")

            ctx.send(post.getString("url"))
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get random awwnime post from reddit", it)
            ctx.sendError(it)
            Sentry.capture(it)
        }
    }
}