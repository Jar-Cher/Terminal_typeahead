package Terminal_typeahead

import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException

fun main(args: Array<String>) {
    println(simplify("filter{(element>10)}%>%filter{(element<20)}"))
    println(simplify("map{(element+10)}%>%filter{(element>10)}%>%map{(element*element)}"))
    println(simplify("filter{(element>0)}%>%filter{(element<0)}%>%map{(element*element)}"))
    println(simplify("filter{(element>0)}%>%filter{(element<0)}%>%map{(element*element)"))
    println(simplify("adfad"))
}

enum class CommandType {
    MAP, FILTER
}

data class Command internal constructor(val type: CommandType,
                                        var rawExpression: String) {

}

fun simplify(expression: String): String {
    // Java/Kotlin RegExp does not support recursions :(
    val ans = simplifyChain(expression)
    return "filter{" + ans.first().rawExpression + "}%>%map{" + ans.last().rawExpression + "}"
}

fun simplifyChain(expression: String): MutableList<Command> {
    val chain = expression.split("%>%")
    val commands = mutableListOf<Command>()
    for (i in chain) {
        when {
            i.matches(Regex("""^filter\{.*}$""")) ->
                commands.add(Command(CommandType.FILTER, i.substring(7, i.length-1)))
            i.matches(Regex("""^map\{.*}$""")) ->
                commands.add(Command(CommandType.MAP, i.substring(4, i.length-1)))
            else -> throw SyntaxException("Invalid command")
        }
    }
    //if (commands.last().type != CommandType.MAP)
    //    commands.add(Command(CommandType.MAP, "element"))
    var toReplaceWith = "element"
    var cond = ""
    for (i in commands) {
        i.rawExpression = Regex("element").replace(i.rawExpression, toReplaceWith)
        if (i.type == CommandType.MAP) {
            toReplaceWith = i.rawExpression
        }
        else if (i.type == CommandType.FILTER) {
            cond = if (cond == "")
                i.rawExpression
            else
                "(" + cond + "&" + i.rawExpression + ")"
        }
    }

    return mutableListOf(Command(CommandType.FILTER, cond),
        Command(CommandType.MAP, toReplaceWith))
}