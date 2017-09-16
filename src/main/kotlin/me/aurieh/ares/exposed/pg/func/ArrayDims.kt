/*
 * Copyright 2017 aurieh <me@aurieh.me>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.aurieh.ares.exposed.pg.func

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Function

class ArrayDims<T>(private val arr: Expression<Array<T>>) : Function<StringColumnType>() {
    override val columnType = StringColumnType()

    override fun toSQL(queryBuilder: QueryBuilder): String {
        return "array_dims(${arr.toSQL(queryBuilder)})"
    }
}