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
import moe.kyubey.akatsuki.db.DBContract
import moe.kyubey.akatsuki.db.schema.Contracts
import net.dv8tion.jda.core.entities.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.joda.time.DateTime

fun User.createContract(wish: String) {
    asyncTransaction(Akatsuki.pool) {
        Contracts.insert {
            it[userId] = idLong
            it[this.wish] = wish
            it[date] = DateTime.now()
            it[gem] = "common"
            it[corruption] = 0
            it[level] = 1
            it[experience] = 0
            it[inventory] = arrayOf()
            it[balance] = 5000
        }
    }.execute()
}