package com.nutridiario.app

data class ParsedQuery(val query: String, val grams: Double?, val units: Double?)

/**
 * Interpreta frases como "150 g de pollo", "2 huevos", "un vaso de leche", "medio kilo de fresas".
 */
object QuantityParser {
    private val numberWords = mapOf(
        "un" to 1.0, "una" to 1.0, "uno" to 1.0, "dos" to 2.0, "tres" to 3.0, "cuatro" to 4.0,
        "cinco" to 5.0, "seis" to 6.0, "siete" to 7.0, "ocho" to 8.0, "nueve" to 9.0, "diez" to 10.0,
        "medio" to 0.5, "media" to 0.5, "cuarto" to 0.25,
    )
    private val containers = mapOf(
        "vaso" to 250.0, "vasos" to 250.0, "taza" to 250.0, "tazas" to 250.0, "tazon" to 300.0,
        "plato" to 300.0, "platos" to 300.0, "cuenco" to 250.0, "bol" to 250.0,
        "cucharada" to 15.0, "cucharadas" to 15.0, "cucharadita" to 5.0, "cucharaditas" to 5.0,
        "rebanada" to 30.0, "rebanadas" to 30.0, "loncha" to 20.0, "lonchas" to 20.0,
        "puñado" to 30.0, "punado" to 30.0, "lata" to 330.0, "latas" to 330.0, "copa" to 150.0,
        "copas" to 150.0, "trozo" to 60.0, "trozos" to 60.0, "pieza" to 100.0, "piezas" to 100.0,
        "tercio" to 330.0, "botellin" to 200.0, "caña" to 200.0,
    )
    private val unitRegex = Regex(
        "(\\d+(?:[.,]\\d+)?|medio|media|un|una|dos|tres|cuarto)\\s*(kg|kilos?|kilogramos?|g|gr|grs|gramos?|ml|mililitros?|cl|centilitros?|l|litros?)\\b"
    )

    fun parse(input: String): ParsedQuery {
        var text = input.lowercase().trim()
        var grams: Double? = null
        var units: Double? = null

        unitRegex.find(text)?.let { m ->
            val num = m.groupValues[1].let { numberWords[it] ?: it.replace(',', '.').toDoubleOrNull() } ?: 1.0
            val unit = m.groupValues[2]
            grams = when {
                unit.startsWith("k") -> num * 1000
                unit.startsWith("cl") || unit.startsWith("centi") -> num * 10
                unit == "l" || unit.startsWith("litro") -> num * 1000
                else -> num
            }
            text = text.removeRange(m.range)
        }

        if (grams == null) {
            val words = text.split(Regex("\\s+")).toMutableList()
            val first = words.firstOrNull()
            val n = first?.let { numberWords[it] ?: it.replace(',', '.').toDoubleOrNull() }
            if (n != null) {
                words.removeAt(0)
                val c = words.firstOrNull()?.let { containers[it] }
                if (c != null) {
                    grams = n * c
                    words.removeAt(0)
                } else units = n
                text = words.joinToString(" ")
            } else {
                val c = first?.let { containers[it] }
                if (c != null) { grams = c; words.removeAt(0); text = words.joinToString(" ") }
            }
        }
        text = text.trim().replace(Regex("\\s+"), " ").replace(Regex("^(de|del|con)\\s+"), "").replace(Regex("\\s+(de|del)$"), "").trim()
        return ParsedQuery(text, grams, units)
    }
}
