enum class UpdateType {
    ENTRY_CHANGED, // card edited
    ENCY_CHANGED, // card added/removed
    ENCY_SWAPPED, // other deck loaded
    INPUTFIELD_CHANGED, // input field in one of the options windows changed contents
    PROGRAMSTATE_CHANGED // changing program state (from reviewing to summarizing, for example)
}

/**
 * Produces an update; the exact value of the update is given with the String
 * "contents". Note that contents can (so far) only be provided for program
 * state (Main window state) updates.
 */
class Update (val type: UpdateType, val contents: String = ""){
    init {
        require(type != UpdateType.PROGRAMSTATE_CHANGED || contents.isNotEmpty()) {
            "Update constructor error: must give second parameter when the program state changes."}
    }
}

