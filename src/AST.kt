var currentScope: ScopedSymbolTable? = null

interface AST {
    fun visit()
}

/** Statement functions **/

abstract class Statement(
    open val name: String,
    open val type: DataType
) : AST

class VarStat(
    override val name: String,
    override val type: DataType
) : Statement(name, type) {
    override fun visit() {
        if (currentScope?.lookup(name, true) != null) {
            throw VariableAlreadyDeclaredException(varName = name)
        }

        val typeSymbol = currentScope?.lookup(type.toString(), true) ?:
            throw UnexpectedTypeException(type.toString())

        val symbol = VarSymbol(name, typeSymbol)
        symbol.category = "var"

        currentScope?.insert(symbol)
    }
}

class ConstStat(
    override val name: String,
    val ast: AST
) : Statement(name, CreateDataType("")) {
    override fun visit() {
        if (currentScope?.lookup(name) != null) {
            throw ConstantAlreadyDeclaredException(name)
        }

        val typeName = ast::class.java.simpleName.toString().toLowerCase()
        val typeSymbol = currentScope?.lookup(typeName) ?: throw UnexpectedTypeException(type.toString())

        val symbol = ConstSymbol(name, typeSymbol)
        symbol.category = "const"

        currentScope?.insert(symbol)
    }
}

class FunctionStat(
    override val name: String,
    override val type: DataType,
    val parameters: MutableList<Field>,
    val bloc: Bloc
) : Statement(name, type) {
    override fun visit() {
        // Verifica se a função já foi declarada
        if (currentScope?.lookup(name) != null) {
            throw FunctionAlreadyDeclaredException(name)
        }

        // insere a declaração da função no escopo atual
        val functionSymbol = FunctionSymbol(name)

        // valida o tipo de retorno da função
        var typeName = type::class.java.simpleName.toString().toLowerCase()
        functionSymbol.type = currentScope!!.lookup(typeName) ?: throw UnexpectedTypeException(typeName)

        currentScope?.insert(functionSymbol)

        // cria um novo escopo
        val functionScope = ScopedSymbolTable(
            scopeName = name,
            scopeLevel = 2,
            enclosingScope = currentScope
        )
        currentScope = functionScope

        // adiciona os parametros no escopo da função
        parameters.forEach {
            typeName = it.type::class.java.simpleName.toString().toLowerCase()
            val typeSymbol = currentScope!!.lookup(typeName) ?: throw UnexpectedTypeException(typeName)

            val varSymbol = VarSymbol(it.name, typeSymbol)
            currentScope?.insert(varSymbol)
            functionSymbol.parameters.add(varSymbol)
        }

        bloc.visit()
        currentScope = currentScope?.enclosingScope
    }
}

class TypeStat(
    override val name: String,
    override val type: DataType,
) : Statement(name, type) {
    override fun visit() {
        if (currentScope?.lookup(name) != null) {
            throw TypeAlreadyDeclaredException(name)
        }

        val symbol = BuiltInType(name)
        symbol.category = "type"

        currentScope?.insert(symbol)
    }
}

/** Binary Operators **/

abstract class BinOp(
    open val left: AST,
    open val right: AST,
    open val operator: String
) : AST {
    override fun visit() {
        left.visit()
        right.visit()
    }
}

// Math Operators

abstract class MathOp(
    override val left: AST,
    override val right: AST,
    override val operator: String
) : BinOp(left, right, operator)

class Plus(
    override val left: AST,
    override val right: AST
) : MathOp(left, right, "+")

class Minus(
    override val left: AST,
    override val right: AST
) : MathOp(left, right, "-")

class Mul(
    override val left: AST,
    override val right: AST
) : MathOp(left, right, "*")

class Div(
    override val left: AST,
    override val right: AST
) : MathOp(left, right, "/")

// Logical Operators

abstract class LogicalOp(
    override val left: AST,
    override val right: AST,
    override val operator: String
) : BinOp(left, right, operator)

class LessThan(
    override val left: AST,
    override val right: AST
) : LogicalOp(left, right, "<")

class MoreThan(
    override val left: AST,
    override val right: AST
) : LogicalOp(left, right, ">")

class Exclamation(
    override val left: AST,
    override val right: AST
) : LogicalOp(left, right, "!")

class Equal(
    override val left: AST,
    override val right: AST
) : LogicalOp(left, right, "=")

// Assignment Operator

class AssignmentOp(
    override val left: AST,
    override val right: AST
) : BinOp(left, right, ":=")

/** Command Blocs **/

class Program(
    private val statements: MutableList<Statement>,
    private val bloc: Bloc
) : AST {
    override fun visit() {

        currentScope = ScopedSymbolTable(
            scopeName = "global",
            scopeLevel = 1,
            enclosingScope = currentScope
        )

        statements.forEach { it.visit() }
        bloc.visit()

        currentScope = currentScope?.enclosingScope
    }
}

class Bloc(
    private val commands: MutableList<AST>
) : AST {
    override fun visit() {
        commands.forEach { it.visit() }
    }
}

class While(
    val condition: AST,
    val bloc: Bloc
) : AST {
    override fun visit() {
        condition.visit()
        bloc.visit()
    }
}

class If(
    val condition: AST,
    val bloc: Bloc,
    val elseBloc: Bloc?
) : AST {
    override fun visit() {
        condition.visit()
        bloc.visit()
        elseBloc?.visit()
    }
}

class Write(
    val value: AST
) : AST {
    override fun visit() {

    }
}

class Read(
    val value: Usage
) : AST {
    override fun visit() {

    }
}

/** Data Types **/

interface DataType : AST

class CreateDataType(
    override val name: String
) : DataType, Usage(name) {
    override fun visit() {
        currentScope?.insert(BuiltInType(name))
    }

    override fun toString(): String = name
}

class Text(
    override val name: String
) : DataType, Usage(name) {
    override fun visit() {
    }
}

class Integer(
    val value: String
) : DataType, Usage(value) {
    override fun visit() {
    }

    override fun toString(): String = "integer"
}

class Real(
    val value: String
) : DataType, Usage(value) {
    override fun visit() {

    }

    override fun toString(): String = "real"
}

class Array(
    override val name: String,
    val size: String,
    val type: DataType
) : DataType, Usage(name) {
    override fun visit() {

    }

    override fun toString(): String = "array"
}

class Field(
    override val name: String,
    val type: DataType
) : DataType, Usage(name) {
    override fun visit() {

    }

    override fun toString(): String = type.toString()
}

class Record(
    override val name: String,
    val fields: MutableList<Field>
) : DataType, Usage(name) {
    override fun visit() {

    }

    override fun toString(): String = "record"
}

class EmptyDataType : DataType, Usage("") {
    override fun visit() {

    }
}

/** Usage Classes **/

abstract class Usage(
    open val name: String
) : AST

class RecordUsage(
    override val name: String,
    val child: Usage
) : Usage(name) {
    override fun visit() {

    }
}

class ArrayUsage(
    override val name: String,
    val child: Usage
) : Usage(name) {
    override fun visit() {

    }
}

class VarUsage(
    override val name: String
) : Usage(name) {
    override fun visit() {

    }
}

class ParameterUsage(
    override val name: String
) : Usage(name) {
    override fun visit() {

    }
}

class FunctionUsage(
    override val name: String,
    val parameters: MutableList<ParameterUsage>
) : Usage(name) {
    override fun visit() {

    }
}
