package com.nutridiario.app.ui

import androidx.compose.ui.graphics.Color
import com.nutridiario.app.LocalFoodDb
import com.nutridiario.app.Meal
import kotlin.math.abs

/** Emoji representativo de un alimento, a partir de su nombre. */
private val emojiRules: List<Pair<List<String>, String>> = listOf(
    listOf("monster", "red bull", "energetica", "burn") to "⚡",
    listOf("cola", "refresco", "fanta", "sprite", "pepsi", "tonica") to "🥤",
    listOf("cerveza", "cana") to "🍺",
    listOf("vino") to "🍷",
    listOf("cafe", "espresso", "capuchino", "cortado") to "☕",
    listOf("te ", "infusion", "manzanilla") to "🍵",
    listOf("zumo", "jugo") to "🧃",
    listOf("batido", "proteina", "whey") to "🥛",
    listOf("leche", "bebida de") to "🥛",
    listOf("agua") to "💧",
    listOf("yogur", "kefir") to "🥣",
    listOf("helado", "polo") to "🍦",
    listOf("chocobom", "chocolate", "cacao", "nutella", "nocilla", "bombon") to "🍫",
    listOf("galleta", "oreo", "principe", "digestive") to "🍪",
    listOf("donut", "berlina") to "🍩",
    listOf("tarta", "pastel", "bizcocho", "magdalena", "muffin") to "🧁",
    listOf("croissant", "cruasan") to "🥐",
    listOf("churro") to "🥨",
    listOf("pretzel") to "🥨",
    listOf("flan", "natillas", "pudin", "crema catalana") to "🍮",
    listOf("miel") to "🍯",
    listOf("caramelo", "gominola", "chuche", "azucar") to "🍬",
    listOf("pizza") to "🍕",
    listOf("hamburguesa", "burger") to "🍔",
    listOf("perrito", "hot dog", "salchicha") to "🌭",
    listOf("burrito", "kebab", "wrap", "fajita") to "🌯",
    listOf("taco", "nachos") to "🌮",
    listOf("sushi", "maki") to "🍣",
    listOf("sandwich", "bocadillo", "bocata", "mixto") to "🥪",
    listOf("tostada", "pan", "baguette", "bagel") to "🍞",
    listOf("patatas fritas", "papas fritas") to "🍟",
    listOf("patatas de bolsa", "chips", "snack") to "🍿",
    listOf("palomitas") to "🍿",
    listOf("espagueti", "macarrones", "pasta", "lasana", "carbonara", "bolonesa", "fideo", "tallarin") to "🍝",
    listOf("paella", "arroz") to "🍚",
    listOf("lenteja", "garbanzo", "alubia", "cocido", "judias") to "🍲",
    listOf("sopa", "caldo", "consome", "crema de", "gazpacho", "salmorejo", "estofado", "guiso") to "🥣",
    listOf("tortilla", "huevo", "revuelto") to "🍳",
    listOf("pollo", "pavo", "nugget", "alitas") to "🍗",
    listOf("jamon", "chorizo", "embutido", "fuet", "salchichon", "york") to "🥓",
    listOf("ternera", "filete", "carne", "cerdo", "lomo", "chuleta", "albondiga", "costilla") to "🥩",
    listOf("salmon", "merluza", "atun", "sardina", "pescado", "bacalao") to "🐟",
    listOf("gamba", "langostino", "marisco", "calamar", "mejillon") to "🦐",
    listOf("queso", "mozzarella") to "🧀",
    listOf("mantequilla") to "🧈",
    listOf("ensalada", "lechuga") to "🥗",
    listOf("aguacate", "guacamole") to "🥑",
    listOf("brocoli", "coliflor") to "🥦",
    listOf("tomate", "ketchup") to "🍅",
    listOf("zanahoria") to "🥕",
    listOf("maiz") to "🌽",
    listOf("pepino", "calabacin") to "🥒",
    listOf("pimiento") to "🫑",
    listOf("berenjena") to "🍆",
    listOf("champinon", "seta") to "🍄",
    listOf("patata", "pure") to "🥔",
    listOf("cebolla") to "🧅",
    listOf("espinaca", "judias verdes", "verdura", "repollo", "alcachofa") to "🥬",
    listOf("platano", "banana") to "🍌",
    listOf("manzana") to "🍎",
    listOf("pera") to "🍐",
    listOf("naranja", "mandarina") to "🍊",
    listOf("limon") to "🍋",
    listOf("fresa") to "🍓",
    listOf("uva") to "🍇",
    listOf("sandia") to "🍉",
    listOf("melon") to "🍈",
    listOf("pina") to "🍍",
    listOf("melocoton", "nectarina") to "🍑",
    listOf("cereza", "picota") to "🍒",
    listOf("kiwi") to "🥝",
    listOf("mango") to "🥭",
    listOf("arandano") to "🫐",
    listOf("almendra", "nuez", "nueces", "cacahuete", "frutos secos") to "🥜",
    listOf("aceituna", "oliva") to "🫒",
    listOf("avena", "cereales", "muesli", "granola") to "🥣",
)

fun foodEmoji(name: String): String {
    val n = " " + LocalFoodDb.normalize(name) + " "
    // se compara al inicio de palabra para que "chocolate" no cuente como "cola" ni "espinaca" como "piña"
    return emojiRules.firstOrNull { (keys, _) -> keys.any { n.contains(" $it") } }?.second ?: "🍽️"
}

private val tints = listOf(
    Color(0xFFDCFCE7), Color(0xFFFFEDD5), Color(0xFFEDE9FE), Color(0xFFFCE7F3),
    Color(0xFFE0F2FE), Color(0xFFFEF9C3), Color(0xFFCCFBF1),
)

fun foodTint(name: String): Color = tints[abs(name.hashCode()) % tints.size]

val Meal.emoji: String
    get() = when (this) {
        Meal.BREAKFAST -> "☀️"; Meal.LUNCH -> "🍽️"; Meal.SNACK -> "🍎"; Meal.DINNER -> "🌙"
    }

val Meal.tint: Color
    get() = when (this) {
        Meal.BREAKFAST -> Color(0xFFFFEDD5); Meal.LUNCH -> Color(0xFFDCFCE7); Meal.SNACK -> Color(0xFFFCE7F3); Meal.DINNER -> Color(0xFFE0E7FF)
    }
