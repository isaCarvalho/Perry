class Lex(fileName: String) {
    private var fileReader = Buffer(fileName)

    fun getNextToken(): Token {
        spaces()

        comments()

        spaces()
        fileReader.confirm()

        var nextToken: Token? = endOfFile()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = keyWord()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = number()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = mathOperator()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = relationalOperator()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = points()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = parenthesis()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = brackets()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = text()
        if (check(nextToken)) {
            return nextToken!!
        }

        nextToken = identifier()
        if (check(nextToken)) {
            return nextToken!!
        }

        throw Exception("Lexical error")
    }

    private fun text(): Token? {
        if (!match('"')) {
            return null
        }

        while (match('"')) {
            getChar()
        }

        return Token(TokenType.Text, fileReader.lexeme)
    }

    private fun mathOperator(): Token? {
        return when (getChar()) {
            '*' -> Token(TokenType.Times, "*")

            '+' -> Token(TokenType.Plus, "+")

            '-' -> Token(TokenType.Minus, "-")

            '/' -> Token(TokenType.Divider, "/")

            else -> null
        }
    }

    private fun parenthesis(): Token? {
        return when (getChar()) {
            '(' -> Token(TokenType.LeftParenthesis, "(")

            ')' -> Token(TokenType.RightParenthesis, ")")

            else -> null
        }
    }

    private fun relationalOperator(): Token? {
        return when (getChar()) {
            '<' -> Token(TokenType.LessThan, "<")

            '>' -> Token(TokenType.MoreThan, ">")

            '=' -> Token(TokenType.Equal, "=")

            else -> null
        }
    }

    private fun brackets(): Token? {
        return when (getChar()) {
            '[' -> Token(TokenType.LeftBracket, "[")

            ']' -> Token(TokenType.RightBracket, "]")

            else -> null
        }
    }

    private fun points(): Token? {
        return when (getChar()) {
            '!' -> Token(TokenType.Exclamation, "!")

            ';' -> Token(TokenType.Semicolon, ";")

            ',' -> Token(TokenType.Comma, ",")

            '.' -> Token(TokenType.Dot, ".")

            ':' -> {
                if (match('=')) {
                    return Token(TokenType.Assignment, ":=")
                }

                fileReader.decreasePointer()
                Token(TokenType.Colon, ":")
            }

            else -> null
        }
    }

    private fun keyWord(): Token? {
        while (true) {
            if (!getChar().isLetter()) {
                fileReader.decreasePointer()

                return when (fileReader.lexeme) {
                    "const" -> Token(TokenType.Const, "const")

                    "array" -> Token(TokenType.Array, "array")

                    "type" -> Token(TokenType.Type, "type")

                    "of" -> Token(TokenType.Of, "of")

                    "begin" -> Token(TokenType.Begin, "begin")

                    "end" -> Token(TokenType.End, "end")

                    "function" -> Token(TokenType.Function, "function")

                    "var" -> Token(TokenType.Var, "var")

                    "while" -> Token(TokenType.While, "while")

                    "read" -> Token(TokenType.Read, "read")

                    "write" -> Token(TokenType.Write, "write")

                    "integer" -> Token(TokenType.Integer, "integer")

                    "real" -> Token(TokenType.Real, "real")

                    "record" -> Token(TokenType.Record, "record")

                    "then" -> Token(TokenType.Then, "then")

                    "if" -> Token(TokenType.If, "if")

                    "else" -> Token(TokenType.Else, "else")

                    else -> null
                }
            }
        }
    }

    private fun number(): Token? {
        var state = 1

        while (true) {
            var c = getChar()

            if (state == 1) {
                if (c.isDigit()) {
                    state = 2
                } else {
                    return null
                }
            } else if (state == 2) {
                if (c == '.') {
                    c = getChar()

                    if (c.isDigit()) {
                        state = 3
                    } else {
                        return null
                    }
                } else if (!c.isDigit()) {
                    fileReader.decreasePointer()
                    return Token(TokenType.Integer, fileReader.lexeme)
                }
            } else if (state == 3) {
                if (!c.isDigit()) {
                    fileReader.decreasePointer()
                    return Token(TokenType.Real, fileReader.lexeme)
                }
            }
        }
    }

    private fun identifier(): Token? {
        var state = 1

        while (true) {
            val c = getChar()

            if (state == 1) {
                if (c.isLetter()) {
                    state = 2
                } else {
                    return null
                }
            } else if (state == 2) {
                if (!c.isLetterOrDigit()) {
                    fileReader.decreasePointer()
                    return Token(TokenType.Identifier, fileReader.lexeme)
                }
            }
        }
    }

    private fun endOfFile(): Token? {
        return if (fileReader.getNextInt() == -1)
            Token(TokenType.EOF, "-1")
        else
            null
    }

    private fun comments() {
        if (match('{')) {
            while (!match('}')) {
                getChar()
            }
        } else {
            fileReader.decreasePointer()
        }
    }

    private fun spaces() {
        var c = getChar()

        while (c.isWhitespace()) {
            c = getChar()
        }

        fileReader.decreasePointer()
    }

    private fun getChar() = fileReader.getNextInt().toChar()

    private fun check(token: Token?): Boolean {
        return if (token == null) {
            fileReader.clear()
            false
        } else {
            fileReader.confirm()
            true
        }
    }

    private fun match(char: Char): Boolean {
        return getChar() == char
    }
}