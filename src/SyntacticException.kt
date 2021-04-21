import java.lang.Exception

class SyntacticException(private val token: Token?, vararg types: Any) :
    Exception("Syntactic error: expected one of the following " +
            "(${types.joinToString(", ")}), " +
            "but found ${token ?: "-"}")