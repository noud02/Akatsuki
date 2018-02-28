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

package me.noud02.akatsuki

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import me.noud02.akatsuki.entities.*
import java.io.File

fun main (args: Array<String>) {
    val mapper = ObjectMapper(YAMLFactory())

    mapper.registerModule(KotlinModule())

    val config: Config

    if (System.getenv("USE_ENV") != null || System.getenv("DYNO") != null)
        config = Config(
                System.getenv("BOT_TOKEN"),
                System.getenv("BOT_DESCRIPTION"),
                System.getenv("BOT_OWNERS").split("::"),
                System.getenv("BOT_PREFIXES").split("::"),
                System.getenv("BOT_GAMES").split("::").map {
                    val gameRegex = "game=([^$]+)".toRegex()
                    val typeRegex = "type=([^$]+)".toRegex()

                    val game = gameRegex.find(it)?.groupValues?.get(1) ?: ""
                    val type = typeRegex.find(it)?.groupValues?.get(1) ?: "playing"

                    PresenceConfig(game, type)
                },
                if (System.getenv("DATABASE_URL") != null) {
                    val pgUrl = System.getenv("DATABASE_URL").removePrefix("postgres://")

                    DatabaseConfig(
                            pgUrl.split("/")[1],
                            pgUrl.split(":")[0],
                            pgUrl.split(":")[1].split("@")[0],
                            pgUrl.split("@")[1].split("/")[0]
                    )
                } else
                    DatabaseConfig(
                            System.getenv("DATABASE_NAME"),
                            System.getenv("DATABASE_USER"),
                            System.getenv("DATABASE_PASS"),
                            System.getenv("DATABASE_HOST")
                    ),
                APIConfig(
                        System.getenv("GOOGLE_API_KEY"),
                        System.getenv("WEEBSH_API_KEY"),
                        System.getenv("DISCORDBOTS_API_KEY"),
                        System.getenv("DISCORDBOTSORG_API_KEY"),
                        "${System.getenv("MAL_USERNAME")}:${System.getenv("MAL_PASSWORD")}",
                        System.getenv("SENTRY_DSN"),
                        System.getenv("OSU_API_KEY")
                ),
                SiteConfig(
                        System.getenv("SITE_HOST"),
                        System.getenv("SITE_SSL").toBoolean(),
                        System.getenv("SITE_PORT").toInt()
                ),
                if (System.getenv("DYNO") != null)
                    BackendConfig(
                            "${System.getenv("HEROKU_APP_NAME")}.herokuapp.com",
                            false,
                            System.getenv("PORT").toInt()
                    )
                else
                    BackendConfig(
                            System.getenv("BACKEND_HOST"),
                            System.getenv("BACKEND_SSL").toBoolean(),
                            System.getenv("BACKEND_PORT").toInt()
                    )
        )
    else
        config = mapper.readValue(File("./config.yml"))

    val bot = Akatsuki(config)

    bot.build(0, 2, 3)
    //bot.build()
}
