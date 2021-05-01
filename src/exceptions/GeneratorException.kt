package exceptions

class GeneratorException(name: String) : Exception("Could not generate code. Command: $name")