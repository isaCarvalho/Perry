import java.util.LinkedHashMap

class TableOfSymbol {

    // A classificação é o tipo ao qual o lexema se refere
    class Symbol(
        val lexeme: String,
        val token: Token,
        val category: TokenType,
        val type: String? = null,
        var value: String? = null,
        var used: Boolean = false,
        var scope: String = "global",
        val parameterSize: Int? = null
    ) {
        override fun toString(): String {
            return "Lexeme: $lexeme, Token: $token, Category: $category, Type: ${type ?: "- "}, Value: ${value ?: "- "}, Parameters Size: ${parameterSize ?: "- "}, Scope: $scope, Used: $used"
        }
    }

    var identifiers = ArrayList<Token>()
    private val symbolTable = LinkedHashMap<Token, Symbol>()

    var parameters = HashMap<Token, Token>()

    fun insertSymbol(idToken: Token, category: TokenType, valueToken: Token?, scope: String = "global", parameterSize: Int? = null) {
        symbolTable[idToken] = Symbol(
            lexeme = idToken.lexeme,
            token = idToken,
            category = category,
            value = valueToken?.lexeme,
            type = if (valueToken?.tokenType == TokenType.Identifier) valueToken.lexeme else valueToken?.tokenType.toString(),
            scope = scope,
            parameterSize = parameterSize
        )
    }

    fun printSymbolTable() {
        symbolTable.forEach {
            println(it.value)
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
}