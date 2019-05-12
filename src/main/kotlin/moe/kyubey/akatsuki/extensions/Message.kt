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

package moe.kyubey.akatsuki.extensions

import me.aurieh.ares.exposed.async.asyncTransaction
import moe.kyubey.akatsuki.Akatsuki
import moe.kyubey.akatsuki.db.schema.Logs
import org.jetbrains.exposed.sql.insert
import com.google.gson.Gson
import net.dv8tion.jda.core.entities.Message
import org.json.JSONObject

fun Message.log(ev: String = "CREATE") {
    val gson = Gson()

    asyncTransaction(Akatsuki.pool) {
        Logs.insert {
            it[event] =  ev
            it[messageId] = idLong
            it[content] = contentRaw
            it[attachments] = this@log.attachments.map { it.url }.toTypedArray()
            it[embeds] = this@log.embeds.map { JSONObject(gson.toJson(it)) }.toTypedArray()
            it[timestamp] = this@log.creationTime.toInstant().toEpochMilli() //.getLong(ChronoField.MILLI_OF_SECOND)
            it[authorId] = author.idLong
            it[authorName] = author.name
            it[authorDiscrim] = author.discriminator
            it[authorAvatar] = author.avatarUrl
            it[authorNick] = member?.nickname ?: ""
            it[guildId] = guild.idLong
            it[guildName] = guild.name
            it[channelId] = channel.idLong
            it[channelName] = channel.name
        }
    }.execute().get()
}