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

package moe.kyubey.akatsuki.db.schema

import me.aurieh.ares.exposed.pg.jsonbArray
import me.aurieh.ares.exposed.pg.pgArray
import org.jetbrains.exposed.sql.Table
import org.json.JSONObject

object Logs : Table() {
    val event = varchar("event", 6)

    // Message
    val messageId = long("messageId")
    val content = varchar("content", 2000)
    val attachments = pgArray<String>("attachments", "text")
    val embeds = jsonbArray<JSONObject>("embeds")
    val timestamp = long("timestamp")

    // Author
    val authorId = long("authorId")
    val authorName = varchar("authorName", 33)
    val authorDiscrim = varchar("authorDiscrim", 4)
    val authorAvatar = text("authorAvatar")
    val authorNick = varchar("authorNick", 33)

    // Guild
    val guildId = long("guildId")
    val guildName = varchar("guildName", 100)

    // Channel
    val channelId = long("channelId")
    val channelName = varchar("channelName", 100)
}