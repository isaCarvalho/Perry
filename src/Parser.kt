import exceptions.ParserException

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
    }

    private fun peek(k: Int = 1): Token? {
        return when {
            bufferTokens.isEmpty() -> null

            k - 1 >= bufferTokens.size -> bufferTokens[bufferTokens.size - 1]

            else -> bufferTokens[k - 1]
        }
    }

    private fun match(type: TokenType): Token {
        val token = peek()

        if (token != null && token.tokenType == type) {
            readToken()
        } else {
            throw ParserException(token, type)
        }

        return token
    }

    // [PROGRAMA] => [DECLARACOES] [PRINCIPAL]
    fun program(): Program {
        val statements: MutableList<Statement> = statements()
        val bloc = mainFunction()

        return Program(statements, bloc)
    }

    // [PRINCIPAL] => (begin) [COMANDO] [LISTA_COM] (end)
    private fun mainFunction(): Bloc {
        val commands = mutableListOf<Command>()

        match(TokenType.Begin)
        commands.add(command())
        commands.addAll(listCommand())
        match(TokenType.End)

        return Bloc(commands)
    }

    // [DECLARACOES] => [DEF_CONST] [DEF_TIPOS] [DEF_VAR] [DEF_FUNC]
    private fun statements(): MutableList<Statement> {
        val statements = ArrayList<Statement>()

        statements.addAll(defConst())
        statements.addAll(defTypes())
        statements.addAll(defVar())
        statements.addAll(defFunc())

        return statements
    }

    // [DEF_CONST] => [CONSTANTE] [DEF_CONST]
    // [DEF_CONST] => ??
    private fun defConst(): MutableList<ConstStat> {
        val constants = ArrayList<ConstStat>()

        while (peek()!!.tokenType == TokenType.Const) {
            constants.add(constStat())
        }

        return constants
    }

    // [DEF_TIPOS] => [TIPO] [DEF_TIPOS]
    // [DEF_TIPOS] => ??
    private fun defTypes(): MutableList<Statement> {
        val types = ArrayList<Statement>()

        while (peek()!!.tokenType == TokenType.Type) {
            types.add(type())
        }

        return types
    }

    // [DEF_VAR] => [VARIAVEL] [DEF_VAR]
    // [DEF_VAR] => ??
    private fun defVar(): MutableList<VarStat> {
        val vars = ArrayList<VarStat>()

        while (peek()!!.tokenType == TokenType.Var) {
            vars.addAll(variable())
        }

        return vars
    }

    // [DEF_FUNC] => [FUNCAO] [DEF_FUNC]
    // [DEF_FUNC] => ??
    private fun defFunc(): MutableList<FunctionStat> {
        val functions = ArrayList<FunctionStat>()

        while (peek()!!.tokenType == TokenType.Function) {
            functions.add(function())
        }

        return functions
    }

    // [ID] => Seq????ncia alfanum??rica iniciada por char (tratado no l??xico)
    private fun id(): Token {
        return match(TokenType.Identifier)
    }

    // [NUMERO] => seq????ncia num??rica com a ocorr??ncia de no m??ximo um ponto (tratado no l??xico)
    private fun number(): Command {
        val token = peek()!!

        return when (token.tokenType) {
            TokenType.Integer -> Integer(match(TokenType.Integer).lexeme)

            TokenType.Real -> Real(match(TokenType.Real).lexeme)

            else -> throw ParserException(token, TokenType.Integer, TokenType.Real)
        }
    }

    // [CONSTANTE] => (const) [ID] (=) [CONST_VALOR] (;)
    private fun constStat(): ConstStat {
        match(TokenType.Const)
        val idToken: Token = id()
        match(TokenType.Equal)
        val value = constValue()
        match(TokenType.Semicolon)

        return ConstStat(
            name = idToken.lexeme,
            ast = value
        )
    }

    // [CONST_VALOR] => Seq????ncia alfanum??rica iniciada por aspas e terminada em aspas (tratado no l??xico)
    // [CONST_VALOR] => [EXP_MAT]
    private fun constValue(): Command {
        val token = peek()!!
        val tokenType = token.tokenType

        return if (tokenType == TokenType.Text) {
            match(TokenType.Text)
            Text(token.lexeme)
        } else if (tokenType == TokenType.Identifier || tokenType == TokenType.Integer || tokenType == TokenType.Real) {
            return expMath()
        } else {
            throw ParserException(
                token,
                TokenType.Identifier,
                TokenType.Integer,
                TokenType.Real,
                TokenType.Text
            )
        }
    }

    // [TIPO] => (type) [ID] (=) [TIPO_DADO] (;)
    private fun type(): Statement {
        match(TokenType.Type)
        val idToken = id()
        match(TokenType.Equal)
        val dataType = dataType(idToken.lexeme)
        match(TokenType.Semicolon)

        return when (dataType) {
            is Array -> ArrayStat(
                name = idToken.lexeme,
                type = dataType.dataType,
                size = dataType.size.toInt()
            )

            is Record -> RecordStat(
                name = idToken.lexeme,
                fields = dataType.fields,
                type = dataType
            )

            else -> TypeStat(
                name = idToken.lexeme,
                type = dataType
            )
        }
    }

    // [VARIAVEL] => (var) [ID] [LISTA_ID] (:) [TIPO_DADO] (;)
    private fun variable(): MutableList<VarStat> {
        match(TokenType.Var)
        val idToken = id()
        val idsToken = idList()
        match(TokenType.Colon)
        val dataType = dataType(idToken.lexeme)
        match(TokenType.Semicolon)

        val vars = ArrayList<VarStat>()

        vars.add(VarStat(idToken.lexeme, dataType))
        idsToken.forEach {
            vars.add(VarStat(it.lexeme, dataType))
        }

        return vars
    }

    // [LISTA_ID] => (,) [ID] [LISTA_ID]
    // [LISTA_ID] => ??
    private fun idList(): List<Token> {
        val ids = ArrayList<Token>()

        while (peek()!!.tokenType == TokenType.Comma) {
            match(TokenType.Comma)
            ids.add(id())
        }

        return ids
    }

    // [CAMPOS] => [ID] (:) [TIPO_DADO] [LISTA_CAMPOS]
    // [CAMPOS] => ??
    // [LISTA_CAMPOS] => (;) [CAMPOS] [LISTA_CAMPOS]
    // [LISTA_CAMPOS] => ??
    private fun fields(): MutableList<Field> {
        val fields = ArrayList<Field>()

        if (peek()!!.tokenType == TokenType.Identifier) {
            var idToken = id()
            match(TokenType.Colon)
            var dataType = dataType(idToken.lexeme)

            fields.add(
                Field(
                    name = idToken.lexeme,
                    dataType = dataType
                )
            )

            while (peek()!!.tokenType == TokenType.Semicolon) {
                match(TokenType.Semicolon)

                if (peek()!!.tokenType == TokenType.Identifier) {
                    idToken = id()

                    match(TokenType.Colon)
                    dataType = dataType(idToken.lexeme)

                    fields.add(
                        Field(
                            name = idToken.lexeme,
                            dataType = dataType
                        )
                    )
                }
            }
        }

        return fields
    }

    // [TIPO_DADO] => (integer)
    // [TIPO_DADO] => (real)
    // [TIPO_DADO] => (array) ([) [NUMERO] (]) (of) [TIPO_DADO]
    // [TIPO_DADO] => (record) [CAMPOS] (end)
    // [TIPO_DADO] => [ID]
    private fun dataType(name: String): Command {
        val token = peek()!!
        return when (token.tokenType) {
            TokenType.Integer -> Integer(match(TokenType.Integer).lexeme)

            TokenType.Real -> Real(match(TokenType.Real).lexeme)

            TokenType.Array -> {
                match(TokenType.Array)
                match(TokenType.LeftBracket)
                val number = number()
                match(TokenType.RightBracket)
                match(TokenType.Of)
                val dataType = dataType("")

                if (number !is Integer) {
                    throw ParserException(
                        token,
                        TokenType.Integer
                    )
                }

                Array(
                    name = name,
                    size = number.value,
                    dataType = dataType
                )
            }

            TokenType.Record -> {
                match(TokenType.Record)
                val fields = fields()
                match(TokenType.End)

                Record(
                    name = name,
                    fields = fields
                )
            }

            TokenType.Identifier -> CreateDataType(match(TokenType.Identifier).lexeme)

            else -> throw ParserException(
                token,
                TokenType.Integer,
                TokenType.Real,
                TokenType.Array,
                TokenType.Record,
                TokenType.Identifier
            )
        }
    }

    // [FUNCAO] => (function) [NOME_FUNCAO] [BLOCO_FUNCAO]
    // [NOME_FUNCAO] => [ID] [PARAM_FUNC] (:) [TIPO_DADO]
    private fun function(): FunctionStat {
        match(TokenType.Function)

        val idToken = id()

        val parameters = functionParameter()
        match(TokenType.Colon)

        val dataType = dataType(idToken.lexeme)

        val bloc = functionBloc()

        return FunctionStat(
            name = idToken.lexeme,
            type = dataType,
            parameters = parameters,
            bloc = bloc
        )
    }

    // [PARAM_FUNC] => (() [CAMPOS] ())
    // [PARAM_FUNC] => ??
    private fun functionParameter(): MutableList<Field> {
        var fields = mutableListOf<Field>()

        if (peek()!!.tokenType == TokenType.LeftParenthesis) {
            match(TokenType.LeftParenthesis)
            fields = fields()
            match(TokenType.RightParenthesis)
        }

        return fields
    }

    // [BLOCO_FUNCAO] => [DEF_VAR] (begin) [COMANDO] [LISTA_COM] (end)
    private fun functionBloc(): Bloc {
        val statements = mutableListOf<Statement>()
        val commands = mutableListOf<Command>()

        statements.addAll(defVar())
        match(TokenType.Begin)
        commands.add(command())
        commands.addAll(listCommand())
        match(TokenType.End)

        return Bloc(commands, statements)
    }

    // [LISTA_COM] => (;) [COMANDO] [LISTA_COM]
    // [LISTA_COM] => ??
    private fun listCommand(): MutableList<Command> {
        val commands = mutableListOf<Command>()

        while (peek()!!.tokenType == TokenType.Semicolon) {
            match(TokenType.Semicolon)
            commands.add(command())
        }

        return commands
    }

    // [BLOCO] => (begin) [COMANDO] [LISTA_COM] (end)
    // [BLOCO] => [COMANDO]
    private fun bloc(): Bloc {
        val commands = mutableListOf<Command>()

        val token = peek()!!

        when (token.tokenType) {
            TokenType.Begin -> {
                match(TokenType.Begin)
                commands.add(command())

                while (peek()!!.tokenType == TokenType.Semicolon) {
                    match(TokenType.Semicolon)
                    commands.add(command())
                }

                match(TokenType.End)
            }

            TokenType.Identifier,
            TokenType.While,
            TokenType.If,
            TokenType.Write,
            TokenType.Read -> commands.add(command())

            else -> throw ParserException(
                token,
                TokenType.Begin,
                TokenType.Identifier,
                TokenType.While,
                TokenType.If,
                TokenType.Write,
                TokenType.Read
            )
        }

        return Bloc(commands)
    }

    // [COMANDO] => [ID] [NOME] (:=) [EXP_MAT]
    // [COMANDO] => (while) [EXP_LOGICA] [BLOCO]
    // [COMANDO] => (if) [EXP_LOGICA] (then) [BLOCO] [ELSE]
    // [COMANDO] => (write) [CONST_VALOR]
    // [COMANDO] => (read) [ID] [NOME]
    private fun command(): Command {
        val token = peek()!!

        return when (token.tokenType) {
            TokenType.Identifier -> {
                id()
                val left = name(token.lexeme)
                match(TokenType.Assignment)
                val right = expMath()

                BinOp(
                    left = left,
                    right = right,
                    operator = "="
                )
            }

            TokenType.While -> {
                match(TokenType.While)
                val condition = expLogical()
                val bloc = bloc()

                While(
                    condition = condition,
                    bloc = bloc
                )
            }

            TokenType.If -> {
                match(TokenType.If)
                val condition = expLogical()
                match(TokenType.Then)
                val bloc = bloc()
                val elseBloc = elseCommand()

                If(
                    condition = condition,
                    bloc = bloc,
                    elseBloc = elseBloc
                )
            }

            TokenType.Write -> {
                match(TokenType.Write)
                val value = constValue()

                Write(value)
            }

            TokenType.Read -> {
                match(TokenType.Read)
                val idToken = id()
                val varUsage = name(idToken.lexeme)

                Read(varUsage)
            }

            else -> throw ParserException(
                token,
                TokenType.Identifier,
                TokenType.While,
                TokenType.If,
                TokenType.Write,
                TokenType.Read
            )
        }
    }

    // [ELSE] => (else) [BLOCO]
    // [ELSE] => ??
    private fun elseCommand(): Bloc? {
        if (peek()!!.tokenType == TokenType.Else) {
            match(TokenType.Else)
            return bloc()
        }

        return null
    }

    // [LISTA_PARAM] => [PARAMETRO] (,) [LISTA_PARAM]
    // [LISTA_PARAM] => [PARAMETRO]
    // [LISTA_PARAM] => ??
    private fun listParameter(): MutableList<Command> {
        val listParameter = mutableListOf<Command>()

        var token = peek()!!

        while (token.tokenType == TokenType.Integer
            || token.tokenType == TokenType.Real
            || token.tokenType == TokenType.Identifier
        ) {
            val parameter = parameter()

            listParameter.add(parameter)

            if (peek()!!.tokenType == TokenType.Comma) {
                match(TokenType.Comma)
            }

            token = peek()!!
        }

        return listParameter
    }

    // [EXP_LOGICA] => [EXP_ MAT] [OP_LOGICO] [EXP_LOGICA]
    // [EXP_LOGICA] => [EXP_ MAT]
    private fun expLogical(): Command {

        val left = expMath()

        val token = peek(1)!!

        return when (token.tokenType) {
            TokenType.LessThan -> {
                match(TokenType.LessThan)
                BinOp(left, expLogical(), "<")
            }
            TokenType.MoreThan -> {
                match(TokenType.MoreThan)
                BinOp(left, expLogical(), ">")
            }
            TokenType.Equal -> {
                match(TokenType.Equal)
                BinOp(left, expLogical(), "=")
            }
            TokenType.Exclamation -> {
                match(TokenType.Exclamation)
                BinOp(left, expLogical(), "!")
            }
            else -> left
        }
    }

    // [EXP_MAT] => [PARAMETRO] [OP_ MAT] [EXP_ MAT]
    // [EXP_MAT] => [PARAMETRO]
    private fun expMath(): Command {
        val left = parameter()

        val token = peek(1)!!

        return when (token.tokenType) {
            TokenType.Plus -> {
                match(TokenType.Plus)
                BinOp(left, expMath(), "+")
            }
            TokenType.Minus -> {
                match(TokenType.Minus)
                BinOp(left, expMath(), "-")
            }
            TokenType.Divider -> {
                match(TokenType.Divider)
                BinOp(left, expMath(), "/")
            }
            TokenType.Times -> {
                match(TokenType.Times)
                BinOp(left, expMath(), "*")
            }
            else -> left
        }
    }

    // [PARAMETRO] => [ID] [NOME]
    // [PARAMETRO] => [NUMERO]
    private fun parameter(): Command {
        val token = peek()!!

        return when (token.tokenType) {
            TokenType.Identifier -> {
                match(TokenType.Identifier)
                name(token.lexeme)
            }

            TokenType.Integer -> {
                match(TokenType.Integer)
                Integer(value = token.lexeme)
            }

            TokenType.Real -> {
                match(TokenType.Real)
                Real(value = token.lexeme)
            }

            else -> throw ParserException(
                token,
                TokenType.Identifier,
                TokenType.Integer,
                TokenType.Real
            )
        }
    }

    // [NOME] => (.) [ID] [NOME]
    // [NOME] => ([) [PARAMETRO] (])
    // [NOME] => (() [LISTA_PARAM] ())
    // [NOME] => ??
    private fun name(name: String): Command {
        val nameToken = peek()!!

        return when (nameToken.tokenType) {
            TokenType.Dot -> {
                match(TokenType.Dot)
                val idToken = id()
                val child = name(idToken.lexeme)

                RecordCommand(name = name, child = child)
            }

            TokenType.LeftBracket -> {
                match(TokenType.LeftBracket)
                val varToken = parameter()
                match(TokenType.RightBracket)

                ArrayCommand(name = name, child = varToken)
            }

            TokenType.LeftParenthesis -> {
                match(TokenType.LeftParenthesis)
                val parameters = listParameter()
                match(TokenType.RightParenthesis)

                FunctionCommand(name = name, parameters = parameters)
            }

            else -> Identifier(name)
        }
    }
}