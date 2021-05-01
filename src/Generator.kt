object Generator {
    private var countTemp = 0
    private var countLabel = 0


    fun generateTemp() : String {
        countTemp++
        return "t$countTemp"
    }

    fun generateLabel() : String {
        countLabel++
        return "Label$countLabel"
    }
}