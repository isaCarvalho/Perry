class VariableAlreadyDeclaredException(varName: String) :
    Exception(
        "The variable $varName is already declared in this scope (${currentScope?.scopeName}).\n"
    )

class TypeAlreadyDeclaredException(typeName: String) :
    Exception(
        "The type $typeName is already declared in this scope (${currentScope?.scopeName}).\n"
    )

class ConstantAlreadyDeclaredException(constName: String) :
    Exception(
        "The constant $constName is already declared in this scope (${currentScope?.scopeName}).\n"
    )

class FunctionAlreadyDeclaredException(funName: String) :
    Exception(
        "The function $funName is already declared in this scope (${currentScope?.scopeName}).\n"
    )

class UnexpectedVariableException(varName: String) :
    Exception(
        "The variable $varName was not declared in the scope (${currentScope?.scopeName})."
    )

class UnexpectedTypeException(typeName: String) :
    Exception(
        "The data type $typeName was not declared in the scope (${currentScope?.scopeName})."
    )

class FunctionNotDeclaredException(funName: String) :
    Exception(
        "The function $funName was not declared.\n"
    )

class DeclaredNameIsNotAFunctionException(funName: String) :
    Exception(
        "The name $funName was not declared as a function.\n"
    )

class UnexpectedParameterSizeException(funName: String, expected : Int, found : Int) :
    Exception(
        "The function $funName was excpeting $expected parameter(s), but found $found.\n"
    )

class TypeMismatchException(varName: String, expected: String, found: String) :
    Exception(
        "$varName is $expected and cannot be assigned with $found value."
    )