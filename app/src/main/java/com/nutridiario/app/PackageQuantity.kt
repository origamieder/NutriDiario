package com.nutridiario.app

data class PackageQuantity(val amount: Double, val unit: String) {
    companion object {
        fun parse(quantity: String?, productQuantity: Double?, productUnit: String?) =
            PackageQuantityParser.parse(quantity, productQuantity, productUnit)
    }
}

/**
 * Interpreta el contenido del envase de Open Food Facts: "500 ml", "50 cl", "0,5 L", "160 g",
 * y packs como "6 x 25 g" o "4 x 330 ml" (devuelve una unidad del pack).
 */
object PackageQuantityParser {
    private val pack = Regex("(\\d+)\\s*[x×]\\s*(\\d+(?:[.,]\\d+)?)\\s*(kg|g|gr|ml|cl|l)\\b", RegexOption.IGNORE_CASE)
    private val single = Regex("(\\d+(?:[.,]\\d+)?)\\s*(kg|g|gr|grs|gramos|ml|cl|l|litros?)\\b", RegexOption.IGNORE_CASE)

    fun parse(quantity: String?, productQuantity: Double?, productUnit: String?): PackageQuantity? {
        val q = quantity.orEmpty()
        pack.find(q)?.let { m -> return convert(m.groupValues[2], m.groupValues[3]) }
        single.find(q)?.let { m -> return convert(m.groupValues[1], m.groupValues[2]) }
        if (productQuantity != null) {
            val u = productUnit.orEmpty().lowercase()
            return PackageQuantity(productQuantity, if (u == "ml" || u == "cl" || u == "l") "ml" else "g")
        }
        return null
    }

    private fun convert(num: String, unit: String): PackageQuantity? {
        val n = num.replace(',', '.').toDoubleOrNull() ?: return null
        val r = when (unit.lowercase()) {
            "kg" -> PackageQuantity(n * 1000, "g")
            "ml" -> PackageQuantity(n, "ml")
            "cl" -> PackageQuantity(n * 10, "ml")
            "l", "litro", "litros" -> PackageQuantity(n * 1000, "ml")
            else -> PackageQuantity(n, "g")
        }
        return r.takeIf { it.amount > 0 }
    }
}
