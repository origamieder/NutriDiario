# Modelo de platos (food101.tflite)

- Origen: [Lumia101/Food101-EfficientNet-B0](https://huggingface.co/Lumia101/Food101-EfficientNet-B0) (licencia MIT),
  EfficientNet-B0 entrenado con Food-101 (101 platos).
- Conversión: PyTorch → TFLite con `litert-torch`, añadiendo dentro del modelo la normalización
  ImageNet y el paso NHWC→NCHW. Entrada `[1,128,128,3]` RGB en 0..1, salida: probabilidades `[1,101]`.
  Preprocesado: lado corto a 160 px y recorte central de 128 px (`FoodClassifier.preprocess`).
- Cuantización: `ai-edge-quantizer`, receta `weight_only_wi8_afp32` (19 MB → 5,5 MB).
- Validación con 300 fotos de validación de Food-101 pasadas por el preprocesado real de Android:
  acierto 76,3 % a la primera, 93,7 % entre las 5 primeras (sin cuantizar: 74,7 % / 94,3 %).
- Etiquetas: `food101_labels.json`; cada una es alias de un alimento de `foods.json` (`tools/gen_food101.py`).

# Modelo general (food_model.tflite)

EfficientNet-Lite2 int8 de MediaPipe (ImageNet), para fruta, verdura y bebidas sueltas.
