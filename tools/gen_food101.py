"""Añade a foods.json los 101 platos de Food-101 (modelo de IA de fotos), en español.
Las etiquetas del modelo se guardan como alias para que la IA encuentre el alimento.
Uso: python3 tools/gen_food101.py  (es idempotente)"""
import json, os
P = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "foods.json")
# etiqueta | nombre (o =Nombre existente) | kcal prot carb azuc grasa sat fibra sal (por 100 g) | ración g | nombre ración
T = """
apple_pie|Tarta de manzana|237 2 34 16 11 4 1.6 0.5|120|1 porción
baby_back_ribs|Costillas de cerdo asadas|290 24 4 3 20 7 0 1.0|250|1 ración
baklava|Baklava|428 6 52 25 23 7 2.5 0.3|60|2 piezas
beef_carpaccio|Carpaccio de ternera|150 20 1 0 7.5 2.5 0 1|100|1 plato
beef_tartare|Steak tartar|200 20 2 1 12 4 0 1|150|1 ración
beet_salad|Ensalada de remolacha|90 3 10 7 4.5 1.5 2.5 0.5|200|1 plato
beignets|Buñuelos|390 6 45 15 20 5 1.2 0.6|80|3 unidades
bibimbap|Bibimbap|120 6 17 2 3.5 0.8 1.5 0.7|450|1 bol
bread_pudding|Pudin de pan|200 5 30 18 7 3.5 0.8 0.4|150|1 porción
breakfast_burrito|Burrito de desayuno|210 10 20 1.5 10 4 1.5 1.1|250|1 unidad
bruschetta|Bruschetta|180 5 25 3 7 1.2 2 0.9|100|2 unidades
caesar_salad|Ensalada César|130 9 6 1.5 8 2.5 1.5 0.8|250|1 plato
cannoli|Cannoli|370 7 40 22 20 9 1 0.3|80|1 unidad
caprese_salad|Ensalada caprese|170 9 4 3 13 6 0.8 0.6|200|1 plato
carrot_cake|Tarta de zanahoria|410 4.5 50 35 22 5 1.5 0.5|110|1 porción
ceviche|Ceviche|100 15 6 2 1.5 0.3 0.8 0.8|200|1 plato
cheesecake|=Tarta / pastel
cheese_plate|Tabla de quesos|380 24 2 1 31 19 0 1.7|80|1 ración
chicken_curry|Pollo al curry|150 12 6 2 8.5 3 1 0.8|350|1 plato
chicken_quesadilla|Quesadilla de pollo|290 17 22 2 15 7 1.5 1.2|200|1 unidad
chicken_wings|Alitas de pollo|290 27 0 0 19.5 5.5 0 0.9|150|6 alitas
chocolate_cake|Tarta de chocolate|370 5 50 36 17 7 2.5 0.5|110|1 porción
chocolate_mousse|Mousse de chocolate|270 5 22 20 18 10 1.5 0.1|100|1 copa
churros|=Churros
clam_chowder|Crema de almejas|90 4.5 8 1 4.5 2 0.4 0.8|300|1 plato
club_sandwich|Sándwich club|250 14 20 3 12.5 3.5 1.5 1.3|250|1 unidad
crab_cakes|Pastelitos de cangrejo|210 13 10 1 13 2.5 0.5 1.2|120|2 unidades
creme_brulee|Crema catalana / crème brûlée|260 4 20 18 18 10 0 0.1|120|1 cazuelita
croque_madame|Croque madame|270 15 18 3 15 7.5 1 1.6|250|1 unidad
cup_cakes|Cupcake|390 4 55 38 18 7 1 0.5|70|1 unidad
deviled_eggs|Huevos rellenos|200 11 2 1 16.5 3.5 0 0.6|100|2 mitades
donuts|=Donut
dumplings|Dumplings / empanadillas chinas|200 8.5 25 2 7.5 2.5 1.5 1.0|150|6 piezas
edamame|Edamame|121 12 9 2 5 0.6 5 0.02|100|1 ración
eggs_benedict|Huevos benedictinos|230 12 12 1.5 15 6.5 0.5 1.1|250|2 huevos
escargots|Caracoles|120 15 2 0 5.5 2 0 0.5|100|1 ración
falafel|Falafel|333 13 32 0 18 2.5 5 0.7|120|5 bolitas
filet_mignon|Solomillo de ternera|220 28 0 0 12 5 0 0.15|200|1 pieza
fish_and_chips|Pescado rebozado con patatas|230 11 21 0.5 11.5 2 1.5 0.6|350|1 plato
foie_gras|Foie gras|460 11 4.5 1 44 14.5 0 1.2|50|1 ración
french_fries|=Patatas fritas
french_onion_soup|Sopa de cebolla gratinada|85 4 8 3 4 2 0.8 0.8|350|1 bol
french_toast|Torrijas / tostada francesa|230 7.5 27 11 10 3 1 0.5|150|2 unidades
fried_calamari|=Calamares a la romana
fried_rice|Arroz frito|163 4.5 24 1 5.5 1 0.7 0.9|300|1 plato
frozen_yogurt|Yogur helado|160 4 28 25 3.5 2 0 0.15|150|1 tarrina
garlic_bread|Pan de ajo|350 8 42 3 17 5 2 1.2|80|2 rebanadas
gnocchi|Ñoquis|150 4 28 1 2.5 1 1.5 0.6|250|1 plato
greek_salad|Ensalada griega|110 3.5 5 3 8.5 3.5 1.5 0.9|250|1 plato
grilled_cheese_sandwich|Sándwich de queso a la plancha|330 13 30 4 18 9 1.5 1.6|150|1 unidad
grilled_salmon|=Salmón
guacamole|=Guacamole
gyoza|Gyozas|210 8.5 24 2 9 2.5 1.5 1.1|150|6 piezas
hamburger|=Hamburguesa con queso
hot_and_sour_soup|Sopa agripicante|40 2.5 4.5 1 1.2 0.3 0.5 0.9|300|1 bol
hot_dog|=Perrito caliente
huevos_rancheros|Huevos rancheros|150 7 12 2 8 2.5 3 0.8|300|1 plato
hummus|=Hummus
ice_cream|=Helado
lasagna|=Lasaña
lobster_bisque|Crema de marisco|95 4.5 6 2 6 3.5 0.3 0.8|300|1 plato
lobster_roll_sandwich|Bocadillo de bogavante|220 13 22 3 9 2 1 1.2|200|1 unidad
macaroni_and_cheese|Macarrones con queso|165 7 18 2 7.5 4 0.8 0.8|300|1 plato
macarons|Macarons|410 7 58 50 17 4 2 0.2|40|3 unidades
miso_soup|Sopa de miso|25 2 2.5 0.6 1 0.2 0.5 1.0|250|1 bol
mussels|Mejillones|86 12 3.7 0 2.2 0.4 0 0.7|200|1 ración
nachos|Nachos con queso|340 9 33 2 19.5 7 3 1.2|150|1 ración
omelette|=Tortilla francesa
onion_rings|Aros de cebolla|410 4.5 39 5 26 4 2.5 1.1|100|1 ración
oysters|Ostras|68 7 4 0 2.5 0.7 0 0.5|100|6 ostras
pad_thai|Pad thai|175 8 22 6 6 1 1.5 1.1|350|1 plato
paella|=Paella
pancakes|Tortitas americanas|227 6.5 28 6 10 2.5 1 0.9|150|3 tortitas
panna_cotta|Panna cotta|230 3 20 18 15.5 9.5 0 0.1|120|1 unidad
peking_duck|Pato laqueado|225 19 6 5 14 4.5 0 1.1|200|1 ración
pho|Pho (sopa vietnamita)|70 5 8 1 1.8 0.6 0.4 0.9|500|1 bol
pizza|=Pizza
pork_chop|=Chuleta de cerdo
poutine|Poutine|230 6.5 25 1 12 5 2 1|300|1 ración
prime_rib|Costillar de ternera asado|290 22 0 0 22 9 0 0.2|250|1 ración
pulled_pork_sandwich|Bocadillo de cerdo desmigado|250 14 26 7 10 3.5 1 1.3|250|1 unidad
ramen|Ramen|90 5 11 1 3 1 0.6 1.1|550|1 bol
ravioli|Raviolis|180 7.5 26 2 5.5 2.5 1.5 0.7|250|1 plato
red_velvet_cake|Tarta red velvet|390 4 52 38 19 6 0.8 0.6|110|1 porción
risotto|Risotto|170 5 25 1 5.5 2.5 0.6 0.6|300|1 plato
samosa|Samosa|260 5 30 2 13.5 3 2.5 0.8|100|2 unidades
sashimi|Sashimi|130 21 0 0 5 1 0 0.2|150|1 ración
scallops|Vieiras|110 20 5 0 1 0.2 0 0.9|120|1 ración
seaweed_salad|Ensalada de algas|110 1.5 13 9 5.5 0.8 3 2|100|1 ración
shrimp_and_grits|Gambas con sémola|150 9 13 1 7 3 0.8 0.9|300|1 plato
spaghetti_bolognese|=Espaguetis a la boloñesa
spaghetti_carbonara|=Espaguetis a la carbonara
spring_rolls|Rollitos de primavera|230 5 26 3 12 2.5 2 1|120|2 unidades
steak|=Filete de ternera
strawberry_shortcake|Tarta de fresas con nata|300 4 35 20 16 9 1 0.3|120|1 porción
sushi|=Sushi
tacos|Tacos|210 10 18 2 11 4 2.5 0.9|200|2 tacos
takoyaki|Takoyaki|190 7 20 2 9 2 0.8 0.9|150|6 bolitas
tiramisu|Tiramisú|300 6 30 20 17 10 0.5 0.1|120|1 porción
tuna_tartare|Tartar de atún|160 21 3 1.5 7 1.2 0.6 1|150|1 ración
waffles|Gofres|290 8 33 6 14 3.5 1.5 0.9|100|1 gofre
"""
foods = json.load(open(P, encoding="utf-8"))
by_name = {f["name"]: f for f in foods}
labels = json.load(open(P.replace("foods.json", "food101_labels.json")))
seen = set()
for line in T.strip().splitlines():
    f = line.split("|"); label = f[0]; alias = label.replace("_", " ")
    seen.add(label)
    if f[1].startswith("="):
        item = by_name[f[1][1:]]
    elif f[1] in by_name:
        item = by_name[f[1]]
    else:
        v = [float(x) for x in f[2].split()]; assert len(v) == 8, line
        item = dict(name=f[1], aliases=[], kcal=v[0], protein=v[1], carbs=v[2], sugar=v[3], fat=v[4], satFat=v[5],
                    fiber=v[6], salt=v[7], portionG=float(f[3]), portionName=f[4])
        foods.append(item); by_name[item["name"]] = item
    if alias not in item["aliases"]:
        item["aliases"].append(alias)
assert seen == set(labels), set(labels) ^ seen
json.dump(foods, open(P, "w", encoding="utf-8"), ensure_ascii=False)
print(len(foods), "alimentos en total; etiquetas Food-101 cubiertas:", len(seen))
