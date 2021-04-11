package Terminal_typeahead

import org.junit.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HelloTest {

    @Test
    fun testArithmeticExpressionPlusMinus() {
        assertTrue {
            ArithmeticExpression(arrayListOf(3, 3, 3, 3)) + ArithmeticExpression(arrayListOf(3, 3, 3, 3)) ==
                    ArithmeticExpression(arrayListOf(6, 6, 6, 6))
        }
        assertTrue {
            ArithmeticExpression(arrayListOf(3, 3, 3, 3)) + ArithmeticExpression(arrayListOf(-3, -3, -3, -3)) ==
                    ArithmeticExpression(arrayListOf(0, 0, 0, 0))
        }
        assertTrue {
            ArithmeticExpression(arrayListOf(3, 3, 3, 3)) + ArithmeticExpression(arrayListOf(-3, -3, -3, -3)) ==
                    ArithmeticExpression(arrayListOf(0))
        }
        assertTrue {
            ArithmeticExpression(arrayListOf(1, 2, 3, 4)) + ArithmeticExpression(arrayListOf(0, -42, 42, 80)) ==
                    ArithmeticExpression(arrayListOf(1, -40, 45, 84))
        }
        val v = Random()
        for (i in 1..100) {
            val len = v.nextInt(100) + 1
            val a = (1..len).map { v.nextInt() - 2 * v.nextInt() }
            val b = (1..len).map { v.nextInt() - 2 * v.nextInt() }
            val sum = (1..len).mapIndexed { id, _ -> a[id] + b[id] }
            val rem1 = (1..len).mapIndexed { id, _ -> a[id] - b[id] }
            val rem2 = (1..len).mapIndexed { id, _ -> b[id] - a[id] }
            assertEquals(
                ArithmeticExpression(ArrayList(sum)),
                ArithmeticExpression(ArrayList(a)) + ArithmeticExpression(ArrayList(b)))
            assertEquals(
                sum,
                ArithmeticExpression(ArrayList(a)).polynomial
                    .mapIndexed { id, it -> it + ArithmeticExpression(ArrayList(b)).polynomial[id] })
            assertEquals(ArithmeticExpression(ArrayList(rem1)),
                ArithmeticExpression(ArrayList(a)) - ArithmeticExpression(ArrayList(b)))
            assertEquals(
                rem1,
                ArithmeticExpression(ArrayList(a)).polynomial
                    .mapIndexed { id, it -> it - ArithmeticExpression(ArrayList(b)).polynomial[id] })
            assertEquals(ArithmeticExpression(ArrayList(rem2)),
                ArithmeticExpression(ArrayList(b)) - ArithmeticExpression(ArrayList(a)))
            assertEquals(
                rem2,
                ArithmeticExpression(ArrayList(b)).polynomial
                    .mapIndexed { id, it -> it - ArithmeticExpression(ArrayList(a)).polynomial[id] })
        }
    }

    @Test
    fun testArithmeticExpressionTimes() {
        assertTrue {
            ArithmeticExpression(arrayListOf(-1, 2, -3, 4)) * ArithmeticExpression(arrayListOf(4, 2)) ==
                    ArithmeticExpression(arrayListOf(-4, 6, -8, 10, 8))
        }
        val v = Random()
        for (i in 1..100) {
            val len1 = v.nextInt(5) + 1
            val len2 = v.nextInt(5) + 1
            val lenAns = len1 + len2 - 1
            val a  = (1..len1).map { v.nextInt() - 2*v.nextInt()}
            val b  = (1..len2).map { v.nextInt() - 2*v.nextInt()}
            val c = ArrayList<Int>(List(a.size + b.size - 1) {0})
            for (j in a.indices) {
                for (k in b.indices) {
                    c[j+k] = c.getOrElse(j+k) {0} + a[j] * b[k]
                }
            }
            assertEquals(ArithmeticExpression(ArrayList(c)),
                ArithmeticExpression(ArrayList(a)) * ArithmeticExpression(ArrayList(b)))
            assertEquals(ArithmeticExpression(ArrayList(c)),
                ArithmeticExpression(ArrayList(b)) * ArithmeticExpression(ArrayList(a)))
        }
    }

    @Test
    fun testArithmeticExpressionToString() {
        assertEquals("(((5*(element*element))+(6*element))+7)",
            ArithmeticExpression(arrayListOf(7, 6, 5, 0)).toString())
        assertEquals("(((5*(element*element))+(6*element))+7)",
            ArithmeticExpression.from("(((5*(element*element))+(6*element))+7)").toString())
        assertEquals("(((5*(element*element))+(6*element))+7)",
            ArithmeticExpression.from("(((element*(5*element))+7)+(element*6))").toString())
        assertEquals("0",
            ArithmeticExpression(arrayListOf(0)).toString())
        assertEquals("(((7*((element*element)*element))+(6*(element*element)))+(5*element))",
            ArithmeticExpression(arrayListOf(0, 5, 6, 7)).toString())
        assertEquals("(((134*((element*element)*element))-(6*(element*element)))+5)",
            ArithmeticExpression(arrayListOf(5, 0, -6, 134)).toString())
        assertEquals("(((6*(element*element))+(42*element))+3)",
            ArithmeticExpression(arrayListOf(3, 42, 6)).toString())
        assertEquals("((-7*element)-8)",
            ArithmeticExpression(arrayListOf(-8, -7)).toString())
        assertEquals("(-70*((element*element)*element))",
            ArithmeticExpression(arrayListOf(0, 0, 0, -70)).toString())
        assertEquals("(((element*element)+(4*element))+4)",
            ArithmeticExpression.from("((element+2)*(element+2))").toString())
    }

    @Test
    fun testSimplify() {
        assertEquals("filter{((element>10)&(element<20))}%>%map{element}",
            simplify("filter{(element>10)}%>%filter{(element<20)}"))
        assertEquals("filter{((element+10)>10)}%>%map{(((element*element)+(20*element))+100)}",
            simplify("map{(element+10)}%>%filter{(element>10)}%>%map{(element*element)}"))
    }
}
