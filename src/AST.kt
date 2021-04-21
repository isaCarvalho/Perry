interface AST

class Empty : AST

/** Statement functions **/

abstract class Statement(
    open val name: String,
    open val type : DataType
) : AST

class VarStat(
    override val name: String,
    override val type: DataType
) : Statement(name, type)

class ConstStat(
    override val name: String,
    val ast: AST
) : Statement(name, CreateDataType("") )

class FunctionStat(
    override val name: String,
    override val type: DataType,
    val parameters: MutableList<Field>,
    val bloc : Bloc
) : Statement(name, type)

class TypeStat(
    override val name: String,
    override val type: DataType,
) : Statement(name, type)

/** Binary Operators **/

interface BinOp : AST

// Math Operators

abstract class MathOp(
    open val left: AST,
    open val right: AST,
    open val operator: String
) : BinOp

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
    open val left: AST,
    open val right: AST,
    open val operator: String
) : BinOp

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
    val left: AST,
    val right: AST
) : BinOp

/** Command Blocs **/

class Program(
    val statements : MutableList<Statement>,
    val bloc: Bloc
)

class Bloc(
    val commands : MutableList<AST>
) : AST

class While(
    val condition : AST,
    val bloc : Bloc
) : AST

class If(
    val condition : AST,
    val bloc : Bloc,
    val elseBloc: Bloc?
) : AST

class Write(
    val value: AST
) : AST

class Read(
    val value: Usage
) : AST

/** Data Types **/

interface DataType : AST

class CreateDataType(
    override val name: String
) : DataType, Usage(name)

class Text(
    override val name: String
) : DataType, Usage(name)

class Integer(
    val value: String
) : DataType, Usage(value)

class Real(
    val value: String
) : DataType, Usage(value)

class Array(
    override val name: String,
    val size: String,
    val type: DataType
) : DataType, Usage(name)

class Field(
    override val name: String,
    val type: DataType
) : DataType, Usage(name)

class Record(
    override val name: String,
    val fields : MutableList<Field>
) : DataType, Usage(name)

class EmptyDataType : DataType, Usage("")

/** Usage Classes **/

abstract class Usage(
    open val name: String
) : AST

class RecordUsage(
    override val name: String,
    val child: Usage
) : Usage(name)

class ArrayUsage(
    override val name: String,
    val child: Usage
) : Usage(name)

class VarUsage(
    override val name: String,
    val type: String
) : Usage(name)

class ParameterUsage(
    override val name: String
) : Usage(name)

class FunctionUsage(
    override val name: String,
    val parameters : MutableList<ParameterUsage>
) : Usage(name)
