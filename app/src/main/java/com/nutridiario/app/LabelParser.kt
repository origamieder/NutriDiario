package com.nutridiario.app

import kotlin.math.max
import kotlin.math.min

/** Una línea de texto reconocida en la foto, con su posición (píxeles). */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val cy get() = (top + bottom) / 2
    val height get() = bottom - top
}

/** Resultado de leer una etiqueta nutricional. Los valores son por 100 g/ml salvo que se indique. */
data class LabelData(
    val kcal: Double? = null,
    val fat: Double? = null,
    val satFat: Double? = null,
    val carbs: Double? = null,
    val sugar: Double? = null,
    val fiber: Double? = null,
    val protein: Double? = null,
    val salt: Double? = null,
    val packageAmount: Double? = null,
    val servingAmount: Double? = null,
    val unit: String = "g",
) {
    val nutrientCount get() = listOf(kcal, fat, satFat, carbs, sugar, fiber, protein, salt).count { it != null }
}

/**
 * Interpreta el texto de la tabla nutricional (formato europeo/español: "Valor energético",
 * "Grasas", "de las cuales saturadas", "Hidratos de carbono", "de los cuales azúcares",
 * "Fibra alimentaria", "Proteínas", "Sal"). Funciona tanto si cada fila sale en una línea
 * como si los nombres y los números salen en columnas separadas (se emparejan por altura).
 * Cuando hay varias columnas (por 100 g / por ración / %IR) se toma la primera: por 100 g.
 */
object LabelParser {
    private enum class Key { SAT, SUGAR, FAT, CARBS, FIBER, PROTEIN, SALT, SODIUM }

    // Orden importante: "saturadas" y "azúcares" antes que "grasas" e "hidratos".
    private val keys: List<Pair<Key, Regex>> = listOf(
        Key.SAT to Regex("(?<!in)satur"),
        Key.SUGAR to Regex("azucar|sugar|sucres"),
        Key.FAT to Regex("grasa|lipid|materia grasa|\\bfat\\b|matieres grasses"),
        Key.CARBS to Regex("hidrato|carbohidrat|glucid|carbohydrate"),
        Key.FIBER to Regex("fibra|fibre|fiber"),
        Key.PROTEIN to Regex("prote"),
        Key.SALT to Regex("^\\W*sal\\b|\\bsal\\s*\\(|\\bsal$|\\bsalt\\b|\\bsel\\b"),
        Key.SODIUM to Regex("sodio|sodium"),
    )

    private val num = "(\\d{1,4}(?:[.,]\\d{1,3})?)"
    private val gramValue = Regex("(<\\s*)?$num\\s*(mg|g|gr)?(?![\\d%])", RegexOption.IGNORE_CASE)
    private val kcalRe = Regex("$num\\s*k\\s*cal", RegexOption.IGNORE_CASE)
    private val kjRe = Regex("$num\\s*k\\s*j", RegexOption.IGNORE_CASE)

    fun parse(lines: List<OcrLine>): LabelData {
        val clean = lines.map { it.copy(text = fixOcr(it.text)) }.filter { it.text.isNotBlank() }
        val all = clean.joinToString("\n") { it.text }
        val normAll = LocalFoodDb.normalize(all)

        // --- energía
        // filas cuyo nombre es "Energía/Valor energético"; si no hay, las celdas con kcal/kJ en orden de lectura
        val energyRows = clean.filter { "energ" in norm(it) }.ifEmpty {
            clean.filter { norm(it).let { n -> "kcal" in n || "kj" in n } }.sortedWith(compareBy({ it.top / 20 }, { it.left }))
        }
        var kcal: Double? = null
        for (row in energyRows) {
            val texts = listOf(row) + rightOf(row, clean)
            for (t in texts) {
                kcalRe.find(t.text)?.let { kcal = toNum(it.groupValues[1]) }
                if (kcal != null) break
            }
            if (kcal != null) break
        }
        if (kcal == null) kcal = clean.sortedWith(compareBy({ it.top / 20 }, { it.left }))
            .firstNotNullOfOrNull { l -> kcalRe.find(l.text)?.let { toNum(it.groupValues[1]) } }
        if (kcal == null) kcal = kjRe.find(all)?.let { toNum(it.groupValues[1])?.div(4.184) }

        // --- nutrientes
        val found = mutableMapOf<Key, Double>()
        for (line in clean) {
            val n = norm(line)
            if ("energ" in n || "kcal" in n) continue
            val key = keys.firstOrNull { (_, re) -> re.containsMatchIn(n) }?.first ?: continue
            if (key in found) continue
            // número en la misma línea, después del nombre
            val afterName = line.text.substringAfterLast(':', line.text)
            val inline = firstGrams(stripLeadingWords(afterName))
            val value = inline ?: rightOf(line, clean).firstNotNullOfOrNull { firstGrams(it.text) }
            if (value != null) found[key] = value
        }
        val salt = found[Key.SALT] ?: found[Key.SODIUM]?.let { it * 2.5 }

        // --- envase y ración
        val unit = if (Regex("100\\s*ml").containsMatchIn(normAll.replace(" ", " "))) "ml" else "g"
        val pkg = Regex("(peso neto|contenido neto|cont neto|neto|net weight|poids net)\\D{0,12}$num\\s*(kg|g|ml|cl|l)\\b")
            .find(normAll)?.let { convert(it.groupValues[2], it.groupValues[3]) }
            ?: Regex("$num\\s*(g|ml|kg|cl|l)\\s*℮").find(all.lowercase())?.let { convert(it.groupValues[1], it.groupValues[2]) }
        val serving = Regex("(racion|porcion|serving|portion)\\D{0,15}$num\\s*(g|ml)\\b").find(normAll)
            ?.let { toNum(it.groupValues[2]) }

        return LabelData(
            kcal = kcal?.takeIf { it in 0.0..950.0 },
            fat = found[Key.FAT]?.takeIf { it <= 100 },
            satFat = found[Key.SAT]?.takeIf { it <= 100 },
            carbs = found[Key.CARBS]?.takeIf { it <= 100 },
            sugar = found[Key.SUGAR]?.takeIf { it <= 100 },
            fiber = found[Key.FIBER]?.takeIf { it <= 100 },
            protein = found[Key.PROTEIN]?.takeIf { it <= 100 },
            salt = salt?.takeIf { it <= 100 },
            packageAmount = pkg,
            servingAmount = serving,
            unit = unit,
        )
    }

    private fun norm(l: OcrLine) = LocalFoodDb.normalize(l.text)

    /** Líneas a la derecha y a la misma altura, de izquierda a derecha. */
    private fun rightOf(line: OcrLine, all: List<OcrLine>): List<OcrLine> =
        all.filter { o ->
            o !== line && o.left >= line.right - line.height &&
                overlap(line, o) >= 0.45 * min(line.height, o.height).coerceAtLeast(1)
        }.sortedBy { it.left }

    private fun overlap(a: OcrLine, b: OcrLine) = max(0, min(a.bottom, b.bottom) - max(a.top, b.top))

    /** Quita las palabras del nombre ("de las cuales saturadas") para quedarse con los números. */
    private fun stripLeadingWords(s: String) = s.replace(Regex("^[^\\d<]*"), "")

    private fun firstGrams(s: String): Double? {
        for (m in gramValue.findAll(s)) {
            val v = toNum(m.groupValues[2]) ?: continue
            val unit = m.groupValues[3].lowercase()
            return if (unit == "mg") v / 1000 else v
        }
        return null
    }

    private fun toNum(s: String) = s.replace(',', '.').toDoubleOrNull()

    private fun convert(n: String, unit: String): Double? {
        val v = toNum(n) ?: return null
        return when (unit.lowercase()) { "kg", "l" -> v * 1000; "cl" -> v * 10; else -> v }
    }

    /** Error típico del OCR en números: "O" por "0" pegada a cifras. */
    private fun fixOcr(s: String): String = s
        .replace(Regex("(?<=\\d)[oO](?=\\D|$)"), "0")
        .replace(Regex("(?<=[\\d,.])[oO](?=\\d)"), "0")
        .replace(Regex("(?<=\\s|^)[oO](?=[,.]\\d)"), "0")
        .replace(Regex("(?<=[\\s:]|^)[oO](?=\\s*(g|mg)\\b)"), "0")
}
