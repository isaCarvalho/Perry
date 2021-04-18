class Token(val tokenType: TokenType, val lexeme : String)
{
    override fun toString(): String {
        return "<$tokenType, $lexeme>"
    }
}