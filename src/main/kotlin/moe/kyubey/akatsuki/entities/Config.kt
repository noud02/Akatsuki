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

package moe.kyubey.akatsuki.entities

data class APIConfig(
        val google: String,
        val weebsh: String,
        val discordbots: String,
        val discordbotsorg: String,
        val myanimelist: String,
        val sentry: String,
        val osu: String
)

data class DatabaseConfig(
        val name: String,
        val user: String,
        val pass: String,
        val host: String
)

data class SiteConfig(
        val host: String,
        val ssl: Boolean,
        val port: Int
)

data class BackendConfig(
        val host: String,
        val ssl: Boolean,
        val port: Int
)

data class PresenceConfig(
        val text: String,
        val type: String
)

data class LavalinkNodeConfig(
        val url: String,
        val password: String
)

data class Config(
        val id: String,
        val token: String,
        val description: String,
        val owners: List<String>,
        val prefixes: List<String>,
        val presences: List<PresenceConfig>,
        val database: DatabaseConfig,
        val api: APIConfig,
        val site: SiteConfig,
        val backend: BackendConfig,
        val lavalink: List<LavalinkNodeConfig>
)
