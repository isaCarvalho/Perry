//package analisators
//
//class Parser(private val lex : Lex) {
//    private val bufferTokens =  ArrayList<Token>()
//    private var isEnd = false
//
//    init {
//        readToken()
//    }
//
//    private fun readToken() {
//        if (bufferTokens.isNotEmpty()) {
//            bufferTokens.removeAt(0)
//        }
//
//        while (bufferTokens.size < BUFFER_SIZE && !isEnd) {
//            val next : Token = lex.getNextToken()
//            bufferTokens.add(next)
//
//            if (next.tokenType == TokenType.EOF) {
//                isEnd = true
//            }
//        }
//        println("Token ${lookahead(1)} read")
//    }
//
//    private fun lookahead(k: Int): Token? {
//        val quantityOfElementsIntoTheBuffer = bufferTokens.size - 1
//
//        return when {
//            bufferTokens.isEmpty() -> null
//
//            k - 1 >= quantityOfElementsIntoTheBuffer -> bufferTokens[quantityOfElementsIntoTheBuffer]
//
//            else -> bufferTokens[k - 1]
//        }
//    }
//}