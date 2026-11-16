"""Genera app/src/main/assets/ingredients.json y recipes.json (recetas de 1 ración)."""
import json, os
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
# id|nombre|emoji|alias|kcal prot carb azuc grasa sat fibra sal (por 100 g, en crudo)|despensa|busqueda tienda|unidad_g|unidad_nombre
ING = """
pollo|Pechuga de pollo|🍗|pollo,pechuga,pechuga de pollo,filete de pollo,pechugas|110 23 0 0 1.5 0.4 0 0.15|0|pechuga pollo||
muslo_pollo|Muslos de pollo|🍗|muslo,muslos,contramuslo,muslos de pollo|170 18 0 0 11 3 0 0.2|0|muslo pollo||
pavo|Pechuga de pavo|🦃|pavo,pechuga de pavo|105 23 0 0 1.2 0.4 0 0.15|0|pechuga pavo||
ternera|Filete de ternera|🥩|ternera,filete,filetes,bistec,carne de ternera,vaca|130 21 0 0 5 2 0 0.15|0|filete ternera||
carne_picada|Carne picada|🥩|carne picada,picada,carne picada mixta|250 17 0 0 20 8 0 0.2|0|carne picada||
cerdo|Lomo de cerdo|🥩|cerdo,lomo,lomo de cerdo,cinta de lomo|140 21 0 0 6 2 0 0.15|0|lomo cerdo||
chorizo|Chorizo|🌭|chorizo|455 24 2 1 38 14 0 4|0|chorizo||
jamon|Jamón serrano|🥓|jamon,jamon serrano,jamon iberico|241 30 0 0 13 4.5 0 4.5|0|jamon serrano||
jamon_york|Jamón cocido|🥓|york,jamon york,jamon cocido|110 18 1.5 1 3.5 1.2 0 2|0|jamon cocido||
bacon|Bacon|🥓|bacon,beicon,panceta|400 14 1 0 38 14 0 2.5|0|bacon||
salmon|Salmón|🐟|salmon|208 20 0 0 13 3 0 0.15|0|salmon||
merluza|Merluza|🐟|merluza,pescado blanco,bacalao,pescado|86 17 0 0 1.8 0.3 0 0.2|0|merluza||
atun|Atún en lata|🐟|atun,lata de atun,atun en aceite|198 29 0 0 8.2 1.3 0 0.9|0|atun aceite oliva|60|lata
gambas|Gambas|🦐|gambas,langostinos,gamba,camarones|99 24 0.2 0 0.3 0.1 0 0.6|0|gambas peladas||
huevo|Huevos|🥚|huevo,huevos|143 12.6 0.7 0.4 9.5 3.1 0 0.36|0|huevos|60|huevo
leche|Leche|🥛|leche|46 3.3 4.8 4.8 1.6 1 0 0.1|0|leche semidesnatada||
nata|Nata para cocinar|🥛|nata,nata para cocinar,nata liquida|195 2.5 4 3.5 18 11 0 0.1|0|nata cocinar||
queso|Queso rallado|🧀|queso,queso rallado,parmesano,emmental,queso curado|392 28 3 0.5 30 19 0 1.6|0|queso rallado||
queso_fresco|Queso fresco|🧀|queso fresco,queso de burgos,requeson|174 12.4 2.8 2.8 13 8 0 0.8|0|queso fresco||
mozzarella|Mozzarella|🧀|mozzarella|280 22 2.2 1 20 13 0 1.2|0|mozzarella||
yogur|Yogur natural|🥣|yogur,yogures,yogur natural,yogur griego|61 3.5 4.7 4.7 3.3 2.1 0 0.1|0|yogur natural|125|yogur
mantequilla|Mantequilla|🧈|mantequilla|717 0.9 0.1 0.1 81 51 0 0.03|0|mantequilla||
arroz|Arroz|🍚|arroz,arroz redondo,arroz largo,arroz basmati|354 7 78 0.2 0.6 0.2 1.3 0|0|arroz||
pasta|Pasta|🍝|pasta,macarrones,espaguetis,espagueti,tallarines,fideos,penne,spaghetti,tiburones|357 12.5 72 3 1.5 0.3 3 0|0|macarrones||
pan|Pan|🍞|pan,barra de pan,baguette,pan de barra|265 9 51 4.5 3.2 0.7 2.7 1.2|0|pan||
tortilla_trigo|Tortillas de trigo|🌯|tortilla de trigo,tortillas de trigo,wrap,wraps,tortillas mexicanas,fajitas|310 8.5 52 3 7.5 3 2.5 1.2|0|tortillas trigo|30|tortilla
patata|Patatas|🥔|patata,patatas,papa,papas|77 2 17 0.8 0.1 0 2.2 0|0|patatas||
avena|Copos de avena|🥣|avena,copos de avena|389 16.9 66 1 6.9 1.2 10.6 0|0|copos avena||
harina|Harina|🌾|harina|364 10 76 0.3 1 0.2 2.7 0|1|harina trigo||
cuscus|Cuscús|🍚|cuscus|376 12.8 77 0 0.6 0.1 5 0|0|cuscus||
quinoa|Quinoa|🍚|quinoa,quinua|368 14 64 0 6 0.7 7 0|0|quinoa||
lentejas|Lentejas cocidas (bote)|🥫|lentejas|116 9 20 1.8 0.4 0.1 7.9 0.5|0|lentejas cocidas||
garbanzos|Garbanzos cocidos (bote)|🥫|garbanzos|139 7.2 20 0.3 2.9 0.3 6 0.6|0|garbanzos cocidos||
alubias|Alubias cocidas (bote)|🥫|alubias,judias blancas,frijoles,alubias blancas|110 7 17 0.3 0.5 0.1 6 0.5|0|alubias cocidas||
tomate|Tomate|🍅|tomate,tomates|18 0.9 3.9 2.6 0.2 0 1.2 0.01|0|tomates||
tomate_frito|Tomate frito|🥫|tomate frito,salsa de tomate|75 1.5 9 7 3.5 0.4 1.5 1.2|0|tomate frito||
tomate_triturado|Tomate triturado|🥫|tomate triturado,tomate natural triturado,tomate en lata|25 1.2 4 3.5 0.2 0 1.2 0.2|0|tomate triturado||
cebolla|Cebolla|🧅|cebolla,cebollas,cebolleta|40 1.1 9.3 4.2 0.1 0 1.7 0.01|0|cebollas||
ajo|Ajo|🧄|ajo,ajos,diente de ajo|149 6.4 33 1 0.5 0.1 2.1 0.04|1|ajos|5|diente
pimiento|Pimiento|🌶️|pimiento,pimientos,pimiento rojo,pimiento verde|26 1 6 4.2 0.3 0 2.1 0|0|pimientos||
calabacin|Calabacín|🥒|calabacin,calabacines|17 1.2 3.1 2.5 0.3 0 1 0.02|0|calabacin||
berenjena|Berenjena|🍆|berenjena,berenjenas|25 1 5.9 3.5 0.2 0 3 0|0|berenjena||
zanahoria|Zanahoria|🥕|zanahoria,zanahorias|41 0.9 9.6 4.7 0.2 0 2.8 0.17|0|zanahorias||
champinones|Champiñones|🍄|champinones,champinon,setas,seta|22 3.1 3.3 2 0.3 0 1 0.01|0|champiñones laminados||
espinacas|Espinacas|🥬|espinacas,espinaca|23 2.9 3.6 0.4 0.4 0.1 2.2 0.2|0|espinacas||
lechuga|Lechuga|🥬|lechuga,ensalada,canonigos,rucula,brotes|15 1.4 2.9 0.8 0.2 0 1.3 0.07|0|lechuga||
pepino|Pepino|🥒|pepino,pepinos|15 0.7 3.6 1.7 0.1 0 0.5 0|0|pepino||
brocoli|Brócoli|🥦|brocoli|34 2.8 6.6 1.7 0.4 0 2.6 0.08|0|brocoli||
judias_verdes|Judías verdes|🥬|judias verdes,vainas|31 1.8 7 3.3 0.2 0 2.7 0.02|0|judias verdes||
guisantes|Guisantes|🥬|guisantes|81 5.4 14 5.7 0.4 0.1 5.1 0.01|0|guisantes congelados||
maiz|Maíz dulce|🌽|maiz,maiz dulce|86 3.3 19 6.3 1.4 0.2 2.7 0.04|0|maiz dulce||
aguacate|Aguacate|🥑|aguacate,aguacates|160 2 8.5 0.7 14.7 2.1 6.7 0|0|aguacate||
limon|Limón|🍋|limon,limones,lima|29 1.1 9.3 2.5 0.3 0 2.8 0|0|limones||
platano|Plátano|🍌|platano,platanos,banana|89 1.1 23 12.2 0.3 0.1 2.6 0|0|platanos|120|plátano
manzana|Manzana|🍎|manzana,manzanas|52 0.3 14 10.4 0.2 0 2.4 0|0|manzanas|180|manzana
fresas|Fresas|🍓|fresas,fresa,fresones|32 0.7 7.7 4.9 0.3 0 2 0|0|fresas||
frutos_rojos|Frutos rojos|🍇|arandanos,frutos rojos,frambuesas|57 0.7 14.5 10 0.3 0 2.4 0|0|frutos rojos||
nueces|Nueces|🥜|nueces,nuez,frutos secos,almendras|654 15 14 2.6 65 6 6.7 0|0|nueces||
miel|Miel|🍯|miel|304 0.3 82 82 0 0 0.2 0|0|miel||
cacao|Cacao en polvo|🍫|cacao,cola cao,colacao,cacao en polvo,chocolate|380 6 80 70 3 2 6 0.3|0|cacao soluble||
azucar|Azúcar|🍬|azucar|400 0 100 100 0 0 0 0|1|azucar||
tofu|Tofu|🧊|tofu|76 8 1.9 0.6 4.8 0.7 0.3 0.02|0|tofu||
aceite|Aceite de oliva|🧴|aceite,aceite de oliva|884 0 0 0 100 14 0 0|1|aceite oliva virgen extra||
sal|Sal|🧂|sal|0 0 0 0 0 0 0 100|1|sal||
especias|Especias|🌶️|pimienta,especias,oregano,comino,pimenton,curry,canela,perejil|250 10 64 0.6 3 1 25 0.1|1|especias||
vinagre|Vinagre|🍶|vinagre|18 0 0.6 0.4 0 0 0 0|1|vinagre||
caldo|Caldo (o agua + pastilla)|🥣|caldo,caldo de pollo,caldo de verduras,brick de caldo,pastilla de caldo|7 1 0.4 0.2 0.2 0.1 0 0.8|1|caldo pollo||
salsa_soja|Salsa de soja|🥢|soja,salsa de soja|60 8 6 1.7 0.1 0 0.8 14|0|salsa soja||
mayonesa|Mayonesa|🥚|mayonesa|680 1 0.6 0.6 75 6 0 1.2|0|mayonesa||
pan_rallado|Pan rallado|🍞|pan rallado|395 13 72 6 5 1 4.5 1.3|0|pan rallado||
"""
# nombre|emoji|comidas|minutos|ingredientes id:gramos|pasos separados por " / "
REC = open(os.path.join(os.path.dirname(__file__), "recipes.txt"), encoding="utf-8").read()

ings = []
for l in ING.strip().splitlines():
    f = l.split("|"); assert len(f) == 9, l
    v = [float(x) for x in f[4].split()]; assert len(v) == 8, l
    ings.append(dict(id=f[0], name=f[1], emoji=f[2], aliases=f[3].split(","), kcal=v[0], protein=v[1], carbs=v[2],
                     sugar=v[3], fat=v[4], satFat=v[5], fiber=v[6], salt=v[7], pantry=f[5] == "1", shop=f[6],
                     unitG=float(f[7]) if f[7] else None, unitName=f[8] or None))
ids = {i["id"] for i in ings}
recs = []
for n, l in enumerate([x for x in REC.strip().splitlines() if x.strip()]):
    f = l.split("|"); assert len(f) == 6, l
    items = []
    for p in f[4].split(","):
        i, g = p.split(":"); assert i in ids, (i, l); items.append({"id": i, "g": float(g)})
    recs.append(dict(id=n + 1, name=f[0], emoji=f[1], meals=f[2].split(","), minutes=int(f[3]), ingredients=items,
                     steps=[s.strip() for s in f[5].split(" / ")]))
json.dump(ings, open(os.path.join(OUT, "ingredients.json"), "w", encoding="utf-8"), ensure_ascii=False)
json.dump(recs, open(os.path.join(OUT, "recipes.json"), "w", encoding="utf-8"), ensure_ascii=False)
byid = {i["id"]: i for i in ings}
for r in recs:
    k = sum(byid[x["id"]]["kcal"] * x["g"] / 100 for x in r["ingredients"])
    p = sum(byid[x["id"]]["protein"] * x["g"] / 100 for x in r["ingredients"])
    print(f'{r["name"][:40]:40} {k:5.0f} kcal {p:5.1f} g prot')
print(len(ings), "ingredientes", len(recs), "recetas")
