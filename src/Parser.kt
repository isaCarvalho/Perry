@Suppress("NON_EXHAUSTIVE_WHEN")
class Parser(private val lex: Lex) {
    private val bufferTokens = ArrayList<Token>()
    private var isEnd = false

    val scopes = Scopes()

    init {
        readToken()
    }

    companion object {
        const val TOKEN_BUFFER_SIZE = 5
    }

    private fun readToken() {
        if (bufferTokens.isNotEmpty()) {
            bufferTokens.removeAt(0)
        }

        while (bufferTokens.size < TOKEN_BUFFER_SIZE && !isEnd) {
            val next: Token = lex.getNextToken()
            bufferTokens.add(next)

            if (next.tokenType == TokenType.EOF) {
                isEnd = true
            }
        }
        println("Token ${peek()} read")
    }

    private fun peek(): Token? {
        return when {
            bufferTokens.isEmpty() -> null
            else -> bufferTokens[0]
        }
    }

    private fun match(type: TokenType): Token {
        val token = peek()

        if (token != null && token.tokenType == type) {
            println("Match: ${token.tokenType}")
            readToken()
        } else {
            syntacticError(type)
        }

        return token!!
    }

    private fun syntacticError(vararg types: TokenType) {
        throw Exception(
            "Syntactic error: expected one of the following (${types.joinToString(", ")}), " +
                    "but found ${peek()}"
        )
    }

    private fun variableNotDeclaredError(token: Token) {
        var msg = "Semantic error: variable $token not declared"

        val scope = scopes.getCurrentScope().name

        if (scope != "global") {
            msg += " in the scope of function '$scope'"
        }

        throw Exception(msg)
    }

    private fun unexpectedParameterError(functionName: String, expected : Int, found: Int) {
        throw Exception("Semantic error: function '$functionName' was expecting $expected parameter(s), but $found found")
    }

    private fun invalidArrayError(name: String) {
        throw Exception("Semantic error: $name is not an array")
    }

    private fun invalidRecordError(name: String) {
        throw Exception("Semantic error: $name is not a record")
    }

    // [PROGRAMA] => [DECLARACOES] [PRINCIPAL]
    fun program() {
        scopes.createNewScope("global")

        declarations()
        mainFunction()

        scopes.dropScope()
    }

    // [PRINCIPAL] => (begin) [COMANDO] [LISTA_COM] (end)
    private fun mainFunction() {
        match(TokenType.Begin)
        command()
        listCommand()
        match(TokenType.End)
    }

    // [DECLARACOES] => [DEF_CONST] [DEF_TIPOS] [DEF_VAR] [DEF_FUNC]
    private fun declarations() {
        defConst()
        defTypes()
        defVar()
        defFunc()
    }

    // [DEF_CONST] => [CONSTANTE] [DEF_CONST]
    // [DEF_CONST] => Є
    private fun defConst() {
        if (peek()!!.tokenType == TokenType.Const) {
            constant()
            defConst()
        }
    }

    // [DEF_TIPOS] => [TIPO] [DEF_TIPOS]
    // [DEF_TIPOS] => Є
    private fun defTypes() {
        if (peek()!!.tokenType == TokenType.Type) {
            type()
            defTypes()
        }
    }

    // [DEF_VAR] => [VARIAVEL] [DEF_VAR]
    // [DEF_VAR] => Є
    private fun defVar() {
        if (peek()!!.tokenType == TokenType.Var) {
            variable()
            defVar()
        }
    }

    // [DEF_FUNC] => [FUNCAO] [DEF_FUNC]
    // [DEF_FUNC] => Є
    private fun defFunc() {
        if (peek()!!.tokenType == TokenType.Function) {
            function()
            defFunc()
        }
    }

    // [ID] => Seqüência alfanumérica iniciada por char (tratado no léxico)
    private fun id(): Token {
        return match(TokenType.Identifier)
    }

    // [NUMERO] => seqüência numérica com a ocorrência de no máximo um ponto (tratado no léxico)
    private fun number() {
        when (val tokenType = peek()!!.tokenType) {
            TokenType.Integer -> match(TokenType.Integer)

            TokenType.Real -> match(TokenType.Real)

            else -> syntacticError(TokenType.Integer, TokenType.Real)
        }
    }

    // [CONSTANTE] => (const) [ID] (=) [CONST_VALOR] (;)
    private fun constant() {
        match(TokenType.Const)

        val idToken = id()
        match(TokenType.Equal)

        val valueToken = constValue()
        match(TokenType.Semicolon)

        scopes.getGlobalScope().insertSymbol(
            idToken = idToken,
            category = TokenType.Const,
            valueToken = valueToken
        )
    }

    // [CONST_VALOR] => Seqüência alfanumérica iniciada por aspas e terminada em aspas (tratado no léxico)
    // [CONST_VALOR] => [EXP_MAT]
    private fun constValue(): Token? {
        val tokenType = peek()!!.tokenType
        if (tokenType == TokenType.Text) {
            return match(TokenType.Text)
        } else if (tokenType == TokenType.Identifier || tokenType == TokenType.Integer || tokenType == TokenType.Real) {
            return expMath()
        } else {
            syntacticError(TokenType.Identifier, TokenType.Integer, TokenType.Real, TokenType.Text)
        }

        return null
    }

    // [TIPO] => (type) [ID] (=) [TIPO_DADO] (;)
    private fun type() {
        match(TokenType.Type)

        val idToken = id()
        match(TokenType.Equal)

        val dataTypeToken = dataType()
        match(TokenType.Semicolon)

        scopes.getGlobalScope().insertSymbol(
            idToken = idToken,
            category = TokenType.Type,
            valueToken = dataTypeToken
        )
    }

    // [VARIAVEL] => (var) [ID] [LISTA_ID] (:) [TIPO_DADO] (;)
    private fun variable() {
        match(TokenType.Var)

        val idToken = id()
        scopes.getCurrentScope().addIdentifier(idToken)

        idList()
        match(TokenType.Colon)

        val dataTypeToken = dataType()
        match(TokenType.Semicolon)

        scopes.getCurrentScope().identifiers.forEach {
            scopes.getCurrentScope().insertSymbol(
                idToken = it,
                category = TokenType.Var,
                valueToken = dataTypeToken
            )
        }

        scopes.getCurrentScope().clearIdentifiers()
    }

    // [LISTA_ID] => (,) [ID] [LISTA_ID]
    // [LISTA_ID] => Є
    private fun idList() {
        if (peek()!!.tokenType == TokenType.Comma) {
            match(TokenType.Comma)
            val idToken = id()

            scopes.getCurrentScope().addIdentifier(idToken)
            idList()
        }
    }

    // [CAMPOS] => [ID] (:) [TIPO_DADO] [LISTA_CAMPOS]
    // [CAMPOS] => Є
    private fun fields(isParameters: Boolean = false) {
        if (peek()!!.tokenType == TokenType.Identifier) {
            val idToken = id()

            match(TokenType.Colon)

            val dataTypeToken = dataType()

            if (isParameters) {
                scopes.getCurrentScope().addParameter(idToken, dataTypeToken)
                fieldList(true)
            } else {
                fieldList()
            }
        }
    }

    // [LISTA_CAMPOS] => (;) [CAMPOS] [LISTA_CAMPOS]
    // [LISTA_CAMPOS] => Є
    private fun fieldList(isParameters: Boolean = false) {
        if (peek()!!.tokenType == TokenType.Semicolon) {
            match(TokenType.Semicolon)
            fields(isParameters)
            fieldList(isParameters)
        }
    }

    // [TIPO_DADO] => (integer)
    // [TIPO_DADO] => (real)
    // [TIPO_DADO] => (array) ([) [NUMERO] (]) (of) [TIPO_DADO]
    // [TIPO_DADO] => (record) [CAMPOS] (end)
    // [TIPO_DADO] => [ID]
    private fun dataType(): Token {
        val token = peek()!!
        when (token.tokenType) {
            TokenType.Integer -> match(TokenType.Integer)

            TokenType.Real -> match(TokenType.Real)

            TokenType.Array -> {
                match(TokenType.Array)
                match(TokenType.LeftBracket)
                number()
                match(TokenType.RightBracket)
                match(TokenType.Of)
                dataType()
            }

            TokenType.Record -> {
                match(TokenType.Record)
                fields()
                match(TokenType.End)
            }

            TokenType.Identifier -> match(TokenType.Identifier)

            else -> syntacticError(
                TokenType.Integer,
                TokenType.Real,
                TokenType.Array,
                TokenType.Record,
                TokenType.Identifier
            )
        }

        return token
    }

    // [FUNCAO] => (function) [NOME_FUNCAO] [BLOCO_FUNCAO]
    private fun function() {
        match(TokenType.Function)

        functionName()
        functionBloc()

        scopes.dropScope()
    }

    // [NOME_FUNCAO] => [ID] [PARAM_FUNC] (:) [TIPO_DADO]
    private fun functionName() {
        val idToken = id()

        scopes.createNewScope(idToken.lexeme)

        val parameterSize = functionParameter(idToken.lexeme)
        match(TokenType.Colon)

        val dataTypeToken = dataType()

        scopes.getGlobalScope().insertSymbol(
            idToken = idToken,
            valueToken = dataTypeToken,
            category = TokenType.Function,
            parameterSize = parameterSize
        )
    }

    // [PARAM_FUNC] => (() [CAMPOS] ())
    // [PARAM_FUNC] => Є
    private fun functionParameter(functionName: String): Int {
        var parameterSize = 0

        if (peek()!!.tokenType == TokenType.LeftParenthesis) {
            match(TokenType.LeftParenthesis)
            fields(true)

            scopes.getCurrentScope().parameters.forEach {

                scopes.getCurrentScope().insertSymbol(
                    idToken = it.key,
                    category = it.key.tokenType,
                    valueToken = it.value
                )
            }

            parameterSize = scopes.getCurrentScope().parameters.size

            match(TokenType.RightParenthesis)
            scopes.getCurrentScope().clearParameter()
        }

        return parameterSize
    }

    // [BLOCO_FUNCAO] => [DEF_VAR] (begin) [COMANDO] [LISTA_COM] (end)
    private fun functionBloc() {
        defVar()
        match(TokenType.Begin)
        command()
        listCommand()
        match(TokenType.End)
    }

    // [LISTA_COM] => (;) [COMANDO] [LISTA_COM]
    // [LISTA_COM] => Є
    private fun listCommand() {
        if (peek()!!.tokenType == TokenType.Semicolon) {
            match(TokenType.Semicolon)
            command()
            listCommand()
        }
    }

    // [BLOCO] => (begin) [COMANDO] [LISTA_COM] (end)
    // [BLOCO] => [COMANDO]
    private fun bloc() {
        when (val tokenType = peek()!!.tokenType) {
            TokenType.Begin -> {
                match(TokenType.Begin)
                command()
                listCommand()
                match(TokenType.End)
            }

            TokenType.Identifier,
            TokenType.While,
            TokenType.If,
            TokenType.Write,
            TokenType.Read -> command()

            else -> syntacticError(
                TokenType.Begin,
                TokenType.Identifier,
                TokenType.While,
                TokenType.If,
                TokenType.Write,
                TokenType.Read
            )
        }
    }

    // [COMANDO] => [ID] [NOME] (:=) [EXP_MAT]
    // [COMANDO] => (while) [EXP_LOGICA] [BLOCO]
    // [COMANDO] => (if) [EXP_LOGICA] (then) [BLOCO] [ELSE]
    // [COMANDO] => (write) [CONST_VALOR]
    // [COMANDO] => (read) [ID] [NOME]
    private fun command() {
        when (val tokenType = peek()!!.tokenType) {
            TokenType.Identifier -> {
                val idToken = id()

                if (!scopes.matchNestedScopes(idToken.lexeme)) {
                    variableNotDeclaredError(idToken)
                }

                name(idToken.lexeme)
                match(TokenType.Assignment)
                expMath()
            }

            TokenType.While -> {
                match(TokenType.While)
                expLogical()
                bloc()
            }

            TokenType.If -> {
                match(TokenType.If)
                expLogical()
                match(TokenType.Then)
                bloc()
                elseCommand()
            }

            TokenType.Write -> {
                match(TokenType.Write)
                constValue()
            }

            TokenType.Read -> {
                match(TokenType.Read)
                val idToken = id()
                name(idToken.lexeme)
            }

            else -> syntacticError(TokenType.Identifier, TokenType.While, TokenType.If, TokenType.Write, TokenType.Read)
        }
    }

    // [ELSE] => (else) [BLOCO]
    // [ELSE] => Є
    private fun elseCommand() {
        if (peek()!!.tokenType == TokenType.Else) {
            match(TokenType.Else)
            bloc()
        }
    }

    // [LISTA_PARAM] => [PARAMETRO] [LIST_PARAM_2]
    // [LISTA_PARAM] => Є
    private fun listParameter() {
        val token = peek()!!
        val tokenType = token.tokenType

        if (tokenType == TokenType.Integer || tokenType == TokenType.Real || tokenType == TokenType.Identifier) {
            parameter(true)
            listParameter2()
        }
    }

    // [LIST_PARAM_2] => (,) [LISTA_PARAM]
    // [LIST_PARAM_2] => Є
    private fun listParameter2() {
        if (peek()!!.tokenType == TokenType.Comma) {
            match(TokenType.Comma)
            listParameter()
        }
    }

    // [EXP_LOGICA] => [EXP_ MAT] [EXP_LOGICA_2]
    private fun expLogical() {
        val tokenType = peek()!!.tokenType
        if (tokenType == TokenType.Integer || tokenType == TokenType.Real || tokenType == TokenType.Identifier) {
            expMath()
            expLogical2()
        }
    }

    // [EXP_LOGICA_2] => [OP_LOGICO] [EXP_LOGICA]
    // [EXP_LOGICA_2] => Є
    private fun expLogical2() {
        val tokenType = peek()!!.tokenType
        if (tokenType == TokenType.Equal ||
            tokenType == TokenType.LessThan ||
            tokenType == TokenType.MoreThan ||
            tokenType == TokenType.Exclamation
        ) {
            opLogical()
            expLogical()
        }
    }

    // [EXP_MAT] => [PARAMETRO] [EXP_MAT_2]
    private fun expMath(): Token {
        val token = peek()!!
        if (token.tokenType == TokenType.Integer || token.tokenType == TokenType.Real || token.tokenType == TokenType.Identifier) {
            parameter()
            expMath2()
        }

        return token
    }

    // [EXP_MAT] => [OP_ MAT] [EXP_ MAT]
    // [EXP_MAT] => Є
    private fun expMath2() {
        val tokenType = peek()!!.tokenType
        if (tokenType == TokenType.Plus ||
            tokenType == TokenType.Minus ||
            tokenType == TokenType.Times ||
            tokenType == TokenType.Divider
        ) {
            opMath()
            expMath()
        }
    }

    // [PARAMETRO] => [ID] [NOME]
    // [PARAMETRO] => [NUMERO]
    private fun parameter(isListParameter: Boolean = false) {
        val token = peek()!!
        var dataTypeToken: Token? = null

        when (token.tokenType) {
            TokenType.Identifier -> {
                dataTypeToken = match(TokenType.Identifier)
                name(token.lexeme)
            }

            TokenType.Integer -> dataTypeToken = match(TokenType.Integer)

            TokenType.Real -> dataTypeToken = match(TokenType.Real)

            else -> syntacticError()
        }

        if (isListParameter) {
            scopes.getGlobalScope().addParameter(token, dataTypeToken!!)
        }
    }

    // [OP_LOGICO] => (>) | (<) | (=) | (!)
    private fun opLogical() {
        when (peek()!!.tokenType) {
            TokenType.LessThan -> {
                match(TokenType.LessThan)
            }
            TokenType.MoreThan -> {
                match(TokenType.MoreThan)
            }
            TokenType.Equal -> {
                match(TokenType.Equal)
            }
            TokenType.Exclamation -> {
                match(TokenType.Exclamation)
            }
        }
    }

    // [OP_MAT] => (+) | (-) | (*) | (/)
    private fun opMath() {
        when (peek()!!.tokenType) {
            TokenType.Plus -> {
                match(TokenType.Plus)
            }
            TokenType.Minus -> {
                match(TokenType.Minus)
            }
            TokenType.Divider -> {
                match(TokenType.Divider)
            }
            TokenType.Times -> {
                match(TokenType.Times)
            }
        }
    }

    // [NOME] => (.) [ID] [NOME]
    // [NOME] => ([) [PARAMETRO] (])
    // [NOME] => (() [LISTA_PARAM] ())
    // [NOME] => Є
    private fun name(name: String) {
        when (peek()!!.tokenType) {
            TokenType.Dot -> {
                match(TokenType.Dot)
                val idToken = id()

                if (!validateType(name, TokenType.Record)) {
                    invalidRecordError(name)
                }

                name(idToken.lexeme)
            }
            TokenType.LeftBracket -> {
                match(TokenType.LeftBracket)
                parameter()
                match(TokenType.RightBracket)

                if (!validateType(name, TokenType.Array)) {
                    invalidArrayError(name)
                }
            }
            TokenType.LeftParenthesis -> {
                match(TokenType.LeftParenthesis)
                listParameter()

                validateParametersSize(name)

                match(TokenType.RightParenthesis)
            }
        }
    }

    private fun validateParametersSize(name: String) {
        // Verifica se o numero de parqmetros passados pra função é igual ao declarado
        val global = scopes.getGlobalScope()
        val expected = global.getSymbol(name)!!.parameterSize
        val found = global.parameters.size
        if (found != expected) {
            unexpectedParameterError(name, expected!!, found)
        }
        scopes.getGlobalScope().clearParameter()
    }

    private fun validateType(name: String, type: TokenType) : Boolean {
        var isValid = false
        scopes.goThoughtAlignScopes().forEach {
            val symbol = it.getSymbol(name)
            if (symbol != null) {
                isValid = when {
                    symbol.type == type.toString() -> {
                        true
                    }
                    TokenType.values().joinToString(",").contains(symbol.type.toString()) -> {
                        false
                    }
                    else -> {
                        validateType(symbol.type.toString(), type)
                    }
                }

                if (isValid)
                    return@forEach
            }
        }

        return isValid
    }
}