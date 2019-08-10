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

package moe.kyubey.akatsuki

import io.sentry.Sentry
import lavalink.client.io.jda.JdaLavalink
import me.aurieh.ares.exposed.async.asyncTransaction
import moe.kyubey.akatsuki.db.schema.*
import moe.kyubey.akatsuki.entities.Config
import moe.kyubey.akatsuki.entities.CoroutineDispatcher
import moe.kyubey.akatsuki.utils.Logger
import moe.kyubey.akatsuki.utils.Wolk
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import org.jetbrains.exposed.sql.*
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.reflect.jvm.jvmName

class Akatsuki(private val config: Config) {
    init {
        Akatsuki.config = config

        if (config.api.sentry.isNotEmpty()) {
            Sentry.init(config.api.sentry)
        }

        Database.connect(
                "jdbc:postgresql://${config.database.host}/${config.database.name}",
                "org.postgresql.Driver",
                config.database.user,
                config.database.pass
        )

        Wolk.setToken(config.api.weebsh)

        asyncTransaction(pool) {
            SchemaUtils.create(
                    Guilds,
                    Users,
                    Starboard,
                    Logs,
                    Modlogs,
                    Contracts,
                    Tags,
                    Reminders,
                    Scripts,
                    Items,
                    Restrictions,
                    Roles
            )
        }.execute()
    }

    fun build() {
        lavalink = JdaLavalink(
                config.id,
                1,
                { jda!! }
        )

        jda = JDABuilder(AccountType.BOT).apply {
            setToken(config.token)
            addEventListener(EventListener())
            addEventListener(lavalink)
        }.build()

        for (node in config.lavalink) {
            lavalink.addNode(URI(node.url), node.password)
        }

        Akatsuki.jda = jda
    }

    fun build(firstShard: Int, lastShard: Int, total: Int) {
        lavalink = JdaLavalink(
                config.id,
                total,
                { shardManager.getShardById(it) }
        )

        shardManager = DefaultShardManagerBuilder().apply {
            setToken(config.token)
            addEventListeners(EventListener())
            addEventListeners(lavalink)
            setAutoReconnect(true)
            setShardsTotal(total)
            setShards(firstShard, lastShard)
        }.build()

        for (node in config.lavalink) {
            lavalink.addNode(URI(node.url), node.password)
        }
    }

    companion object {
        lateinit var config: Config
        lateinit var shardManager: ShardManager
        lateinit var lavalink: JdaLavalink
        var jda: JDA? = null

        val logger = Logger(Akatsuki::class.jvmName)
        val pool: ExecutorService by lazy {
            Executors.newCachedThreadPool {
                Thread(it, "Akatsuki-Main-Pool-Thread").apply {
                    isDaemon = true
                }
            }
        }
        val coroutineDispatcher by lazy {
            CoroutineDispatcher(pool)
        }
    }
}
