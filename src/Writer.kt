import java.io.File
import java.lang.Exception

object Writer {

    var fileName: String = "";

    private val file: File by lazy {
        val file = File(fileName)

        if (file.exists()) {
            file.delete()
        }

        if (!file.createNewFile()) {
            throw Exception("Could not create out file \"$fileName\"")
        }

        file
    }


    fun write(code: String) {
        file.appendText(code + "\n")
    }
}