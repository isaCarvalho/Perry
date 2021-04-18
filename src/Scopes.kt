import java.util.*

class Scopes {
    private val tableStack = LinkedList<TableOfSymbol>()

    fun createNewScope(name: String) = tableStack.push(TableOfSymbol(name))

    fun getCurrentScope(): TableOfSymbol = tableStack.peek()

    fun getGlobalScope() = tableStack.last()

    fun goThoughtAlignScopes(): List<TableOfSymbol> = tableStack

    fun dropScope(): TableOfSymbol = tableStack.pop()

    fun matchNestedScopes(lexeme: String): Boolean {
        tableStack.forEach {
            if (it.match(lexeme)) {
                return true
            }
        }

        return false
    }
}