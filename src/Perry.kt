fun main() {
    val lex = Lex("src/program.pas")
    val parser = Parser(lex)
    val ast = parser.program()

    ast.visit()

    print("s2")
}