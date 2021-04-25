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

        val typeSymbol = currentScope?.lookup(type.toString(), true) ?: throw UnexpectedTypeException(type.toString())

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

        val typeName = ast.getClassName()
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
        var typeName = if (type is CreateDataType)
            type.name
        else
            type.getClassName()

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
            typeName = if (it.dataType is CreateDataType)
                it.dataType.name
            else
                it.dataType.getClassName()

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

class ArrayStat(
    override val name: String,
    override val type: DataType,
    private val size: Int
) : Statement(name, type) {
    override fun visit() {
        val typeSymbol = currentScope?.lookup(type.toString())
        currentScope?.insert(ArraySymbol(name, typeSymbol, size))
    }
}

class RecordStat(
    override val name: String,
    override val type: DataType,
    val fields: MutableList<Field>
) : Statement(name, type) {
    override fun visit() {
        var typeSymbol = currentScope?.lookup(type.toString())
        val recordSymbol = RecordSymbol(name, typeSymbol)

        currentScope?.insert(recordSymbol)

        // adiciona os parametros no escopo da função
        fields.forEach {
            val typeName = if (it.dataType is CreateDataType)
                it.dataType.name
            else
                it.dataType.getClassName()

            typeSymbol = currentScope!!.lookup(typeName) ?: throw UnexpectedTypeException(typeName)

            val varSymbol = VarSymbol(it.name, typeSymbol)
            currentScope?.insert(varSymbol)
            recordSymbol.fields.add(varSymbol)
        }
    }
}

/** Binary Operators **/

abstract class BinOp(
    open val left: Usage,
    open val right: Usage,
    operator: String
) : Usage(operator), AST {
    override var type: String? = null

    override fun visit() {
        left.visit()
        right.visit()

        if (left.type != right.type) {
            throw TypeMismatchException(left.name, left.type ?: "-", right.type ?: "-")
        } else {
            type = left.type
        }
    }
}

// Math Operators

abstract class MathOp(
    override val left: Usage,
    override val right: Usage,
    operator: String
) : BinOp(left, right, operator)

class Plus(
    override val left: Usage,
    override val right: Usage
) : MathOp(left, right, "+")

class Minus(
    override val left: Usage,
    override val right: Usage
) : MathOp(left, right, "-")

class Mul(
    override val left: Usage,
    override val right: Usage
) : MathOp(left, right, "*")

class Div(
    override val left: Usage,
    override val right: Usage
) : MathOp(left, right, "/")

// Logical Operators

abstract class LogicalOp(
    override val left: Usage,
    override val right: Usage,
    operator: String
) : BinOp(left, right, operator)

class LessThan(
    override val left: Usage,
    override val right: Usage
) : LogicalOp(left, right, "<")

class MoreThan(
    override val left: Usage,
    override val right: Usage
) : LogicalOp(left, right, ">")

class Exclamation(
    override val left: Usage,
    override val right: Usage
) : LogicalOp(left, right, "!")

class Equal(
    override val left: Usage,
    override val right: Usage
) : LogicalOp(left, right, "=")

// Assignment Operator

class AssignmentOp(
    override val left: Usage,
    override val right: Usage
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

// extention function to get the class' name
fun AST.getClassName() = this::class.java.simpleName.toString().toLowerCase()

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
        type = "text"
    }
}

class Integer(
    val value: String
) : DataType, Usage(value) {

    override fun visit() {
        type = "integer"
    }

    override fun toString(): String = "integer"
}

class Real(
    val value: String
) : DataType, Usage(value) {

    override fun visit() {
        type = "real"
    }

    override fun toString(): String = "real"
}

class Array(
    override val name: String,
    val size: String,
    val dataType: DataType
) : DataType, Usage(name) {

    override var type: String? = null

    init {
        type = if (dataType is CreateDataType) {
            currentScope?.lookup(dataType.name)?.name
        } else {
            dataType.getClassName()
        }
    }

    override fun visit() {

    }

    override fun toString(): String = "array"
}

class Field(
    override val name: String,
    val dataType: DataType
) : DataType, Usage(name) {
    override var type: String? = null

    init {
        type = if (dataType is CreateDataType) {
            currentScope?.lookup(dataType.name)?.name
        } else {
            dataType.getClassName()
        }
    }

    override fun visit() {

    }

    override fun toString(): String = dataType.toString()
}

class Record(
    override val name: String,
    val fields: MutableList<Field>
) : DataType, Usage(name) {
    override fun visit() {

    }

    override fun toString(): String = "record"
}

/** Usage Classes **/

abstract class Usage(
    open val name: String,
) : AST {
    open var type: String? = null
}

class RecordUsage(
    override val name: String,
    val child: Usage
) : Usage(name) {
    override fun visit() {
        child.visit()

        val varSymbol = currentScope?.lookup(name)
        val recordSymbol = currentScope?.lookup(varSymbol?.type?.name ?: "") ?: throw UnexpectedRecordException(name)

        if (recordSymbol !is RecordSymbol) {
            throw NameIsNotARecordException(name)
        }

        recordSymbol.fields.forEach {
            if (it.name == child.name) {
                type = it.type?.name
            }
        }
    }
}

class ArrayUsage(
    override val name: String,
    val child: Usage
) : Usage(name) {
    override fun visit() {
        child.visit()

        val varSymbol = currentScope?.lookup(name) ?: throw UnexpectedArrayException(name)

        var typeSymbol = varSymbol.type

        var arraySymbol: ArraySymbol? = null

        if (typeSymbol is ArraySymbol) {
            arraySymbol = typeSymbol
        }

        while (typeSymbol !is BuiltInType) {
            if (typeSymbol is ArraySymbol) {
                arraySymbol = typeSymbol
            }

            typeSymbol = currentScope?.lookup(typeSymbol!!.name)?.type
        }

        if (arraySymbol == null) {
            throw NameIsNotAnArrayException(name)
        }

        type = typeSymbol.name

        val sizeUsage = child.name.toIntOrNull()
        if (sizeUsage != null) {
            if (arraySymbol != null && varSymbol != null) {
                if (sizeUsage < 0 || sizeUsage >= arraySymbol.size) {
                    throw IndexOutOfBoundsException(varSymbol.name, arraySymbol.size, sizeUsage)
                }
            }
        }
    }
}

class VarUsage(
    override val name: String
) : Usage(name) {

    override fun visit() {
        val varSymbol = currentScope?.lookup(name) ?: throw UnexpectedVariableException(name)

        type = varSymbol.type?.name
    }
}

class FunctionUsage(
    override val name: String,
    val parameters: MutableList<Usage>
) : Usage(name) {

    override fun visit() {
        val functionSymbol = currentScope?.lookup(name, false) ?: throw FunctionNotDeclaredException(name)

        if (functionSymbol !is FunctionSymbol) {
            throw DeclaredNameIsNotAFunctionException(name)
        }

        val expected = functionSymbol.parameters.size
        val found = parameters.size
        if (expected != found) {
            throw UnexpectedParameterSizeException(name, expected, found)
        }

        type = functionSymbol.type?.name
        parameters.forEach { it.visit() }
    }
}
