import java.util.LinkedHashMap

class TableOfSymbol(val name: String) {
    // A classificação é o tipo ao qual o lexema se refere
    class Symbol(
        val lexeme: String,
        val token: Token,
        val category: TokenType,
        val type: String? = null,
        var value: String? = null,
        var used: Boolean = false,
        val parameterSize: Int? = null
    ) {
        override fun toString(): String {
            return "Lexeme: $lexeme, Token: $token, Category: $category, Type: ${type ?: "- "}, Value: ${value ?: "- "}, Parameters Size: ${parameterSize ?: "- "}, Used: $used"
        }
    }

    var identifiers = ArrayList<Token>()
    private val symbolTable = LinkedHashMap<String, Symbol>()

    var parameters = HashMap<Token, Token>()

    fun insertSymbol(
        idToken: Token,
        category: TokenType,
        valueToken: Token?,
        parameterSize: Int? = null
    ) {
        symbolTable[idToken.lexeme] = Symbol(
            lexeme = idToken.lexeme,
            token = idToken,
            category = category,
            value = valueToken?.lexeme,
            type = if (valueToken?.tokenType == TokenType.Identifier) valueToken.lexeme else valueToken?.tokenType.toString(),
            parameterSize = parameterSize
        )
    }

    fun printSymbolTable() {
        println("Scope: $name\n")

        symbolTable.forEach {
            println(it)
        }
    }

    fun addIdentifier(token: Token) {
        identifiers.add(token)
    }

    fun clearIdentifiers() {
        identifiers.clear()
    }

    fun addParameter(idToken: Token, valueToken: Token) = parameters.put(idToken, valueToken)

    fun clearParameter() = parameters.clear()

    fun getSymbol(lexeme: String) = symbolTable[lexeme]

    fun match(lexeme: String) = symbolTable.containsKey(lexeme)
}