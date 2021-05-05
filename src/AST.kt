import exceptions.GeneratorException

var currentScope: ScopedSymbolTable? = null

interface AST {
    fun visit() {}

    open fun gen() {}

    fun getClassName() = this::class.java.simpleName.toString().toLowerCase()
}

abstract class Statement(
    open val name: String,
    open val type: Command
) : AST

abstract class Command(
    open val name: String,
) : AST {
    open var type: String? = null

    open fun leftValue(): Command {
        throw GeneratorException(name)
    }

    open fun rightValue(): Command {
        throw GeneratorException(name)
    }
}

class VarStat(
    override val name: String,
    override val type: Command
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
    override val type: Command,
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

    override fun gen() {
        Writer.write("$name:")
        bloc.gen()
    }
}

class TypeStat(
    override val name: String,
    override val type: Command,
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
    override val type: Command,
    private val size: Int
) : Statement(name, type) {
    override fun visit() {
        val typeSymbol = currentScope?.lookup(type.toString())
        currentScope?.insert(ArraySymbol(name, typeSymbol, size))
    }
}

class RecordStat(
    override val name: String,
    override val type: Command,
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

class BinOp(
    val left: Command,
    val right: Command,
    val operator: String
) : Command(operator), AST {
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


    override fun gen() {
        when (operator) {
            "=" -> {
                val c1 = left.leftValue()
                val c2 = right.rightValue()
                Writer.write("$c1 = $c2")
            }
        }
    }

    override fun rightValue(): Command {
        val temp = Generator.generateTemp()

        val c1 = left.rightValue()
        val c2 = right.rightValue()

        Writer.write("$temp = $c1 $operator $c2")

        return Identifier(temp)
    }
}

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

    override fun gen() {
        Writer.write("GOTO main")
        statements.forEach { it.gen() }
        Writer.write("main:")
        bloc.gen()
    }
}

class Bloc(
    private val commands: MutableList<Command>,
    private val statements: MutableList<Statement>? = null
) : AST {
    override fun visit() {
        statements?.forEach { it.visit() }
        commands.forEach { it.visit() }
    }

    override fun gen() {
        statements?.forEach { it.gen() }
        commands.forEach { it.gen() }
    }
}

class While(
    val condition: Command,
    val bloc: Bloc
) : Command("while") {
    override fun visit() {
        condition.visit()
        bloc.visit()
    }

    override fun gen() {
        val label1 = Generator.generateLabel()
        val label2 = Generator.generateLabel()

        var exp = condition.rightValue()
        Writer.write("IFFALSE $exp GOTO $label2")
        Writer.write("$label1:")

        bloc.gen()

        exp = condition.rightValue()
        Writer.write("IFTRUE $exp GOTO $label1")

        Writer.write("$label2:")
    }
}

class If(
    val condition: Command,
    val bloc: Bloc,
    val elseBloc: Bloc?
) : Command("if") {
    override fun visit() {
        condition.visit()
        bloc.visit()
        elseBloc?.visit()
    }

    override fun gen() {
        var label = Generator.generateLabel()
        val exp = condition.rightValue()

        Writer.write("IFFALSE $exp GOTO $label")
        bloc.gen()
        Writer.write("$label:")

        if (elseBloc != null) {
            label = Generator.generateLabel()

            Writer.write("IFTRUE $exp GOTO $label")
            elseBloc.gen()
            Writer.write("$label:")
        }

    }
}

class Write(private val value: Command) : Command("write") {

    override fun gen() {
        val c1 = value.rightValue()
        Writer.write("WRITE $c1")
    }
}

class Read(private val value: Command) : Command("read") {

    override fun gen() {
        val c1 = value.rightValue()
        Writer.write("READ $c1")
    }
}

// extention function to get the class' name

class CreateDataType(
    override val name: String
) : Command(name) {
    override fun visit() {
        currentScope?.insert(BuiltInType(name))
    }

    override fun toString(): String = name
}

class Text(
    override val name: String
) : Command(name) {

    override fun visit() {
        type = "text"
    }

    override fun rightValue(): Command = this

    override fun toString(): String = name
}

class Integer(val value: String) : Command(value) {

    override fun visit() {
        type = "integer"
    }

    override fun rightValue(): Command = this

    override fun toString(): String = value
}

class Real(
    val value: String
) : Command(value) {

    override fun visit() {
        type = "real"
    }

    override fun rightValue(): Command = this

    override fun toString(): String = value
}

class Array(
    override val name: String,
    val size: String,
    val dataType: Command
) : Command(name) {

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
    val dataType: Command
) : Command(name) {
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
) : Command(name) {


    override fun toString(): String = "record"


}

class RecordCommand(
    override val name: String,
    val child: Command
) : Command(name) {
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

    override fun leftValue(): Command = this

    override fun rightValue(): Command = this

    override fun toString(): String = "$name.$child"
}

class ArrayCommand(
    override val name: String,
    val child: Command
) : Command(name) {
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
            if (sizeUsage < 0 || sizeUsage >= arraySymbol.size) {
                throw IndexOutOfBoundsException(varSymbol.name, arraySymbol.size, sizeUsage)
            }
        }
    }

    override fun leftValue(): Command = ArrayCommand(name, child.rightValue())

    override fun rightValue(): Command {
        val temp = Generator.generateTemp()
        val c1 = leftValue()

        Writer.write("$temp = $c1")

        return Identifier(temp)
    }

    override fun toString(): String = "$name[$child]"
}

class Identifier(
    override val name: String
) : Command(name) {

    override fun visit() {
        val varSymbol = currentScope?.lookup(name)
            ?: throw UnexpectedVariableException(name)

        type = varSymbol.type?.name
    }

    override fun leftValue(): Command = this

    override fun rightValue(): Command = this

    override fun toString(): String = name
}

class FunctionCommand(
    override val name: String,
    val parameters: MutableList<Command>
) : Command(name) {

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

    override fun rightValue(): Command {
        parameters.forEach {
            val param = it.rightValue()
            Writer.write("param $param")
        }

        val temp = Generator.generateTemp()
        Writer.write("$temp = call $name ${parameters.size}")

        return Identifier(temp)
    }
}
