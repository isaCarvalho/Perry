class VariableAlreadyDeclaredException(private val varName: String) :
    Exception(
        "The variable $varName is already declared in this scope (${currentScope?.scopeName}).\n"
    )

class TypeAlreadyDeclaredException(private val typeName: String) :
    Exception(
        "The type $typeName is already declared in this scope (${currentScope?.scopeName}).\n"
    )

class ConstantAlreadyDeclaredException(private val constName: String) :
    Exception(
        "The constant $constName is already declared in this scope (${currentScope?.scopeName})\n"
    )

class FunctionAlreadyDeclaredException(private val funName: String) :
    Exception(
        "The function $funName is already declared in this scope (${currentScope?.scopeName})\n"
    )

class UnexpectedVariableException(private val varName: String) :
    Exception(
        "The variable $varName was not declared in the scope (${currentScope?.scopeName})."
    )

class UnexpectedTypeException(private val typeName: String) :
    Exception(
        "The data type $typeName was not declared in the scope (${currentScope?.scopeName})."
    )