import java.lang.Exception

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
        println("Token ${lookahead(1)} read")
    }

    private fun lookahead(k: Int): Token? {
        val quantityOfElementsIntoTheBuffer = bufferTokens.size - 1

        return when {
            bufferTokens.isEmpty() -> null

            k - 1 >= quantityOfElementsIntoTheBuffer -> bufferTokens[quantityOfElementsIntoTheBuffer]

            else -> bufferTokens[k - 1]
        }
    }

    private fun match(type: TokenType) {
        val token = lookahead(1)

        if (token != null && token.tokenType == type) {
            println("Match: ${token.tokenType}")
        } else {
            syntacticError(type.toString())
        }
    }

    private fun syntacticError(vararg types: String) {
        throw Exception(
            "Syntactic error: expected one of the following (${types.joinToString(", ")}), " +
                    "but found ${lookahead(1)}"
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
        if (lookahead(1)!!.tokenType == TokenType.Const) {
            constant()
            defConst()
        }
    }

    // [DEF_TIPOS] => [TIPO] [DEF_TIPOS]
    // [DEF_TIPOS] => Є
    private fun defTypes() {
        if (lookahead(1)!!.tokenType == TokenType.Type) {
            type()
            defTypes()
        }
    }

    // [DEF_VAR] => [VARIAVEL] [DEF_VAR]
    // [DEF_VAR] => Є
    private fun defVar() {
        if (lookahead(1)!!.tokenType == TokenType.Var) {
            variable()
            defVar()
        }
    }

    // [DEF_FUNC] => [FUNCAO] [DEF_FUNC]
    // [DEF_FUNC] => Є
    private fun defFunc() {
        if (lookahead(1)!!.tokenType == TokenType.Function) {
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
        match(TokenType.Number)
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
        val tokenType = lookahead(1)!!.tokenType
        if (tokenType == TokenType.Text) {
            match(TokenType.Text)
        } else if (tokenType == TokenType.Identifier || tokenType == TokenType.Number) {
            expMath()
        } else {
            syntacticError(tokenType.toString())
        }
    }

    // [TIPO] => (type) [ID] (=) [TIPO_DADO] (;)
    private fun type() {

    }

    // [VARIAVEL] => (var) [ID] [LISTA_ID] (:) [TIPO_DADO] (;)
    private fun variable() {

    }

    // [LISTA_ID] => (,) [ID] [LISTA_ID]
    // [LISTA_ID] => Є
    private fun idList() {

    }

    // [CAMPOS] => [ID] (:) [TIPO_DADO] [LISTA_CAMPOS]
    private fun fields() {

    }

    // [LISTA_CAMPOS] => (;) [CAMPOS] [LISTA_CAMPOS]
    // [LISTA_CAMPOS] => Є
    private fun fieldList() {

    }

    // [TIPO_DADO] => (integer)
    // [TIPO_DADO] => (real)
    // [TIPO_DADO] => (array) ([) [NUMERO] (]) (of) [TIPO_DADO]
    // [TIPO_DADO] => (record) [CAMPOS] (end)
    // [TIPO_DADO] => [ID]
    private fun dataType() {

    }

    // [FUNCAO] => (function) [NOME_FUNCAO] [BLOCO_FUNCAO]
    private fun function() {

    }

    // [NOME_FUNCAO] => [ID] [PARAM_FUNC] (:) [TIPO_DADO]
    private fun functionName() {

    }

    // [PARAM_FUNC] => (() [CAMPOS] ())
    // [PARAM_FUNC] => Є
    private fun functionParameter() {

    }

    // [BLOCO_FUNCAO] => [DEF_VAR] (begin) [COMANDO] [LISTA_COM] (end)
    private fun functionBloc() {

    }

    // [LISTA_COM] => (;) [COMANDO] [LISTA_COM]
    // [LISTA_COM] => Є
    private fun listCommand() {

    }

    // [BLOCO] => (begin) [COMANDO] [LISTA_COM] (end)
    // [BLOCO] => [COMANDO]
    private fun bloc() {

    }

    // [COMANDO] => [ID] [NOME] (:=) [EXP_MAT]
    // [COMANDO] => (while) [EXP_LOGICA] [BLOCO]
    // [COMANDO] => (if) [EXP_LOGICA] (then) [BLOCO] [ELSE]
    // [COMANDO] => (write) [CONST_VALOR]
    // [COMANDO] => (read) [ID] [NOME]
    private fun command() {

    }

    // [ELSE] => (else) [BLOCO]
    // [ELSE] => Є
    private fun elseCommand() {

    }

    // [LISTA_PARAM] => [PARAMETRO] (,) [LISTA_PARAM]
    // [LISTA_PARAM] => [PARAMETRO]
    // [LISTA_PARAM] => Є
    private fun listParameter() {

    }

    // [EXP_LOGICA] => [EXP_ MAT] [OP_LOGICO] [EXP_LOGICA]
    // [EXP_LOGICA] => [EXP_ MAT]
    private fun expLogical() {

    }

    // [EXP_MAT] => [PARAMETRO] [OP_ MAT] [EXP_ MAT]
    // [EXP_MAT] => [PARAMETRO]
    private fun expMath() {

    }

    // [PARAMETRO] => [ID] [NOME]
    // [PARAMETRO] => [NUMERO]
    private fun parameter() {

    }

    // [OP_LOGICO] => (>) | (<) | (=) | (!)
    private fun opLogical() {

    }

    // [OP_MAT] => (+) | (-) | (*) | (/)
    private fun opMath() {

    }

    // [NOME] => (.) [ID] [NOME]
    // [NOME] => ([) [PARAMETRO] (])
    // [NOME] => (() [LISTA_PARAM] ())
    // [NOME] => Є
    private fun name() {

    }
}