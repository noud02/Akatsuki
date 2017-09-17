package me.noud02.akatsuki.commands

import me.noud02.akatsuki.bot.entities.Argument
import me.noud02.akatsuki.bot.entities.Command
import me.noud02.akatsuki.bot.entities.Context
import me.noud02.akatsuki.bot.entities.Load

@Load
@Argument("command", "string", true)
class Help : Command() {
    override val name = "help"
    override val desc = "Sends you help!"

    override fun run(ctx: Context) {
        if (ctx.args.contains("command"))
            return ctx.send(ctx.help(ctx.args["command"] as String))
        else {
            val commands: List<String> = ctx.client.cmdHandler.commands.toSortedMap().map { entry: Map.Entry<String, Command> -> "\t${entry.value.name}" + " ".repeat(20 - entry.value.name.length) + entry.value.desc }
            val text = "Commands:\n\n${commands.joinToString("\n")}"
            val partSize = 40
            val parts = mutableListOf<String>()
            val lines = text.split("\n")
            var part = ""

            for (line in lines) {
                if (part.split("\n").size >= partSize) {
                    parts.add(part)
                    part = ""
                }

                part += "$line\n"
            }

            if (part.isNotBlank() && part.split("\n").size < partSize)
                parts.add(part)

            for (partt in parts) {
                ctx.author.openPrivateChannel().complete().sendMessage("```$partt```").queue()
            }
        }
    }
}