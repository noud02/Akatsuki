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
import me.noud02.akatsuki.Akatsuki
import me.noud02.akatsuki.annotations.Argument
import me.noud02.akatsuki.annotations.Load
import me.noud02.akatsuki.entities.Command
import me.noud02.akatsuki.entities.Context
import me.noud02.akatsuki.utils.Http
import okhttp3.HttpUrl

@Load
@Argument("step 1 | step 2 | step 3", "string")
class Plan : Command() {
    override fun run(ctx: Context) {
        val stepArg = ctx.args["step 1 | step 2 | step 3"] as String
        val steps = stepArg.split("\\s?\\|\\s?".toRegex())

        if (steps.size < 3) {
            return ctx.send("Not enough steps!") // TODO translation
        }

        val step1 = steps[0]
        val step2 = steps[1]
        val step3 = steps[2]

        Http.get(HttpUrl.Builder().apply {
            scheme(if (Akatsuki.config.backend.ssl) "https" else "http")
            host(Akatsuki.config.backend.host)
            port(Akatsuki.config.backend.port)
            addPathSegments("api/plan")
            addQueryParameter("step1", step1)
            addQueryParameter("step2", step2)
            addQueryParameter("step3", step3)
        }.build()).thenAccept { res ->
            ctx.channel.sendFile(res.body()!!.bytes(), "plan.png").queue()
            res.close()
        }.thenApply {}.exceptionally {
            ctx.logger.error("Error while trying to get plan image from backend", it)
            Sentry.capture(it)
            ctx.sendError(it)
        }
    }
}