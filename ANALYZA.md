# Analýza – ZapisPotvrdenieDPN_v4 (eDPN.v4)

Zdroj: kapitoly *Špecifikácia štruktúry eDPN* a *Služba ZapisPotvrdenieDPN_v4* zo zadania. Ďalej sa na nejasnosti odkazujem ako `N01`…, na otázky ako `Q1`… a na testy ako `TC0001`….

## 0. Špecifikácia v skratke

### Strom dokumentu

```
ehr_extract.compositions[0].edpn                COMPOSITION  1..1  rc_id.@root = 1.3.158.00165387.100.40.400
└── content
    ├── potvrdenieDocasnejPraceneschopnosti     ENTRY at0013 1..1  items 6..12
    ├── diagnoza                                ENTRY at0014 1..1  items 1..2
    ├── dovodPracovnejNeschopnosti              ENTRY at0015 1..1  items 2..3
    ├── trvaniePracovnejNeschopnosti            ENTRY at0016 1..1  items 1..3
    ├── vychadzky                               ENTRY at0017 0..1  items 1..*
    │   └── povolenaVychadzka                   CLUSTER at0097 1..*  parts 2..*
    ├── vynimkaZPoistenia                       ENTRY at0096 0..*  items 1..*
    ├── zmenaStavuZaznamu                       ENTRY at0120 0..1  items 1..1
    │   └── zmenaStavuZaznamu                   CLUSTER at0121 (slot → Zmena_stavu_zaznamu.v2)  parts 2..4
    │       └── stornoval                       CLUSTER at0003 (slot → Zdravotnicky_pracovnik_odborneho_utvaru.v2)  parts 2..4
    └── zaznamVytvoril                          ENTRY at0133 1..1  items 1..1
        └── zaznamVytvoril                      CLUSTER at0136 (slot → Zdravotnicky_pracovnik_odborneho_utvaru.v2)  parts 2..4
```

### Súlad výskytov a kardinalít

Pre každú kolekciu som sčítala minimálny a maximálny počet položiek, ktorý pripúšťajú výskyty jej uzlov, a porovnala ho s kardinalitou.

| Kolekcia | Povinné uzly | Voliteľné uzly | Súčet výskytov | Kardinalita | Výsledok |
| --- | --- | --- | --- | --- | --- |
| at0013 `items` | 6 | 6 | 6..12 | 6..12 | sedí |
| at0014 `items` | 1 | 1 | 1..2 | 1..2 | sedí |
| at0015 `items` | 2 | 1 | 2..3 | 2..3 | sedí |
| at0016 `items` | 1 | 2 | 1..3 | 1..3 | sedí |
| at0017 `items` | `povolenaVychadzka` 1..* | – | 1..* | 1..* | sedí |
| at0097 `parts` | 2 | 1 | 2..3 | **2..\*** | horná hranica sa nedá dosiahnuť (N05) |
| at0096 `items` | – | `cisloPoistenia` 0..* | **0..\*** | **1..\*** | dolnú hranicu určuje kardinalita, nie výskyt (N06) |
| at0120 `items` | 1 (slot) | – | 1..1 | 1..1 | sedí |
| at0133 `items` | 1 (slot) | – | 1..1 | 1..1 | sedí |
| Zdravotnícky pracovník `parts` | 2 | 2 | 2..4 | 2..4 | sedí |
| Zmena stavu záznamu `parts` | 2 | 2 | 2..4 | 2..4 | sedí |

Dôsledok pre testy: pri väčšine kolekcií sa porušenie kardinality nedá vyrobiť bez súčasného porušenia výskytu niektorého uzla. Na jednom teste sa tak dá vidieť, ktoré pravidlo služba vyhodnotí ako prvé (N19).

### Minimálny a maximálny request

| | Minimum (`TC0001`) | Maximum (`TC0002`) |
| --- | --- | --- |
| at0013 | 6 povinných elementov | všetkých 12 elementov |
| at0014 | `diagnoza` | `diagnoza`, `platnostOd` |
| at0015 | `dovod`, `pozitieAlkoholu` | + `platnostOd` |
| at0016 | `datumPredpokladanehoUkoncenia` | + `poznamka`, `informaciaONasledujucejKontrole` |
| at0017 | – | 2× `povolenaVychadzka`, každá s 3 parts |
| at0096 | – | 1 ENTRY s 2× `cisloPoistenia` |
| at0120 | – | – (N18) |
| at0133 | `identifikaciaZprac`, `specializaciaLekara` | všetky 4 parts |
| Spolu elementov | 12 | 32 |

## 1. Nejasnosti a predpoklady

Každý predpoklad som volila tak, aby bol v súlade s textom špecifikácie a aby sa dal ľahko zmeniť. Vo frameworku je zakódovaný na jednom mieste (model špecifikácie alebo tvorba hodnôt).

### Štruktúra a identifikácia uzlov

**N01 – Rovnaké at-kódy v rôznych archetypoch.** Oba vnorené archetypy číslujú uzly od `at0001`. `at0003` je v archetype *Zdravotnícky pracovník* „Identifikácia OÚ PZS“ a v archetype *Zmena stavu záznamu* „Stornoval“. Pri vnorení cez slot `stornoval` sa oba objavia v tom istom podstrome. Aj ukážkový krok zo zadania („element [at0034] má hodnotu“) používa len at-kód.
*Predpoklad:* uzol identifikujeme vždy plným `archetype_id` (`<archetyp>/<at-kód>`). Krátky zápis `[at0034]` v krokoch znamená at-kód v archetype eDPN.v4. Pre uzly vnorených archetypov musí krok uviesť aj archetyp.

**N02 – Rovnaký JSON kľúč na dvoch úrovniach.** `diagnoza` je ENTRY (at0014) aj ELEMENT (at0019). `zmenaStavuZaznamu` a `zaznamVytvoril` sú ENTRY aj CLUSTER. Kľúč teda uzol jednoznačne neurčuje.
*Predpoklad:* to isté ako N01. Vyhľadáva sa podľa `archetype_id`, kľúč sa použije len na zostavenie requestu.

**N03 – `archetype_id` samotného slotu.** Nie je jasné, či CLUSTER v slote nesie `eDPN.v4/at0136` (uzol rodiča), alebo identifikátor vnoreného archetypu (napr. `…Zdravotnicky_pracovnik_odborneho_utvaru.v2` bez at-kódu).
*Predpoklad:* CLUSTER nesie at-kód slotu z rodičovského archetypu (`…eDPN.v4/at0136`, `…Zmena_stavu_zaznamu.v2/at0003`). Jeho `parts` nesú `archetype_id` vnoreného archetypu. Tak to zodpovedá tabuľkám, kde má slot vlastný at-kód v rodičovi. → `Q1`

**N04 – Dvojúrovňové vnorenie v storne.** *Zmena stavu záznamu* obsahuje slot `stornoval`, do ktorého sa vkladá *Zdravotnícky pracovník*. Špecifikácia neuvádza, či `stornoval` je sám CLUSTER s `parts`, alebo obal okolo ďalšieho CLUSTER.
*Predpoklad:* rovnaký tvar ako pri `zaznamVytvoril`. `stornoval` je CLUSTER a jeho `parts` sú elementy vnoreného archetypu.

### Kardinality a výskyty

**N05 – Povolená vychádzka `parts 2..*`.** CLUSTER má len tri rôzne elementy s výskytom 1..1, 1..1 a 0..1, takže viac ako 3 položky sa platne poslať nedajú. Horná hranica `*` je buď nepresnosť, alebo znamená, že výskyty elementov sa nekontrolujú.
*Predpoklad:* efektívna kardinalita je 2..3. Štvrtá položka (napr. duplicitný `casOd`) je E000004.

**N06 – Výnimka z poistenia.** ENTRY má výskyt 0..* a `items 1..*`, element `cisloPoistenia` má výskyt 0..*. Nie je jasné, kedy sa posiela viac ENTRY a kedy viac čísel v jednej ENTRY. Výskyt 0..* je pri jedinom možnom uzle v kolekcii s minimom 1 zavádzajúci.
*Predpoklad:* platí kardinalita, teda ENTRY bez čísla poistenia je E000004. V maximálnom scenári posielam 1 ENTRY s 2 číslami. Variant s 2 ENTRY je samostatný test s nízkou prioritou. → `Q6`

**N07 – Horná hranica pre opakovateľné uzly.** `povolenaVychadzka 1..*` a `vynimkaZPoistenia 0..*` nemajú maximum.
*Predpoklad:* žiadny limit. Testujeme 2 výskyty, záťažový test s veľkým počtom nerobíme.

**N08 – „Maximálne kardinality“ verzus „čo pri zápise dáva zmysel“.** Zadanie pre scenár 2 žiada oboje. Kardinalitu 12 v at0013 však dosiahnem len vtedy, ak pošlem aj `datumUmrtia` a `schopnyPraceOd`. Pri novom potvrdení PN sú obe hodnoty biznisovo nezvyčajné.
*Predpoklad:* scenár 2 je technické maximum. Obsahuje všetkých 12 elementov at0013, pretože je to jediný spôsob, ako overiť hornú hranicu kardinality. Biznisovú zmysluplnosť riešim pri at0120 (N18), kde ide o inú operáciu, nie o jeden údaj.

### Dátové typy a formáty

**N09 – Čiastočný dátum pri kľúčových dátumoch.** Formát „dátum“ pripúšťa `RRRR` a `RRRR-MM` aj pri „Práceneschopný od“ a „Dátum vystavenia“. Pre PN je to biznisovo nezmyselné.
*Predpoklad:* služba prijme každý formát zo zoznamu. Pozitívny test s čiastočným dátumom mám pri voliteľnom dátume, pri kľúčových dátumoch ho nechávam ako otázku. → `Q4`

**N10 – Hranice formátu dátumu a času.** Zoznam neobsahuje `RRRRMM` a nehovorí, či sa kontroluje platnosť dňa v kalendári (`2026-02-30`). Pri čase nie je povolená zmiešaná forma (`10:00` vs. `1000`), zlomky sekúnd ani `24:00`. Formát DATE_TIME nie je definovaný vôbec.
*Predpoklad:* platí len to, čo je vymenované. `RRRRMM` je neplatný. Kalendárna kontrola je otvorená, preto má jej test nízku prioritu. DATE_TIME je `RRRR-MM-DDThh:mm[:ss]` s voliteľnou zónou.

**N11 – Tvar `@value` pri BOOLEAN a INTEGER.** Vzor kontraktu ukazuje `@value` len ako reťazec pri DATE. Nie je jasné, či `true` a `1` sú reťazce alebo natívne JSON typy.
*Predpoklad:* všetky `@value` posielam ako reťazec (`"true"`, `"1"`), konzistentne so vzorom. → `Q10`

**N12 – Prázdny reťazec.** Pri SIMPLE_TEXT a iných typoch nie je dané, či `"@value": ""` znamená chýbajúcu hodnotu (E000003), alebo neplatnú hodnotu (E000004).
*Predpoklad:* prázdny reťazec pri povinnom elemente je E000004, lebo uzol prítomný je. Vo frameworku prázdna bunka tabuľky znamená „element neposielať“. `RequestBuilder` dnes vynechá každú prázdnu hodnotu, takže `"@value": ""` sa poslať nedá. TC0121 by potreboval vlastný zápis v tabuľke (napr. `(prázdny reťazec)`; zápis v `<…>` by Cucumber v osnove bral ako parameter), ktorý builder preloží na `""`.

**N13 – ATTACHMENT.** `@media_type` ∈ {`XML`, `JSON`} nie sú MIME typy (`application/json`). Kódovanie obsahu (text, base64) nie je určené.
*Predpoklad:* hodnoty doslovne podľa špecifikácie, obsah ako čistý text (krátky fiktívny JSON s adresou pobytu). Otázku k tomu nekladiem: týka sa jediného voliteľného elementu a jeho tvar je pri každom formáte rovnaký (`@media_type` + `@value`).

**N14 – INSTANCE_IDENTIFIER.** Pri „ľubovoľný OID“ nie je jasné, či služba overuje syntax OID. Nie je uvedené, či je `@extension` povinný, ani aký má formát (napr. kód lekára).
*Predpoklad:* `@root` musí mať syntax OID (`\d+(\.\d+)*`), `@extension` je povinný neprázdny reťazec. Hodnoty sú fiktívne.

**N15 – Význam `verziaSluzbyeDpn`.** Obmedzenie je len „≥ 1“. Nie je jasné, či musí zodpovedať verzii služby (4).
*Predpoklad:* platí len ≥ 1, posielam `4`. Hranice `0` a `1` sú test.

### Číselníky

**N16 – Čo služba pri CODED_VALUE overuje.** Nie je uvedené, či kontroluje len `@code` a `@code_system`, alebo aj `@code_schema_version` a `@display_name`. Pri zmene verzie číselníka na NCZI by testy so starou verziou mohli padať bez zmeny služby.
*Predpoklad:* overuje sa `@code` + `@code_system`. Verziu a názov beriem z jedného súboru s číselníkmi, takže sa aktualizujú na jednom mieste. → `Q7`

**N17 – Platnosť kódu v čase.** Kódy v číselníkoch majú obdobie platnosti. Nie je jasné, či sa platnosť posudzuje k dátumu odoslania alebo k `platnostOd` / `praceneschopnyOd`. Ak je číselník hierarchický, nie je jasné ani to, či sú platné aj nadradené (nekoncové) kódy.
*Predpoklad:* používam len kódy platné v deň behu. Testy na neplatný kód v čase navrhujem s nízkou prioritou.

### Kontrakt služby

**N18 – Zmena stavu záznamu pri zápise.** ENTRY at0120 nesie údaje o storne. Pri službe *Zápis* nedáva zmysel, aby nový záznam už obsahoval storno, hoci ho archetyp (0..1) pripúšťa.
*Predpoklad:* pri Zápise at0120 neposielam ani v maximálnom scenári. Patrí do služby Storno. Test „Zápis so stornom“ je biznisový test s neznámym výsledkom. → `Q5`

**N19 – Poradie a tvar chýb.** Pri viacerých porušeniach naraz nie je určené, ktorý kód príde. Pri chýbajúcom povinnom elemente sa súčasne poruší aj minimálna kardinalita (napr. chýbajúci `pozitieAlkoholu` → at0015 má 1 položku pri minime 2). Odpoveď pri chybe nemá definované, či obsahuje cestu k uzlu alebo text chyby.
*Predpoklad:* chýbajúci povinný uzol má prednosť (E000003). Negatívne testy navrhujem tak, aby každý porušil jedno pravidlo. Kde to nejde, test výslovne uvádza oba možné kódy. Overujem len `result.code`, nie `message`. → `Q2`

**N20 – Počet kompozícií.** Vstup je „`ehr_extract` s jednou kompozíciou“, ale `compositions` je pole. Pre prázdne pole a 2 kompozície nie je chybový kód.
*Predpoklad:* je to porušenie počtu výskytov, teda E000004.

**N21 – `rc_id` a idempotencia.** `rc_id` je rovnaké pre všetky uzly a `idPotvrdeniaDpn` je `rc_id` z requestu. ID teda generuje klient. Nie je opísané, čo sa stane pri opakovanom zápise s rovnakým `rc_id`, ani aký je formát `@extension`.
*Predpoklad:* klient generuje UUID, každý beh testu nové. Duplicitný zápis je otázka, test nenavrhujem, kým nie je známe správanie. → `Q9`

**N22 – Neznáme a nadbytočné uzly.** Špecifikácia hovorí, že `version_status`, `audits` a pod. „do requestu nedávajte“. Nie je jasné, či ich služba odmietne, alebo ignoruje. Rovnako nie je jasné, čo sa stane, ak kľúč a `archetype_id` uzla nesúhlasia (kľúč `praceneschopnyOd`, at-kód `at0037`).
*Predpoklad:* rozhoduje `archetype_id`, kľúč je len pomenovanie. Oba prípady navrhujem ako testy s nízkou prioritou.

### Biznis pravidlá, ktoré archetyp nevyjadruje

**N23 – Vzťahy medzi hodnotami.** Špecifikácia neobsahuje žiadne pravidlo naprieč elementmi, hoci z domény vyplývajú:

- `praceneschopnyOd` ≤ `schopnyPraceOd`
- `praceneschopnyOd` ≤ `datumPredpokladanehoUkoncenia`
- `datumVystavenia` ≤ `datumZmeny`, vystavenie nie v budúcnosti
- `casOd` < `casDo` v jednej vychádzke, vychádzky sa neprekrývajú
- `automatickyUkoncenaSystemom` = `true` pri ručnom zápise lekárom nedáva zmysel
- `datumUmrtia` ≥ `praceneschopnyOd`

*Predpoklad:* WireMock tieto pravidlá nesimuluje. Testy navrhujem s neznámym očakávaním, kým nepríde odpoveď na `Q3`.

**N24 – `codeValue` alebo `Skratka` v `@code`.** Položky číselníkov NCZI majú dva identifikátory: `codeValue` (napr. `02089403436`) a `Skratka` (napr. `J069` pre diagnózu J06.9). Špecifikácia neuvádza, ktorý sa posiela v `@code`. Zoznam diagnóz je navyše hierarchický: obsahuje aj nadradené položky (`J06`) a potvrdzuje tak N17.
*Predpoklad:* `@code` = `codeValue`, lebo je to identifikátor položky v číselníku. Skratka slúži na zobrazenie. Používam len koncové kódy diagnóz.

**N25 – Názov číselníka `.702` nezodpovedá poľu.** Špecifikácia priraďuje `pozitieAlkoholu` (at0076) číselník `1.3.158.00165387.100.10.702`. NCZI ho vedie pod názvom „Hodnotiace kritériá“, nie „Požitie alkoholu“. Ide o všeobecný číselník s položkami áno/nie.
*Predpoklad:* OID zo zadania zachovávam. Špecifikácia je jediný zdroj pre at0076, položky áno/nie zodpovedajú významu poľa a NCZI používa všeobecné číselníky pri viacerých poliach. Ak by išlo o chybu v špecifikácii, oprava je jeden riadok v `spec/edpn-v4.json` a jedna položka v `codelists.json`. → `Q8`

## 2. Otázky pre analytika

Zoradené podľa toho, koľko testov alebo kódu by odpoveď zmenila.

1. **Aké `archetype_id` má CLUSTER v slote** (`at0136`, `stornoval`) **a ako sa v ňom rozlišujú uzly s rovnakým at-kódom z rôznych archetypov?** Mení tvar každého requestu. (N01, N03, N04)
2. **Ktorý chybový kód príde, keď request porušuje viac pravidiel naraz** (napr. chýbajúci element zároveň znižuje kardinalitu)? **Obsahuje chybová odpoveď cestu k uzlu?** Rozhoduje o očakávaní vo všetkých negatívnych testoch. (N19)
3. **Ktoré pravidlá medzi hodnotami služba overuje a s akým kódom?** Napr. „schopný práce od“ pred začiatkom PN, `casOd` > `casDo`. (N23)
4. **Je pri „Práceneschopný od“, „Dátum vystavenia“ a „Dátum predpokladaného ukončenia“ naozaj povolený čiastočný dátum** (`RRRR`, `RRRR-MM`)? **Overuje sa kalendárna platnosť dňa?** (N09, N10)
5. **Smie Zápis obsahovať Zmenu stavu záznamu, Dátum úmrtia alebo `automatickyUkoncenaSystemom = true`?** (N08, N18, N23)
6. **Kedy sa posiela viac ENTRY „Výnimka z poistenia“ a kedy viac čísel poistenia v jednej?** (N06)
7. **Overuje služba pri CODED_VALUE aj `@code_schema_version` a `@display_name` a k akému dátumu posudzuje platnosť kódu?** (N16, N17)
8. **Je pre „Požitie alkoholu“ správny číselník `…10.702`?** NCZI ho vedie ako všeobecné „Hodnotiace kritériá“ (áno/nie). Element je povinný, takže zlý číselník by zmenil dáta v každom teste. (N25)
9. **Čo sa stane pri opakovanom zápise s rovnakým `rc_id`?** Má `@extension` predpísaný formát? (N21)
10. **Sú `@value` pri BOOLEAN a INTEGER reťazce, alebo natívne JSON typy?** Musí `verziaSluzbyeDpn` zodpovedať verzii služby? (N11, N15)

## 3. Návrh testov

### Ako som určovala prioritu

Prioritu určujú tri otázky:

1. **Dopad:** čo sa stane, keď služba pravidlo neuplatní? Prijatý záznam PN bez začiatku alebo s neplatnou diagnózou má priamy vplyv na poistenca aj Sociálnu poisťovňu.
2. **Pravdepodobnosť:** ako často takú chybu reálne urobí klient (ambulantný softvér)? Lokálny formát dátumu alebo kód z nesprávneho číselníka sú bežné. Dve kompozície v jednom requeste nie.
3. **Istota očakávania:** viem, čo má služba vrátiť? Test, ktorého výsledok závisí od otvorenej otázky, nemá vysokú prioritu, kým nepríde odpoveď.

| Priorita | Pravidlo |
| --- | --- |
| **vysoká** | hlavný tok, povinné uzly ENTRY at0013–at0016 a at0133, číselníky diagnózy a dôvodu, bežné chyby klientov |
| **stredná** | formáty a konštanty voliteľných častí, výskyty a kardinality, ktoré sa dajú porušiť samostatne |
| **nízka** | hraničné prípady s nejasným očakávaním (odkaz na N/Q), zriedkavé chyby, biznis pravidlá bez definovaného kódu |

Každý negatívny test vychádza z minimálneho requestu (`TC0001`) a mení práve jednu vec, ak nie je uvedené inak. Stĺpec **Impl.** označuje scenáre implementované v Časti 2.

### Pozitívne

| ID | Scenár | Zdroj | Očakávanie | Priorita | Impl. |
| --- | --- | --- | --- | --- | --- |
| TC0001 | Minimum: len povinné uzly (12 elementov) | výskyt 1..1, minimálne kardinality | 200, `result.code` 0, `idPotvrdeniaDpn.@extension` = `rc_id`, `@root` = `…40.400` | vysoká | ✔ |
| TC0002 | Maximum: 12 elementov v at0013, 2 vychádzky × 3 parts, 2 čísla poistenia, 4 parts v at0136 | maximálne kardinality, opakovanie | 200, ako TC0001 | vysoká | ✔ |
| TC0003 | `praceneschopnyOd` v tvare `RRRRMMDD` (`20261103`) | formát dátumu | 200 | stredná | |
| TC0004 | Časy vychádzky v tvaroch `10`, `10:00:00`, `10:00Z`, `10:00+01:00` | formát času | 200 | stredná | |
| TC0005 | Voľný text s diakritikou, úvodzovkami a `<`, `&` v `poznamka` | SIMPLE_TEXT | 200, text v requeste nezmenený | stredná | |
| TC0006 | Vychádzka len s povinnými parts (`casOd`, `casDo`) | parts 2..* | 200 | stredná | |
| TC0007 | Čiastočný dátum `RRRR-MM` v `platnostOd` diagnózy | formát dátumu (N09) | 200 | nízka | |
| TC0008 | 2× ENTRY „Výnimka z poistenia“, každá s 1 číslom | výskyt 0..* (N06) | 200 | nízka | |
| TC0009 | `liecbaVCudzine` = `true`, `nezmluvnyLekar` = `true` | BOOLEAN | 200 | nízka | |

### Negatívne – chýbajúci povinný uzol (E000003)

| ID | Scenár | Zdroj | Očakávanie | Priorita | Impl. |
| --- | --- | --- | --- | --- | --- |
| TC0101 | Chýba „Práceneschopný od“ (at0034) | výskyt 1..1 | 400 E000003 | vysoká | ✔ |
| TC0104 | Chýba celá ENTRY „Záznam vytvoril“ (at0133) | výskyt ENTRY 1..1, slot | 400 E000003 | vysoká | ✔ |
| TC0105 | Chýba celá ENTRY „Diagnóza“ (at0014) | výskyt ENTRY 1..1 | 400 E000003 | vysoká | |
| TC0106 | ENTRY „Diagnóza“ obsahuje len `platnostOd`, element `diagnoza` (at0019) chýba | výskyt 1..1, kardinalita 1..2 ostáva splnená | 400 E000003 | vysoká | |
| TC0107 | V slote at0136 chýba `identifikaciaZprac` (Zdravotnícky pracovník/at0004), ostatné 3 parts sú | výskyt vo vnorenom archetype | 400 E000003 | vysoká | |
| TC0108 | Chýba `pozitieAlkoholu` (at0076) | výskyt 1..1 + kardinalita 2..3 (N19) | 400 E000003, prípadne E000004 | stredná | |
| TC0109 | Vychádzka bez `casDo` (at0103) | výskyt 1..1 + parts 2..* (N19) | 400 E000003, prípadne E000004 | stredná | |
| TC0110 | Uzol bez `archetype_id`, resp. kompozícia bez `rc_id` | „povinne každý uzol“ | 400 E000003 | stredná | |
| TC0111 | ENTRY at0120 bez `datumACasStorna` | výskyt vo vnorenom archetype (N18) | 400 E000003 | nízka | |

### Negatívne – hodnota nespĺňa obmedzenie (E000004)

| ID | Scenár | Zdroj | Očakávanie | Priorita | Impl. |
| --- | --- | --- | --- | --- | --- |
| TC0102 | „Dôvod“ (at0074) s kódom, ktorý v číselníku `…10.265` neexistuje | číselník | 400 E000004 | vysoká | ✔ |
| TC0103 | „Dátum vystavenia“ (at0031) v tvare `03.11.2026` | formát dátumu | 400 E000004 | vysoká | ✔ |
| TC0112 | „Diagnóza“ (at0019) s platným kódom, ale `@code_system` iného číselníka (`…10.265`) | číselník | 400 E000004 | vysoká | |
| TC0113 | `identifikaciaZprac` s `@root` iným ako `…40.90` | konštanta | 400 E000004 | vysoká | |
| TC0114 | `rc_id.@root` kompozície iný ako `…40.400` | konštanta | 400 E000004 | stredná | |
| TC0115 | `liecbaVCudzine` = `"ano"` | BOOLEAN | 400 E000004 | stredná | |
| TC0116 | `verziaSluzbyeDpn` = `0` (a `1` ako pozitívna hranica) | INTEGER ≥ 1 (N15) | 400 E000004 / 200 | stredná | |
| TC0117 | `pobytVCasePracovnejNeschopnosti` s `@media_type` = `PDF` | enum (N13) | 400 E000004 | stredná | |
| TC0118 | Čas `25:00`, resp. `10:60` | formát času | 400 E000004 | stredná | |
| TC0119 | `praceneschopnyOd` 2× v at0013 (kardinalita 7 ≤ 12 ostáva splnená) | výskyt 1..1 | 400 E000004 | stredná | |
| TC0120 | ENTRY „Diagnóza“ 2× v `content` | výskyt ENTRY 1..1 | 400 E000004 | stredná | |
| TC0121 | Povinný element s `"@value": ""` | N12 | 400 E000004 | stredná | |
| TC0122 | `value._type` nezodpovedá typu uzla (DATE v at0033) | dátový typ | 400 E000004 | stredná | |
| TC0123 | ENTRY „Výnimka z poistenia“ bez položiek | kardinalita 1..* (N06) | 400 E000004 | nízka | |
| TC0124 | Vychádzka so 4 parts (duplicitný `casOd`) | N05 | 400 E000004 | nízka | |
| TC0125 | Dátum `2026-02-30`; dátum `202611` (`RRRRMM`) | N10 | 400 E000004 | nízka | |
| TC0126 | `compositions` prázdne, resp. s 2 kompozíciami | N20 | 400 E000004 | nízka | |
| TC0127 | Kľúč `praceneschopnyOd` s `archetype_id` `…/at0037` | N22 | 400 E000004 | nízka | |
| TC0128 | Navyše `version_status` v kompozícii | N22 | 400 E000004, prípadne 200 | nízka | |

### Biznis pravidlá (očakávanie neznáme, `Q3`, `Q5`)

| ID | Scenár | Zdroj | Očakávanie | Priorita |
| --- | --- | --- | --- | --- |
| TC0201 | `schopnyPraceOd` pred `praceneschopnyOd` | N23 | pravdepodobne 400 E000004 | nízka, po odpovedi na Q3 vysoká |
| TC0202 | `datumPredpokladanehoUkoncenia` pred `praceneschopnyOd` | N23 | pravdepodobne 400 E000004 | nízka, po odpovedi na Q3 vysoká |
| TC0203 | `casOd` po `casDo` v jednej vychádzke | N23 | pravdepodobne 400 E000004 | nízka |
| TC0204 | `datumVystavenia` v budúcnosti | N23 | neznáme | nízka |
| TC0205 | Zápis s ENTRY „Zmena stavu záznamu“ (storno) | N18 | neznáme | nízka |
| TC0206 | Zápis s `automatickyUkoncenaSystemom` = `true` | N23 | neznáme | nízka |

Prioritu biznisových testov som dala nízku len preto, že nepoznám ešte očakávaný výsledok. Dopad je vysoký: PN s koncom pred začiatkom je chybný úradný doklad. Po odpovedi na `Q3` by TC0201 a TC0202 patrili medzi prvé testy.

### Čo zámerne nenavrhujem

- **Autentifikácia, podpis a obálka správy:** sú mimo rozsahu zadania.
- **Výkon a veľké počty opakovaní** (stovky vychádzok): bez známeho limitu (N07) nie je čo overiť.
- **Duplicitný zápis s rovnakým `rc_id`:** správanie nie je opísané (N21), test by len zafixoval môj odhad.
- **Kombinácie viacerých chýb v jednom requeste:** kým nie je známe poradie vyhodnotenia (`Q2`), výsledok by nebol interpretovateľný.

### Testovacie dáta

- Kódy pre všetky `CODED_VALUE` beriem z Číselníkov zdravotníckej informatiky NCZI podľa OID zo špecifikácie (`…10.25`, `…10.265`, `…10.702`, `…10.686`, `…10.34`, `…10.352`). Názov, verzia (`codingSchemeVersion`) a dátum stiahnutia (2026-11-11) sú v `src/test/resources/codelists.json`. Použité verzie: 25 → 27, 265 → 2, 702 → 2, 686 → 2, 34 → 18, 352 → 1.
- Pre TC0102 je „neexistujúci kód“ `02089917999`. Overila som, že medzi 6 položkami číselníka 265 (verzia 2) nie je.
- Identifikátory (`rc_id`, ZPrac, OÚ PZS, čísla poistenia), telefóny a texty sú fiktívne. `rc_id.@extension` je nové UUID pre každý beh.
