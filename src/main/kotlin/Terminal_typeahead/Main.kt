package Terminal_typeahead

import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException
import java.lang.Integer.max
import kotlin.math.absoluteValue

fun main(args: Array<String>) {
    var a = ArithmeticExpression.from("(element*10)")
    var b = ArithmeticExpression.from("(element+10)")
    println(a.polynomial)
    println(a)
    println(b.polynomial)
    println(b)
    a = ArithmeticExpression.from("(element-10)")
    println(a.polynomial)
    println(a)
    b = ArithmeticExpression.from("(element-element)")
    println(b.polynomial)
    println(b)
    b = ArithmeticExpression.from("(element--2)")
    println(b.polynomial)
    println(b)
    b = ArithmeticExpression.from("-42")
    println(b.polynomial)
    println(b)
    b = ArithmeticExpression.from("0")
    println(b.polynomial)
    println(b)
    b = ArithmeticExpression.from("3")
    println(b.polynomial)
    println(b)
    b = ArithmeticExpression.from("element")
    println(b.polynomial)
    println(b)
    b = ArithmeticExpression.from("((2-(3*-2))-(element*3))")
    println(b.polynomial)
    println(b)
    println(a==b)
    //a = ArithmeticExpression.from("(element>10)")
    println(simplify("filter{(element>10)}%>%filter{(element<20)}"))
    println(simplify("map{(element+10)}%>%filter{(element>10)}%>%map{(element*element)}"))
    println(simplify("filter{(element>0)}%>%filter{(element<0)}%>%map{(element*element)}"))
    println(simplify("filter{(element>0)}%>%filter{(element<0)}%>%map{(element*element)"))
    println(simplify("adfad"))
}

enum class CommandType {
    MAP, FILTER
}

enum class TruthType {
    TRUE, FALSE, UNDETERMINED
}

val operations = setOf('+', '-', '*', '>', '<', '=', '&', '|')

data class Command (val type: CommandType, var rawExpression: String)

data class ArithmeticExpression(var polynomial: ArrayList<Int> = ArrayList()) {

    init {
        require(polynomial.size != 0) { "Incorrect polynomial" }
        while ((polynomial.last() == 0) and (polynomial.size > 1))
            polynomial.removeAt(polynomial.size - 1)
    }

    companion object {

        fun from(rawExp: String): ArithmeticExpression {
            when {
                rawExp == "element" -> return ArithmeticExpression(arrayListOf(0, 1))
                rawExp.matches(Regex("""^-?\d+$""")) -> return ArithmeticExpression(arrayListOf(rawExp.toInt()))
                rawExp.matches(Regex("""^\(.*\)$""")) -> {
                    val exp = rawExp.subSequence(1, rawExp.length - 1)
                    var depth = 0
                    val tokens = arrayListOf("", "", "")
                    var tokenId = 0
                    for (i in exp.indices) {
                        when (exp[i]) {
                            '(' -> depth ++
                            ')' -> depth --
                        }
                        if ((depth == 0) and (tokenId == 0) and (i > 0) and (exp[i] in operations)) {
                            tokenId = 1
                        }
                        else if (tokenId == 1) {
                            tokenId = 2
                        }
                        tokens[tokenId] = tokens[tokenId] + exp[i]
                    }
                    return when {
                        tokens[1] == "+" -> from(tokens[0]) + from(tokens[2])
                        tokens[1] == "-" -> from(tokens[0]) - from(tokens[2])
                        tokens[1] == "*" -> from(tokens[0]) * from(tokens[2])
                        tokens[1] in operations.map { it.toString() } ->
                            throw IllegalArgumentException("TYPE ERROR")
                        else -> throw SyntaxException("SYNTAX ERROR")
                    }
                }
                else -> throw SyntaxException("SYNTAX ERROR")
            }
        }

        private fun xpow(pow: Int): String {
            if (pow < 1) {
                return "1"
            }
            var ans = "element"
            for (i in 2..pow) {
                ans = "($ans*element)"
            }
            return ans
        }
    }

    operator fun plus(other: ArithmeticExpression): ArithmeticExpression {
        val ans = ArrayList<Int>(List(max(this.polynomial.size, other.polynomial.size)) {0})
        for (i in ans.indices) {
            ans[i] = this.polynomial.getOrElse(i) {0} + other.polynomial.getOrElse(i) {0}
        }
        return ArithmeticExpression(ans)
    }

    operator fun unaryMinus() = ArithmeticExpression(ArrayList(this.copy().polynomial.map { -it }))

    operator fun minus(other: ArithmeticExpression): ArithmeticExpression = this + (-other)

    operator fun times(other: ArithmeticExpression): ArithmeticExpression {
        val ans = ArrayList<Int>(List(this.polynomial.size + other.polynomial.size - 1) {0})
        for (i in this.polynomial.indices) {
            for (j in other.polynomial.indices) {
                ans[i+j] = ans.getOrElse(i+j) {0} + this.polynomial[i] * other.polynomial[j]
            }
        }
        return ArithmeticExpression(ans)
    }

    fun isPositive(): TruthType {
        if (polynomial.filterIndexed { i, _ -> i % 2 == 0}.any { it != 0})
            return TruthType.UNDETERMINED
        val evenId = polynomial.filterIndexed { i, _ -> i % 2 == 0}
        return when {
            evenId.all { it > 0} -> TruthType.TRUE
            evenId.all { it < 0} -> TruthType.FALSE
            else -> TruthType.UNDETERMINED
        }
    }

    override fun toString(): String {
        if (this.polynomial.size == 1)
            return this.polynomial.first().toString()
        //
        var ans = if (polynomial.last() == 1)
            xpow(polynomial.size - 1)
        else
            """(${polynomial.last()}*${xpow(polynomial.size - 1)})"""
        //
        for (i in this.polynomial.size - 2 downTo 1) {
            if (polynomial[i] == 0)
                continue
            val monomial = if (polynomial[i].absoluteValue == 1)
                xpow(i)
            else
                """(${polynomial[i].absoluteValue}*${xpow(i)})"""
            ans = if (polynomial[i] > 0)
                "($ans+$monomial)"
            else
                "($ans-$monomial)"
        }
        //
        return when {
            polynomial.first() == 0 -> ans
            polynomial.first() > 0 -> """($ans+${polynomial.first()})"""
            else -> """($ans${polynomial.first()})"""
        }
    }
}

data class LogicalExpression constructor(var rawExp: String) {

    init {
        if (rawExp.matches(Regex("""^\(.*\)$"""))) {
            val exp = rawExp.subSequence(1, rawExp.length - 1)
            var depth = 0
            val tokens = arrayListOf("", "", "")
            var tokenId = 0
            for (i in exp.indices) {
                when (exp[i]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                if ((depth == 0) and (tokenId == 0) and (i > 0) and (exp[i] in operations)) {
                    tokenId = 1
                } else if (tokenId == 1) {
                    tokenId = 2
                }
                tokens[tokenId] = tokens[tokenId] + exp[i]
            }
            when {
                tokens[1] == "=" -> rawExp = when {
                    ArithmeticExpression.from(tokens[0]) == ArithmeticExpression.from(tokens[2]) -> "(1=1)"
                    (ArithmeticExpression.from(tokens[0]) -
                            ArithmeticExpression.from(tokens[2])).polynomial.size == 1 -> "(1=0)"
                    else -> rawExp
                }
                tokens[1] == ">" -> {
                    val isPositive = (ArithmeticExpression.from(tokens[0]) -
                        ArithmeticExpression.from(tokens[2])).isPositive()
                    rawExp = when (isPositive) {
                        TruthType.TRUE -> "(1=1)"
                        TruthType.FALSE -> "(1=0)"
                        else -> rawExp
                    }
                }
                tokens[1] == "<" -> {
                    val isPositive = (ArithmeticExpression.from(tokens[0]) -
                            ArithmeticExpression.from(tokens[2])).isPositive()
                    rawExp = when (isPositive) {
                        TruthType.TRUE -> "(1=0)"
                        TruthType.FALSE -> "(1=1)"
                        else -> rawExp
                    }
                }
                tokens[1] == "|" -> {
                    val firstExp = LogicalExpression(tokens[0])
                    val secondExp = LogicalExpression(tokens[2])
                    rawExp = when {
                        (firstExp.rawExp == "(1=1)") or (secondExp.rawExp == "(1=1)") -> "(1=1)"
                        firstExp.rawExp == "(1=0)" -> secondExp.rawExp
                        secondExp.rawExp == "(1=0)" -> firstExp.rawExp
                        else -> rawExp
                    }
                }
                tokens[1] == "&" -> {
                    val firstExp = LogicalExpression(tokens[0])
                    val secondExp = LogicalExpression(tokens[2])
                    rawExp = when {
                        (firstExp.rawExp == "(1=0)") or (secondExp.rawExp == "(1=0)") -> "(1=0)"
                        firstExp.rawExp == "(1=1)" -> secondExp.rawExp
                        secondExp.rawExp == "(1=1)" -> firstExp.rawExp
                        else -> rawExp
                    }
                }
                tokens[1] in operations.map { it.toString() } ->
                    throw IllegalArgumentException("TYPE ERROR")
                else -> throw SyntaxException("SYNTAX ERROR")
            }
        }
        else throw SyntaxException("SYNTAX ERROR")
    }

    override fun toString(): String {
        return rawExp
    }
}


fun simplify(expression: String): String {
    // Java/Kotlin RegExp does not support recursions :(
    val ans = simplifyChain(expression)
    return "filter{" + LogicalExpression(ans.first().rawExpression).toString() +
            "}%>%map{" + ArithmeticExpression.from(ans.last().rawExpression).toString() + "}"
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
            else -> throw SyntaxException("SYNTAX ERROR")
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
    if (cond == "")
        cond = "(1=1)"
    return mutableListOf(Command(CommandType.FILTER, cond),
        Command(CommandType.MAP, toReplaceWith))
}