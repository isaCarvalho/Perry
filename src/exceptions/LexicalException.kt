package exceptions

import Token

class UnexcpetedTokenException(private val token: Token) : Exception(
    "Unexcpeted token $token"
)

class TokenNotFoundException() : Exception(
    "Unable to assert token"
)