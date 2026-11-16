package com.nutridiario.app

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

data class IdealWeight(val formula: String, val kg: Double)

data class Targets(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugarMax: Double,
    val satFatMax: Double,
    val saltMax: Double,
    val waterMl: Double,
)

class HealthReport(val p: Profile) {
    private val hM = p.heightCm / 100.0
    private val inchesOver5ft = (p.heightCm / 2.54 - 60.0)
    private val male = p.sex == Sex.MALE

    val bmi = p.weightKg / hM.pow(2)

    val bmiCategory: String = when {
        bmi < 16 -> "Delgadez severa"
        bmi < 18.5 -> "Bajo peso"
        bmi < 25 -> "Peso normal"
        bmi < 30 -> "Sobrepeso"
        bmi < 35 -> "Obesidad grado I"
        bmi < 40 -> "Obesidad grado II"
        else -> "Obesidad grado III"
    }

    val healthyMin = 18.5 * hM.pow(2)
    val healthyMax = 24.9 * hM.pow(2)

    /** Fórmulas clásicas de peso ideal (todas usan la altura y el sexo). */
    val idealFormulas: List<IdealWeight> = listOf(
        IdealWeight("Devine (1974)", if (male) 50 + 2.3 * inchesOver5ft else 45.5 + 2.3 * inchesOver5ft),
        IdealWeight("Robinson (1983)", if (male) 52 + 1.9 * inchesOver5ft else 49 + 1.7 * inchesOver5ft),
        IdealWeight("Miller (1983)", if (male) 56.2 + 1.41 * inchesOver5ft else 53.1 + 1.36 * inchesOver5ft),
        IdealWeight("Hamwi (1964)", if (male) 48 + 2.7 * inchesOver5ft else 45.5 + 2.2 * inchesOver5ft),
        IdealWeight("IMC 22 (OMS)", 22 * hM.pow(2)),
    )

    /** Peso ideal de referencia: media de las fórmulas, limitada al rango saludable. */
    val idealWeight: Double = idealFormulas.map { it.kg }.average().coerceIn(healthyMin, healthyMax)

    val diffToIdeal = p.weightKg - idealWeight

    /** % grasa corporal estimado (Deurenberg, a partir del IMC). Orientativo. */
    val bodyFatPct = 1.2 * bmi + 0.23 * p.age - 10.8 * (if (male) 1 else 0) - 5.4

    val bodyFatCategory: String = run {
        val f = bodyFatPct
        if (male) when {
            f < 6 -> "Muy bajo"; f < 14 -> "Atlético"; f < 18 -> "En forma"; f < 25 -> "Normal"; else -> "Elevado"
        } else when {
            f < 14 -> "Muy bajo"; f < 21 -> "Atlético"; f < 25 -> "En forma"; f < 32 -> "Normal"; else -> "Elevado"
        }
    }

    /** Metabolismo basal (Mifflin-St Jeor). */
    val bmr = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + if (male) 5 else -161

    /** Gasto energético total diario. */
    val tdee = bmr * p.activity.factor

    val targets: Targets = run {
        val minKcal = if (male) 1500.0 else 1200.0
        val kcal = max(tdee + p.goal.kcalDelta, minKcal)
        // proteína según objetivo; en sobrepeso se calcula sobre el peso saludable máximo
        val refWeight = if (bmi > 25) healthyMax else p.weightKg
        val protPerKg = when (p.goal) {
            Goal.MAINTAIN -> if (p.activity.factor >= 1.55) 1.4 else 1.2
            Goal.LOSE, Goal.LOSE_SLOW -> 1.6
            Goal.GAIN -> 1.8
        }
        val protein = refWeight * protPerKg
        val fat = kcal * 0.30 / 9
        val carbs = max((kcal - protein * 4 - fat * 9) / 4, 0.0)
        Targets(
            kcal = kcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = kcal / 1000 * 14,
            sugarMax = kcal * 0.10 / 4,
            satFatMax = kcal * 0.10 / 9,
            saltMax = 5.0,
            waterMl = p.weightKg * 35,
        )
    }

    /** Semanas aproximadas para llegar al peso ideal con el objetivo actual (null si no aplica). */
    val weeksToIdeal: Int? = run {
        val kgPerWeek = -p.goal.kcalDelta * 7 / 7700.0 // positivo si pierde
        when {
            diffToIdeal > 1 && kgPerWeek > 0 -> (diffToIdeal / kgPerWeek).roundToInt()
            diffToIdeal < -1 && kgPerWeek < 0 -> (-diffToIdeal / -kgPerWeek).roundToInt()
            else -> null
        }
    }

    val advice: String = when {
        bmi < 18.5 -> "Estás por debajo del rango saludable. Te conviene ganar ${fmt(healthyMin - p.weightKg)} kg como mínimo; prioriza proteína y comidas completas. Considera el objetivo \"Ganar peso\"."
        bmi < 25 && kotlin.math.abs(diffToIdeal) <= 3 -> "¡Estás en tu peso ideal! Mantén el objetivo \"Mantener peso\" y céntrate en la calidad de la dieta."
        bmi < 25 -> "Estás dentro del rango saludable. Tu peso de referencia ideal sería ${fmt(idealWeight)} kg (diferencia ${fmtSigned(diffToIdeal)} kg)."
        else -> "Estás ${fmt(p.weightKg - healthyMax)} kg por encima del rango saludable y ${fmt(diffToIdeal)} kg por encima de tu peso ideal estimado. Un déficit moderado (-250 a -500 kcal/día) es lo recomendado."
    }
}

fun fmt(v: Double, dec: Int = 1): String =
    if (dec == 0) v.roundToInt().toString() else String.format(java.util.Locale("es", "ES"), "%.${dec}f", v)

fun fmtSigned(v: Double) = (if (v > 0) "+" else "") + fmt(v)
