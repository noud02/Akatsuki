/*
 *  Copyright (c) 2017 Noud Kerver
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

package me.noud02.akatsuki.bot.db

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.bot.entities.CoroutineDispatcher
import me.noud02.akatsuki.bot.entities.DatabaseConfig
import me.noud02.akatsuki.bot.schema.Guilds
import me.noud02.akatsuki.bot.schema.Users
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class DBGuild(
        val id: String,
        val name: String,
        val lang: String,
        val prefixes: List<String>,
        val forceLang: Boolean
)

data class DBUser(
        val id: String,
        val username: String,
        val discriminator: String,
        val lang: String
)

class DatabaseWrapper(private val config: DatabaseConfig) {
    private val pool: ExecutorService by lazy {
        Executors.newCachedThreadPool {
            Thread(it, "Akatsuki-Database-Pool-Thread").apply {
                isDaemon = true
            }
        }
    }
    private val coroutineDispatcher by lazy {
        CoroutineDispatcher(pool)
    }

    val db = Database.connect(
            "jdbc:postgresql:${config.name}",
            "org.postgresql.Driver",
            config.user,
            config.pass
    )

    fun getGuild(guild: Guild) = getGuild(guild.id)

    fun getGuild(id: String): CompletableFuture<DBGuild> {
        val fut = CompletableFuture<DBGuild>()

        async(coroutineDispatcher) {
            asyncTransaction(pool) {
                try {
                    val guild = Guilds.select {
                        Guilds.id.eq(id)
                    }.first()

                    fut.complete(DBGuild(
                            guild[Guilds.id],
                            guild[Guilds.name],
                            guild[Guilds.lang],
                            guild[Guilds.prefixes].toList(),
                            guild[Guilds.forceLang]
                    ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.await()
        }

        return fut
    }

    fun getGuildSafe(guild: Guild): CompletableFuture<DBGuild> {
        async(coroutineDispatcher) {
            asyncTransaction(pool) {
                val selection = Guilds.select {
                    Guilds.id.eq(guild.id)
                }

                if (selection.empty())
                    newGuild(guild)
            }
        }

        return getGuild(guild)
    }

    fun getUser(user: User) = getUser(user.id)

    fun getUser(member: Member) = getUser(member.user.id)

    fun getUser(id: String): CompletableFuture<DBUser> {
        val fut = CompletableFuture<DBUser>()

        async(coroutineDispatcher) {
            asyncTransaction(pool) {
                try {
                    val user = Users.select {
                        Users.id.eq(id)
                    }.first()

                    fut.complete(DBUser(
                            user[Users.id],
                            user[Users.username],
                            user[Users.discriminator],
                            user[Users.lang]
                    ))
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }.await()
        }

        return fut
    }

    fun getUserSafe(member: Member) = getUserSafe(member.user)

    fun getUserSafe(user: User): CompletableFuture<DBGuild> {
        async(coroutineDispatcher) {
            asyncTransaction(pool) {
                val selection = Users.select {
                    Users.id.eq(user.id)
                }

                if (selection.empty())
                    newUser(user)
            }
        }

        return getUser(user)
    }

    fun newGuild(guild: Guild): CompletableFuture<DBGuild> {
        async(coroutineDispatcher) {
            asyncTransaction(pool) {
                try {
                    Guilds.insert {
                        it[id] = guild.id
                        it[name] = guild.name
                        it[prefixes] = arrayOf()
                        it[lang] = "en_US"
                        it[forceLang] = false
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }.await()
        }

        return getGuild(guild)
    }

    fun newUser(member: Member) = newUser(member.user)

    fun newUser(user: User): CompletableFuture<DBUser> {
        async(coroutineDispatcher) {
            asyncTransaction(pool) {
                try {
                    Users.insert {
                        it[id] = user.id
                        it[username] = user.name
                        it[discriminator] = user.discriminator
                        it[lang] = "en_US"
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }.await()
        }

        return getUser(user)
    }

}