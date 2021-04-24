open class Symbol(open val name: String) {
    open var type: Symbol? = null
    open var category: String = ""

    override fun toString(): String {
        return "name: $name\ttype: $type\tcategory: $category"
    }
}

class BuiltInType(override val name: String) : Symbol(name)

class ConstSymbol(override val name: String, override var type: Symbol?) : Symbol(name)
{
    init {
        super.type = type
    }
}

class VarSymbol(override val name: String, override var type: Symbol?) : Symbol(name)
{
    init {
        super.type = type
    }
}

object SymbolTable {
    private val symbolTable = LinkedHashMap<String, Symbol>()

    init {
        symbolTable["integer"] = BuiltInType("integer")
        symbolTable["real"] = BuiltInType("real")
        symbolTable["array"] = BuiltInType("array")
        symbolTable["record"] = BuiltInType("record")
    }

    fun insert(symbol: Symbol) {
        symbolTable[symbol.name] = symbol
    }

    fun lookup(name: String) = symbolTable[name]
}