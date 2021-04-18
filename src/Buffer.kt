import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class Buffer(fileName: String) {
    private lateinit var inputStream: InputStream

    private val readBuffer = arrayOfNulls<Int>(READ_BUFFER_SIZE * 2)

    private var pointer: Int = 0

    private var currentBuffer: Int = 2

    private var beginOfLexeme: Int = 0

    var lexeme: String = ""

    companion object {
        const val READ_BUFFER_SIZE = 20
    }

    init {
        try {
            inputStream = FileInputStream(File(fileName))
            updateBuffer1()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun clear() {
        pointer = beginOfLexeme
        lexeme = ""
    }

    fun confirm() {
        beginOfLexeme = pointer
        lexeme = ""
    }

    fun getNextInt(): Int {
        val ret: Int? = readBuffer[pointer]
        increasePointer()

        lexeme += ret!!.toChar()

        return ret
    }

    fun decreasePointer() {
        pointer--
        lexeme = lexeme.substring(0, lexeme.length - 1)
        if (pointer < 0) {
            pointer = READ_BUFFER_SIZE * 2 - 1
        }
    }

    private fun increasePointer() {
        pointer++
        if (pointer == READ_BUFFER_SIZE) {
            updateBuffer2()
        } else if (pointer == READ_BUFFER_SIZE * 2) {
            updateBuffer1()
            pointer = 0
        }
    }

    private fun updateBuffer1() {
        if (currentBuffer == 2) {
            currentBuffer = 1

            try {
                for (i: Int in 0 until READ_BUFFER_SIZE) {
                    readBuffer[i] = inputStream.read()
                    if (readBuffer[i] == -1) {
                        break
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun updateBuffer2() {
        if (currentBuffer == 1) {
            currentBuffer = 2

            try {
                for (i: Int in READ_BUFFER_SIZE until READ_BUFFER_SIZE * 2) {
                    readBuffer[i] = inputStream.read()
                    if (readBuffer[i] == -1) {
                        break
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}