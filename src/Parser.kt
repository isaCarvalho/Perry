@Suppress("NON_EXHAUSTIVE_WHEN")
class Parser(private val lex: Lex) {
    private val bufferTokens = ArrayList<Token>()
    private var isEnd = false

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

    // [PROGRAMA] => [DECLARACOES] [PRINCIPAL]
    fun program() {
        declarations()
        mainFunction()
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
    private fun id() {
        match(TokenType.Identifier)
    }

    // [NUMERO] => seqüência numérica com a ocorrência de no máximo um ponto (tratado no léxico)
    private fun number() {
        when (peek()!!.tokenType) {
            TokenType.Integer -> match(TokenType.Integer)

            TokenType.Real -> match(TokenType.Real)

            else -> syntacticError(TokenType.Integer, TokenType.Real)
        }
    }

    // [CONSTANTE] => (const) [ID] (=) [CONST_VALOR] (;)
    private fun constant() {
        match(TokenType.Const)
        id()
        match(TokenType.Equal)
        constValue()
        match(TokenType.Semicolon)
    }

    // [CONST_VALOR] => Seqüência alfanumérica iniciada por aspas e terminada em aspas (tratado no léxico)
    // [CONST_VALOR] => [EXP_MAT]
    private fun constValue() {
        val tokenType = peek()!!.tokenType
        if (tokenType == TokenType.Text) {
            match(TokenType.Text)
        } else if (tokenType == TokenType.Identifier || tokenType == TokenType.Integer || tokenType == TokenType.Real) {
            expMath()
        } else {
            syntacticError(TokenType.Identifier, TokenType.Integer, TokenType.Real, TokenType.Text)
        }
    }

    // [TIPO] => (type) [ID] (=) [TIPO_DADO] (;)
    private fun type() {
        match(TokenType.Type)
        id()
        match(TokenType.Equal)
        dataType()
        match(TokenType.Semicolon)
    }

    // [VARIAVEL] => (var) [ID] [LISTA_ID] (:) [TIPO_DADO] (;)
    private fun variable() {
        match(TokenType.Var)
        id()
        idList()
        match(TokenType.Colon)
        dataType()
        match(TokenType.Semicolon)
    }

    // [LISTA_ID] => (,) [ID] [LISTA_ID]
    // [LISTA_ID] => Є
    private fun idList() {
        if (peek()!!.tokenType == TokenType.Comma) {
            match(TokenType.Comma)
            id()
            idList()
        }
    }

    // [CAMPOS] => [ID] (:) [TIPO_DADO] [LISTA_CAMPOS]
    // [CAMPOS] => Є
    private fun fields() {
        if (peek()!!.tokenType == TokenType.Identifier) {
            id()
            match(TokenType.Colon)
            dataType()
            fieldList()
        }
    }

    // [LISTA_CAMPOS] => (;) [CAMPOS] [LISTA_CAMPOS]
    // [LISTA_CAMPOS] => Є
    private fun fieldList() {
        if (peek()!!.tokenType == TokenType.Semicolon) {
            match(TokenType.Semicolon)
            fields()
            fieldList()
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
    }

    // [NOME_FUNCAO] => [ID] [PARAM_FUNC] (:) [TIPO_DADO]
    private fun functionName() {
        id()
        functionParameter()
        match(TokenType.Colon)
        dataType()
    }

    // [PARAM_FUNC] => (() [CAMPOS] ())
    // [PARAM_FUNC] => Є
    private fun functionParameter() {
        if (peek()!!.tokenType == TokenType.LeftParenthesis) {
            match(TokenType.LeftParenthesis)
            fields()
            match(TokenType.RightParenthesis)
        }
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
        when (peek()!!.tokenType) {
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
        when (peek()!!.tokenType) {
            TokenType.Identifier -> {
                id()
                name()
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
                id()
                name()
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
            parameter()
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
    private fun expMath() {
        val token = peek()!!
        if (token.tokenType == TokenType.Integer || token.tokenType == TokenType.Real || token.tokenType == TokenType.Identifier) {
            parameter()
            expMath2()
        }
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
    private fun parameter() {
        when (peek()!!.tokenType) {
            TokenType.Identifier -> {
                match(TokenType.Identifier)
                name()
            }

            TokenType.Integer -> match(TokenType.Integer)

            TokenType.Real -> match(TokenType.Real)

            else -> syntacticError()
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
    private fun name(){
        val nameToken = peek()!!

        when (nameToken.tokenType) {
            TokenType.Dot -> {
                match(TokenType.Dot)
                id()
                name()
            }

            TokenType.LeftBracket -> {
                match(TokenType.LeftBracket)
                parameter()
                match(TokenType.RightBracket)
            }

            TokenType.LeftParenthesis -> {
                match(TokenType.LeftParenthesis)
                listParameter()
                match(TokenType.RightParenthesis)
            }
        }
    }
}