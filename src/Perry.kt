fun main(args: kotlin.Array<String>) {
    Writer.fileName = args[1]

    val lex = Lex(args[0])
    val parser = Parser(lex)
    val ast = parser.program()

    ast.visit()
    ast.gen()
}