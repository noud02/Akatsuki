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
import me.aurieh.ares.exposed.async.asyncTransaction
import me.noud02.akatsuki.Akatsuki
import me.noud02.akatsuki.EventListener
import me.noud02.akatsuki.annotations.Argument
import me.noud02.akatsuki.annotations.Load
import me.noud02.akatsuki.annotations.Perm
import me.noud02.akatsuki.db.schema.Guilds
import me.noud02.akatsuki.entities.Command
import me.noud02.akatsuki.entities.Context
import me.noud02.akatsuki.extensions.searchTextChannels
import me.noud02.akatsuki.utils.TextChannelPicker
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.update
import java.util.concurrent.CompletableFuture

@Load
@Perm(Permission.MANAGE_SERVER)
@Argument("topic", "string", true)
class Setup : Command() {
    override val desc = "Easy setup of Kyubey."
    override val guildOnly = true

    private val topics = listOf(
            "starboard",
            "modlogs",
            "logs"
    )

    override fun run(ctx: Context) {
        val topic = ctx.args.getOrDefault("topic", "choose") as String

        if (topic == "choose") {
            ctx.channel.sendMessage("What would you like to set up? (`cancel` to cancel, `list` for a list of topics)").queue {
                EventListener.waiter.await<MessageReceivedEvent>(1, 60000L) { event ->
                    if (event.author.id == ctx.author.id && event.channel.id == ctx.channel.id) {
                        if (event.message.contentRaw == "list") {
                            ctx.sendCode("asciidoc", topics.joinToString("\n"))
                            return@await true
                        }

                        if (event.message.contentRaw != "cancel") {
                            setupTopic(ctx, event.message.contentRaw)
                        }

                        true
                    } else {
                        false
                    }
                }
            }
        } else {
            setupTopic(ctx, topic)
        }
    }

    private fun setupTopic(ctx: Context, topic: String) {
        when (topic) {
            "starboard" -> setupStarboard(ctx)
            "modlogs" -> setupModlogs(ctx)
            "modlog" -> setupModlogs(ctx)
            "logs" -> setupLogs(ctx)
            else -> ctx.send("Setup topic not found!")
        }
    }

    private fun askChannel(ctx: Context): CompletableFuture<TextChannel> {
        val fut = CompletableFuture<TextChannel>()

        EventListener.waiter.await<MessageReceivedEvent>(1, 60000L) { event ->
            if (event.author.id == ctx.author.id && event.channel.id == ctx.channel.id) {
                if (event.message.contentRaw == "cancel") {
                    return@await true
                }

                val channels = ctx.guild!!.searchTextChannels(event.message.contentRaw)

                if (channels.isEmpty()) {
                    ctx.channel.sendMessage("Couldn't find that channel, mind trying again?").queue {
                        askChannel(ctx).thenAccept { fut.complete(it) }
                    }
                    return@await true
                }

                if (channels.size == 1) {
                    fut.complete(channels[0])
                    return@await true
                }

                val picker = TextChannelPicker(EventListener.waiter, ctx.member!!, channels, ctx.guild)

                picker.build(ctx.msg).thenAccept { fut.complete(it) }

                true
            } else {
                false
            }
        }

        return fut
    }

    private fun setupStarboard(ctx: Context) {
        ctx.channel.sendMessage("What channel would you like to have the starboard in? (`cancel` to cancel)").queue {
            askChannel(ctx).thenAccept { channel ->
                asyncTransaction(Akatsuki.pool) {
                    Guilds.update({
                        Guilds.id.eq(ctx.guild!!.idLong)
                    }) {
                        it[starboard] = true
                        it[starboardChannel] = channel.idLong
                    }
                }.execute().thenAccept {
                    ctx.send("Successfully set up starboard! (You can disable starboard using `k;config disable starboard`)")
                }.thenApply {}.exceptionally {
                    Sentry.capture(it)
                    ctx.sendError(it)
                    ctx.logger.error("Error while trying to set up starboard", it)
                }
            }
        }
    }

    private fun setupModlogs(ctx: Context) {
        ctx.channel.sendMessage("What channel would you like to have the modlog in? (`cancel` to cancel)").queue {
            askChannel(ctx).thenAccept { channel ->
                asyncTransaction(Akatsuki.pool) {
                    Guilds.update({
                        Guilds.id.eq(ctx.guild!!.idLong)
                    }) {
                        it[modlogs] = true
                        it[modlogChannel] = channel.idLong
                    }
                }.execute().thenAccept {
                    ctx.send("Successfully set up modlogs! (You can disable modlogs using `k;config disable modlogs`)")
                }.thenApply {}.exceptionally {
                    Sentry.capture(it)
                    ctx.sendError(it)
                    ctx.logger.error("Error while trying to set up modlogs", it)
                }
            }
        }
    }

    private fun setupLogs(ctx: Context) {
        ctx.channel.sendMessage("Are you sure you want to enable message logs for **all** channels? [y/N]").queue {
            EventListener.waiter.await<MessageReceivedEvent>(1, 60000L) { event ->
                if (event.author.id == ctx.author.id && event.channel.id == ctx.channel.id) {
                    if (event.message.contentRaw.toLowerCase().startsWith("y")) {
                        asyncTransaction(Akatsuki.pool) {
                            Guilds.update({
                                Guilds.id.eq(ctx.guild!!.idLong)
                            }) {
                                it[logs] = true
                            }
                        }.execute().thenAccept {
                            ctx.send("Successfully set up message logs! (You can disable logs using `k;config disable logs`)")
                        }.thenApply {}.exceptionally {
                            Sentry.capture(it)
                            ctx.sendError(it)
                            ctx.logger.error("Error while trying to set up logs", it)
                        }
                    } else {
                        ctx.send("Logs will **not** be enabled.")
                    }

                    true
                } else {
                    false
                }
            }
        }
    }
}