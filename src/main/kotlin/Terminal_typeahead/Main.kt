package Terminal_typeahead

import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver
import java.lang.Integer.max
import kotlin.math.*

fun main(args: Array<String>) {
    val solv = NewtonRaphsonSolver().solve(500000, PolynomialFunction(doubleArrayOf(0.0, 4.0, 1.0)), -3.0, 100.0)
    println(solv)
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

data class ArithmeticExpression(var polynomial: ArrayList<Int>) {

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

    fun toPolynomialFunction(): PolynomialFunction =
        PolynomialFunction(DoubleArray(this.polynomial.size) { i -> polynomial[i].toDouble() })

    fun derivative(): ArithmeticExpression {
        val coefficients = toPolynomialFunction().polynomialDerivative().coefficients
        val ans = ArrayList<Int>(List(coefficients.size) {0})
        for (i in ans.indices)
            ans[i] = coefficients[i].toInt()
        return ArithmeticExpression(ans)
    }

    fun eval(x: Int) = polynomial.foldIndexed(0) { id, prevResult, element ->
        prevResult + x.toDouble().pow(id).toInt() * element
    }

    fun solve(): ArrayList<Pair<Double, Double>> {
        if ((polynomial.size == 1) and (polynomial.first() == 0))
            return arrayListOf(Pair(Double.MIN_VALUE, Double.MAX_VALUE))
        if ((polynomial.size == 1) and (polynomial.first() != 0))
            return arrayListOf()
        if(polynomial.size == 2) {
            val ans = -polynomial.first().toDouble() / polynomial.last().toDouble()
            return arrayListOf(Pair(ans, ans))
        }
        val extremes = derivative().solve().map { i -> i.first}
        val ans = ArrayList<Pair<Double, Double>>()
        var lowerEstimate = Double.MIN_VALUE
        for (i in extremes) {
            val root = NewtonRaphsonSolver().solve(500000, toPolynomialFunction(), lowerEstimate, i)
            ans.add(Pair(root, root))
            lowerEstimate = i
        }
        val root =
            NewtonRaphsonSolver().solve(500000, toPolynomialFunction(), extremes.last(), Double.MAX_VALUE)
        ans.add(Pair(root, root))
        return ans
    }

    fun equality(other: ArithmeticExpression): LogicalExpression {
        val rem = this - other
        val roots = rem.solve()
        if ((roots.size == 1) and
            (roots.first().first == Double.MIN_VALUE) and
            (roots.first().second == Double.MAX_VALUE))
            return LogicalExpression(arrayListOf(Pair(Int.MIN_VALUE, Int.MAX_VALUE)))
        if (roots.size == 0)
            return LogicalExpression(arrayListOf())
        val ans =  ArrayList<Pair<Int, Int>>()
        // Doublecheck they are Int roots
        for (i in roots) {
            if (abs(i.first.roundToInt().toDouble() - i.first) < 0.000000001)
                ans.add(Pair(i.first.roundToInt(), i.first.roundToInt()))
        }
        return LogicalExpression(ans)
    }

    fun bigger(other: ArithmeticExpression): LogicalExpression {
        val rem = this - other
        if (rem.polynomial.size == 1)
            return if (rem.polynomial.first() > 0)
                LogicalExpression(arrayListOf(Pair(Int.MIN_VALUE, Int.MAX_VALUE)))
            else
                LogicalExpression(arrayListOf())
        val roots = rem.solve()
        roots.add(0, Pair(Double.MIN_VALUE, Double.MIN_VALUE))
        roots.add(0, Pair(Double.MAX_VALUE, Double.MAX_VALUE))
        val ans =  ArrayList<Pair<Int, Int>>()
        for (i in roots.indices-1) {
            val tester = (roots[i].first + roots[i+1].first) / 2
            if (tester < 0)
                continue
            val lower = if (abs(roots[i].first.roundToInt().toDouble() - roots[i].first) < 0.000000001)
                roots[i].first.roundToInt()
                else ceil(roots[i].first).toInt()
            val higher = if (abs(roots[i+1].first.roundToInt().toDouble() - roots[i+1].first) < 0.000000001)
                roots[i+1].first.roundToInt()
            else floor(roots[i+1].first).toInt()
            if (higher > lower)
                ans.add(Pair(lower, higher))
        }
        return LogicalExpression(ans)
    }

    fun smaller(other: ArithmeticExpression) = other.bigger(this)

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

data class LogicalExpression constructor(var segments: ArrayList<Pair<Int, Int>>) {

    companion object {
        fun from(rawExp: String): LogicalExpression {
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
                    tokens[1] == "=" -> {
                        return ArithmeticExpression.from(tokens[0]).equality(ArithmeticExpression.from(tokens[2]))
                    }
                    tokens[1] == ">" -> {
                        return ArithmeticExpression.from(tokens[0]).bigger(ArithmeticExpression.from(tokens[2]))
                    }
                    tokens[1] == "<" -> {
                        return ArithmeticExpression.from(tokens[0]).smaller(ArithmeticExpression.from(tokens[2]))
                    }
                    tokens[1] == "|" -> {
                        return from(tokens[0]).or(from(tokens[2]))
                    }
                    tokens[1] == "&" -> {
                        return from(tokens[0]).and(from(tokens[2]))
                    }
                    tokens[1] in operations.map { it.toString() } ->
                        throw IllegalArgumentException("TYPE ERROR")
                    else -> throw SyntaxException("SYNTAX ERROR")
                }
            } else throw SyntaxException("SYNTAX ERROR")
        }
    }

    fun or(other: LogicalExpression): LogicalExpression {
        if (this.segments.size == 0)
            return other
        if (other.segments.size == 0)
            return this
        val first = ArrayList<Int>(List(this.segments.size * 2) {0})
        val second = ArrayList<Int>(List(other.segments.size * 2) {0})
        for (i in first.indices) {
            first[i*2] = this.segments[i].first
            first[i*2+1] = this.segments[i].second
            second[i*2] = other.segments[i].first
            second[i*2+1] = other.segments[i].second
        }
        val total = arrayListOf<Int>()
        total.addAll(first)
        total.addAll(second)
        total.sort()
        var isInFirst = false
        var isInSecond = false
        var ans = arrayListOf<Int>()
        var firstId = 0
        var secondId = 0
        for (i in total) {
            if (i in first)
                isInFirst = !isInFirst
            if (i in second)
                isInSecond = !isInSecond
        }
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