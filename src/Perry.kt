fun main() {
    val lex = Lex("src/program.pas")

    var token = lex.getNextToken()

    while (token != null && token.tokenType != TokenType.EOF) {
        println(token)
        token = lex.getNextToken()
    }
}