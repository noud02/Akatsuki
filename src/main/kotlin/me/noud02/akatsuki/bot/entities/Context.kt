package me.noud02.akatsuki.bot.entities

import me.aurieh.ares.utils.ArgParser
import me.noud02.akatsuki.bot.Akatsuki
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*

class Context(val event: MessageReceivedEvent, val client: Akatsuki, private val cmd: Command, val args: MutableMap<String, Any>, val rawArgs: List<String>, val flags: ArgParser.ParsedResult, val perms: MutableMap<String, Boolean>, val lang: ResourceBundle) {
    val guild: Guild? = event.guild
    val author: User = event.author
    val channel: MessageChannel = event.channel
    val msg: Message = event.message
    val member: Member? = event.member
    val selfMember: Member? = event.guild.selfMember

    // TODO use await here instead of queue

    fun send(arg: String) = event.channel.sendMessage(arg).queue()
    fun send(arg: MessageEmbed) = event.channel.sendMessage(arg).queue()

    fun sendCode(lang: String, arg: Any) = event.channel.sendMessage("```$lang\n$arg```").queue()

    fun sendError(e: Throwable) = event.channel.sendMessage("```diff\n- ${e.stackTrace}```").queue()

    fun help() = client.cmdHandler.help(cmd)

    fun help(cmdd: String): String {
        return try {
            client.cmdHandler.help(cmdd)
        } catch (e: Exception) {
            e.message.toString()
        }
    }
}