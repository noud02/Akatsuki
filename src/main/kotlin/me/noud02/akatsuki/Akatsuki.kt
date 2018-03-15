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

import io.sentry.Sentry
import lavalink.client.io.Lavalink
import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.db.schema.*
import me.noud02.akatsuki.entities.Config
import me.noud02.akatsuki.entities.CoroutineDispatcher
import me.noud02.akatsuki.utils.Logger
import me.noud02.akatsuki.utils.Wolk
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

        Sentry.init(config.api.sentry)

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
        lavalink = Lavalink(
                config.id,
                1,
                { jda!! }
        )

        jda = JDABuilder(AccountType.BOT).apply {
            setToken(config.token)
            addEventListener(EventListener())
            addEventListener(lavalink)
        }.buildAsync()

        for (node in config.lavalink) {
            lavalink.addNode(URI(node.url), node.password)
        }

        Akatsuki.jda = jda
    }

    fun build(firstShard: Int, lastShard: Int, total: Int) {
        lavalink = Lavalink(
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
        lateinit var lavalink: Lavalink
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