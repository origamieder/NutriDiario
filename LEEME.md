# NutriDiario

App Android (Kotlin + Jetpack Compose) para registrar lo que comes y calcular calorías y nutrientes.

## Formas de añadir comida
- **Código de barras**: cámara + ML Kit (modelo incluido en la app) → datos de Open Food Facts (requiere internet).
- **Búsqueda**: base local de unos 160 alimentos genéricos (sin conexión) + productos de Open Food Facts (online).
- **Foto con IA local**: modelo EfficientNet-Lite2 (Google/MediaPipe) dentro del APK, funciona sin internet.
  Reconoce unas 50 comidas comunes (pizza, hamburguesa, fruta, verdura, pasta carbonara, helado…); tú confirmas y ajustas los gramos.
- **Decírselo**: texto o voz, p. ej. "150 g de pollo", "2 huevos", "un vaso de leche".
- **Crear alimento**: copiando los valores de la etiqueta nutricional (por 100 g o por ración).

## Perfil
Sexo, edad, altura, peso, actividad y objetivo → IMC, rango de peso saludable, peso ideal (Devine, Robinson,
Miller, Hamwi, IMC 22), grasa corporal estimada, metabolismo basal (Mifflin-St Jeor), gasto diario,
objetivo calórico y de macronutrientes. Pestaña "Progreso" con gráfica de peso y calorías.

Los datos se guardan solo en el móvil.

## Compilar
Requisitos: JDK 17+ y Android SDK (platform 35). `./gradlew assembleRelease` →
`app/build/outputs/apk/release/app-release.apk`.
