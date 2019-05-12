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

import me.aurieh.ares.exposed.pg.jsonbMap
import me.aurieh.ares.exposed.pg.pgArray
import org.jetbrains.exposed.sql.Table

object Guilds : Table() {
    val id = long("id")
            .uniqueIndex()
            .primaryKey()
    val name = varchar("name", 100)
    val lang = varchar("lang", 5)
    val prefixes = pgArray<String>("prefixes", "varchar")
    val forceLang = bool("forceLang")
    val starboard = bool("starboard")
    val starboardChannel = long("starboardChannel")
            .nullable()
    val logs = bool("logs")
    val modlogs = bool("modlogs")
    val modlogChannel = long("modlogChannel")
            .nullable()
    val rolemeRoles = jsonbMap<String, Long>("rolemeRoles")
    val welcome = bool("welcome")
    val welcomeChannel = long("welcomeChannel")
            .nullable()
    val welcomeMessage = varchar("welcomeMessage", 2000)
    val leaveMessage = varchar("leaveMessage", 2000)
    val ignoredChannels = pgArray<Long>("ignoredChannels", "BIGINT")
    val levelMessages = bool("levelMessages")
    val mutedRole = long("mutedRole")
            .nullable()
    val antiInvite = bool("antiInvite")
}