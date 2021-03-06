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

package moe.kyubey.akatsuki.utils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.text.SimpleDateFormat
import java.util.*

class Logger(loggerName: String) {
    val ANSI_RESET = "\u001B[0m"
    val ANSI_BLACK = "\u001B[30m"
    val ANSI_RED = "\u001B[31m"
    val ANSI_GREEN = "\u001B[32m"
    val ANSI_YELLOW = "\u001B[33m"
    val ANSI_BLUE = "\u001B[34m"
    val ANSI_PURPLE = "\u001B[35m"
    val ANSI_CYAN = "\u001B[36m"
    val ANSI_WHITE = "\u001B[37m"

    val ANSI_BLACK_BACKGROUND = "\u001B[40m"
    val ANSI_RED_BACKGROUND = "\u001B[41m"
    val ANSI_GREEN_BACKGROUND = "\u001B[42m"
    val ANSI_YELLOW_BACKGROUND = "\u001B[43m"
    val ANSI_BLUE_BACKGROUND = "\u001B[44m"
    val ANSI_PURPLE_BACKGROUND = "\u001B[45m"
    val ANSI_CYAN_BACKGROUND = "\u001B[46m"
    val ANSI_WHITE_BACKGROUND = "\u001B[47m"

    private val name = "$ANSI_BLACK_BACKGROUND$ANSI_YELLOW[ $loggerName ]$ANSI_RESET"

    private val time
        get() = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Date())

    private val timeText
        get() = "$ANSI_CYAN_BACKGROUND$ANSI_BLACK[ $time ]$ANSI_RESET"

    fun command(event: MessageReceivedEvent)
            = println(
            "$name $timeText " +
                    (if (event.guild != null)
                        "$ANSI_PURPLE${event.guild.name} (${event.guild.id})$ANSI_RESET -> $ANSI_GREEN${event.channel.name} (${event.channel.id})$ANSI_RESET"
                    else
                        "${ANSI_GREEN}DM$ANSI_RESET") +
                    " -> $ANSI_BLUE${event.author.name}#${event.author.discriminator} (${event.author.id})$ANSI_RESET:" +
                    " $ANSI_WHITE${event.message.contentDisplay}$ANSI_RESET"
    )

    fun info(vararg args: String)
            = println("$name $timeText $ANSI_PURPLE_BACKGROUND$ANSI_WHITE INF $ANSI_RESET ${args.joinToString(" ")}")

    fun error(vararg args: String)
            = println("$name $timeText $ANSI_RED_BACKGROUND$ANSI_WHITE ERR $ANSI_RESET ${args.joinToString(" ")}")

    fun error(text: String, e: Throwable)
            = error("$text\n$e\n${e.stackTrace.joinToString("\n") {
        "\tat ${it.className}(${it.fileName ?: "Unknown Source"})"
    }}")

    fun warn(vararg args: String)
            = println("$name $timeText $ANSI_YELLOW_BACKGROUND$ANSI_BLACK WRN $ANSI_RESET ${args.joinToString(" ")}")

    fun debug(vararg args: String)
            = println("$name $timeText $ANSI_WHITE_BACKGROUND$ANSI_BLACK DBG $ANSI_RESET ${args.joinToString(" ")}")
}