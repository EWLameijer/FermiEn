
import data.utils.getDateString
import data.utils.pathPart
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

val doNothing = Unit

const val EMPTY_STRING = ""

const val DEFAULT_SEPARATOR = ": "

fun Any.genericEqualsWith(other: Any?, specialistCompare: (Any) -> Boolean): Boolean = when {
    this === other -> true
    other == null -> false
    javaClass != other.javaClass -> false
    else -> specialistCompare(other)
}

fun backup(filename: String) {
    val (namePart, extension) = splitFilenameAtExtension(filename)
    val backupName = "${namePart}_${getDateString()}.$extension"
    Files.copy(Path(filename), Path(backupName), StandardCopyOption.REPLACE_EXISTING)
    val searchDirectory = filename.pathPart()
    val backupFiles =
        File(searchDirectory).walk().filter { it.isDatePostFixed(filename) }.map { it.absolutePath }.toList()
            .sortedDescending()
    if (backupFiles.size > 3) {
        for (index in 0..2) println("Keeping '${backupFiles[index]}'.")
        for (index in 3..backupFiles.lastIndex) {
            println("Marking '${backupFiles[index]}' for removal.")
            File(backupFiles[index]).delete()
        }
    }
}

private fun File.isDatePostFixed(filename: String): Boolean {
    val (myName, myExtension) = splitFilenameAtExtension(absolutePath)
    val (origName, origExtension) = splitFilenameAtExtension(filename)
    return (myExtension == origExtension && myName.startsWith(origName) && myName.removePrefix(origName).isDateSuffix())
}

private fun String.isDateSuffix(): Boolean = length == 12 && all { it.isDigit() || it == '_' }

fun splitFilenameAtExtension(filename: String): Pair<String, String> =
    filename.substringBeforeLast(".") to filename.substringAfterLast(".")