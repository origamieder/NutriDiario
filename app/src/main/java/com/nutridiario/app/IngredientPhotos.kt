package com.nutridiario.app

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Fotos de ingredientes (productos Eroski de Open Food Facts), con caché permanente. */
object IngredientPhotos {
    // Open Food Facts limita las búsquedas: de una en una.
    private val gate = Semaphore(1)

    suspend fun get(i: Ingredient): IngredientImage? {
        Repository.data.value.ingredientImages[i.id]?.let { return it }
        return gate.withPermit {
            Repository.data.value.ingredientImages[i.id]?.let { return@withPermit it }
            val found = runCatching { OpenFoodFacts.ingredientImage(i.shop) }.getOrNull()
            // solo se guarda lo encontrado; si falla la red se reintentará otro día
            found?.also { Repository.cacheIngredientImage(i.id, it) }
        }
    }
}
