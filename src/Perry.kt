fun main() {
    val lex = Lex("src/program.pas")
    val parser = Parser(lex)
    parser.program()
}