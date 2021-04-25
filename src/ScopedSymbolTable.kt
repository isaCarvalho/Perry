open class Symbol(open val name: String) {
    open var type: Symbol? = null
    open var category: String = ""

    override fun toString(): String {
        return "name: $name\ttype: $type\tcategory: $category"
    }
}

class BuiltInType(override val name: String) : Symbol(name)

class CreatedType(override val name: String) : Symbol(name)

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

class ArraySymbol(override val name: String, override var type: Symbol?, val size : Int) : Symbol(name)
{
    init {
        super.type = type
    }
}

class RecordSymbol(override val name: String, override var type: Symbol?) : Symbol(name)
{
    val fields: MutableList<Symbol> = mutableListOf()

    init {
        super.type = type
    }
}

class FunctionSymbol(
    override val name: String
) : Symbol(name) {
    val parameters : MutableList<Symbol> = mutableListOf()
}

class ScopedSymbolTable(
    val scopeName : String,
    val scopeLevel : Int,
    val enclosingScope : ScopedSymbolTable? = null
) {
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

    fun lookup(name: String, currentScopeOnly : Boolean = false) : Symbol? {
        val symbol = symbolTable[name]
        if (symbol != null)
            return symbol

        if (currentScopeOnly) {
            return null
        }

        return enclosingScope?.lookup(name)
    }
}