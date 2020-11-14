package usonia.kotlin

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors

actual fun Any.getResourceContents(path: String): String = javaClass.classLoader
    .getResourceAsStream(path).use { inputStream: InputStream ->
        InputStreamReader(inputStream).use { isr ->
            BufferedReader(isr).use { reader ->
                reader.lines().collect(Collectors.joining(System.lineSeparator()))
            }
        }
    }
