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

package moe.kyubey.akatsuki.utils

import moe.kyubey.akatsuki.Akatsuki
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import kotlin.reflect.jvm.jvmName

enum class WolkType(val str: String) {
    AWOO("awoo"),
    BANG("bang"),
    BLUSH("blush"),
    CLAGWIMOTH("clagwimoth"),
    CRY("cry"),
    CUDDLE("cuddle"),
    DANCE("dance"),
    HUG("hug"),
    INSULT("insult"),
    JOJO("jojo"),
    KISS("kiss"),
    LEWD("lewd"),
    LICK("lick"),
    MEGUMIN("megumin"),
    NEKO("neko"),
    NOM("nom"),
    OWO("owo"),
    PAT("pat"),
    POKE("poke"),
    POUT("pout"),
    REM("rem"),
    SHRUG("shrug"),
    SLAP("slap"),
    SLEEPY("sleepy"),
    SMILE("smile"),
    TEEHEE("teehee"),
    SMUG("smug"),
    STARE("stare"),
    THUMBSUP("thumbsup"),
    TRIGGERED("triggered"),
    WAG("wag"),
    WAIFU_INSULT("waifu_insult"),
    WASTED("wasted"),
    SUMFUK("sumfuk"),
    DAB("dab"),
    TICKLE("tickle"),
    HIGHFIVE("highfive"),
    BANGHEAD("banghead"),
    BITE("bite"),
    DISCORD_MEMES("discord_memes"),
    NANI("nani"),
    INITIAL_D("initial_d"),
    DELET_THIS("delet_this"),
    POI("poi"),
    THINKING("thinking"),
    GREET("greet")
}

data class WolkTag(
        val name: String,
        val hidden: Boolean,
        val user: String
)

data class WolkResponse(
        val id: String,
        val baseType: String,
        val fileType: String,
        val mimeType: String,
        val account: String,
        val hidden: Boolean,
        val nsfw: Boolean,
        val tags: List<WolkTag>,
        val url: String
)

object Wolk {
    private val logger = Logger(this::class.jvmName)
    private var token: String? = null
    private var wolkeToken = true

    fun setToken(token: String, wolkeToken: Boolean = true): Wolk {
        Wolk.token = token
        Wolk.wolkeToken = wolkeToken
        return this
    }

    fun getByType(type: WolkType) = getByType(type.str)

    fun getByType(type: String): CompletableFuture<WolkResponse> {
        val fut = CompletableFuture<WolkResponse>()

        if (token.isNullOrBlank()) {
            fut.completeExceptionally(Exception("Invalid or no token"))
            return fut
        }

        Http.get("https://api.weeb.sh/images/random?type=$type") {
            addHeader("User-Agent", "Akatsuki (https://github.com/yuwui/Akatsuki)")
            addHeader("Authorization", if (wolkeToken) "Wolke $token" else "Bearer $token")
        }.thenAccept { res ->
            val json = JSONObject(res.body()!!.string())
            val tags = mutableListOf<WolkTag>()

            if (res.code() != 200) {
                throw Exception("Expected status code 200, got ${res.code()}")
            }

            (0 until json.getJSONArray("tags").length())
                    .map {
                        json
                                .getJSONArray("tags")
                                .getJSONObject(it)
                    }
                    .forEach {
                        tags += WolkTag(
                                it.getString("name"),
                                it.getBoolean("hidden"),
                                it.getString("user")
                        )
                    }

            res.close()

            fut.complete(
                    WolkResponse(
                            json.getString("id"),
                            json.getString("baseType"),
                            json.getString("fileType"),
                            json.getString("mimeType"),
                            json.getString("account"),
                            json.getBoolean("hidden"),
                            json.getBoolean("nsfw"),
                            tags,
                            json.getString("url")
                    )
            )
        }.thenApply {}.exceptionally { fut.completeExceptionally(it) }

        return fut
    }
}
