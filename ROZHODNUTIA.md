# Rozhodnutia – framework pre ZapisPotvrdenieDPN_v4

Odkazy `N..` a `TC..` smerujú do [ANALYZA.md](ANALYZA.md).

## 1. Štruktúra eDPN je dáta, nie kód

**Možnosti**

- at-kódy a tvar JSON priamo v krokoch alebo v Java konštantách,
- model ako Java triedy (enum, records),
- model v dátovom súbore,
- parsovanie ADL.

**Voľba:** [`spec/edpn-v4.json`](src/test/resources/spec/edpn-v4.json). Súbor je strom uzlov a pri každom uzle uvádza kľúč, RM typ, at-kód, výskyt, dátový typ, číselník a pevný `@root`. Vnorené archetypy sú v `slots`. [`Spec`](src/test/java/sk/edpn/spec/Spec.java) ho načíta a sloty rozbalí, takže každý uzol pozná svoj archetyp. Builder, overovacie kroky ani feature súbory nepoznajú tvar JSON ani at-kódy natvrdo.

**Prečo:**

- Súbor zodpovedá tabuľkám zo špecifikácie riadok po riadku, takže sa dá skontrolovať pohľadom.
- Pri `eDPN.v5` sa mení hlavne tento súbor.
- Na čítanie stačí Jackson, nepotrebujem novú závislosť.
- Parsovanie ADL by bolo pre takúto jednoduchú špecifikáciu zbytočne náročné a nákladné.

**Čo strácam:** súbor je ručný prepis z ADL a jeho chybu kompilátor nezachytí. Čiastočne to vyvažuje stĺpec `adl` v Gherkine, ktorý sa so súborom porovnáva (rozhodnutie 2). Voľba prestane stačiť pri desiatkach archetypov alebo pri častých zmenách verzií. Vtedy treba `*.json` generovať priamo z ADL.

## 2. Tvar dátovej tabuľky: riadok = element, prázdna bunka = element sa nepošle

**Možnosti**

- jeden stĺpec v `Príklady` na každé pole a JSON šablóna,
- základný JSON request + prepisy z tabuľky,
- vertikálna tabuľka `pole | hodnota | adl`.

**Voľba:** vertikálna tabuľka.

- **`pole`** je koniec cesty v strome. Stačí `praceneschopnyOd`. Pri nejednoznačnom kľúči (N02) treba napísať aj rodiča, napr. `diagnoza.platnostOd`. Opakovanie sa píše s indexom: `povolenaVychadzka[2].casOd`, `cisloPoistenia[2]`.
- **Prázdna hodnota:** element sa nepošle a ENTRY či CLUSTER bez elementov tiež nie. Minimum, maximum aj chýbajúci povinný uzol sú preto tá istá tabuľka s inými `Príklady`.
- **`adl`** (`!DATE [at0034]`) je pre čitateľa, ale [`RequestBuilder`](src/test/java/sk/edpn/request/RequestBuilder.java) ho porovná so špecifikáciou a pri nezhode okamžite zlyhá. Preklep v at-kóde, type alebo povinnosti sa tak prejaví pri zostavení requestu, nie až zle zostaveným requestom.
- **Builder nevaliduje výskyty ani kardinality.** Inak by negatívne testy nevedeli poslať chybný request.

**Čo strácam:**

- `Príklady` v pozitívnom feature majú 24 stĺpcov, lebo každé voliteľné pole musí byť viditeľné.
- Hodnoty niektorých typov majú vlastný zápis: `root:extension` pri INSTANCE_IDENTIFIER s ľubovoľným OID, `mediaType:obsah` pri ATTACHMENT. Treba ho poznať.
- Prázdny reťazec v povinnom elemente (N12, TC0121) sa týmto tvarom poslať nedá, lebo Cucumber dáva prázdnu bunku ako `null`. Keby bol potrebný, pridal by som explicitný token, napr. `<prázdne>`.

## 3. Kontext scenára cez PicoContainer, WireMock jeden na beh

**Možnosti**

- statické polia,
- `ThreadLocal`,
- DI kontajner Cucumbera (PicoContainer, Spring).

**Voľba:** `cucumber-picocontainer`. [`ScenarioContext`](src/test/java/sk/edpn/context/ScenarioContext.java) obsahuje `rc_id` (nové UUID pri každom vytvorení), telo requestu a odpoveď. Pico ho vytvorí pre každý scenár nanovo a odovzdá ho obom triedam krokov cez konštruktor. Rovnako sa vytvárajú `Spec`, `Codelists` a `RequestBuilder`.

WireMock je jediný statický objekt ([`WireMockSupport`](src/test/java/sk/edpn/support/WireMockSupport.java)). Je to zámer, lebo patrí celému behu: štartuje v `@BeforeAll` na náhodnom porte a končí v `@AfterAll`.

**Čo strácam:** Pico vyberá konštruktor sám. Verejný konštruktor s parametrom typu `String` ho pomýlil, preto je `Spec(String)` súkromný. Medzi scenármi sa stav zdieľať nedá. To je pri izolovaných testoch výhoda, pri reťazení Zápis → Storno by to bolo obmedzenie (pozri SKALOVANIE.md).

## 4. Čo presne overujem v requeste prijatom WireMockom

**Možnosti**

- posledný prijatý request,
- vyhľadanie podľa JSON kľúča,
- porovnanie celého JSON so vzorovým súborom,
- vyhľadanie podľa `archetype_id`.

**Voľba:** [`OverenieSteps`](src/test/java/sk/edpn/steps/OverenieSteps.java) nájde medzi requestmi na nakonfigurovaný endpoint práve ten, ktorého `rc_id.@extension` zodpovedá scenáru. Nájsť sa musí presne jeden. V ňom vyhľadá uzly JSONPath filtrom podľa plného `archetype_id`:

- `[at0034]` znamená archetyp eDPN.v4,
- uzol vnoreného archetypu sa píše s názvom archetypu, napr. `[Zdravotnicky_pracovnik_odborneho_utvaru.v2/at0003]`, kvôli kolízii at-kódov (N01).

Hodnota sa porovnáva v tom istom zápise, aký je v tabuľke. Prázdne očakávanie znamená, že uzol musí chýbať. Zoznam hodnôt (`má hodnoty "10:00, 15:00+01:00"`) overuje aj počet a poradie opakovaní.

**Prečo:**

- Vyhľadanie podľa `rc_id` funguje aj pri paralelnom behu, kde by „posledný request“ bol náhodný.
- Plné `archetype_id` jednoznačne určuje druh uzla aj naprieč vnorenými archetypmi (N01, N02). Neurčuje konkrétny výskyt: dve vychádzky majú rovnaké `archetype_id`. Výskyty opakovaného uzla preto rozlišuje poradie v kroku `má hodnoty`.
- Vzorový súbor by padal pri každej zmene poradia alebo pridaní uzla. Bol by to detektor zmien, nie test.
- Pri chybových scenároch krok overuje, že framework chybu naozaj vyrobil. Bez toho by negatívny test prešiel aj vtedy, keby stub odpovedal 400 z iného dôvodu.

**Čo strácam:** neoverujem celý tvar requestu, len vybrané uzly. Zle zaradený uzol so správnym `archetype_id` (napr. element v nesprávnej ENTRY) by krok nezachytil. Počty uzlov v minime (12) a maxime (32) som skontrolovala ručne v reporte, automatický test na to nie je.

Pri `edpn.baseUrl` sa embedded WireMock nespustí a [`WireMockSupport.receivedRequests`](src/test/java/sk/edpn/support/WireMockSupport.java) číta prijaté requesty z admin API WireMocku na tej adrese (napr. zdieľanej inštancie v CI). Služba bez admin API, napr. reálna služba, prijaté requesty neukáže. Krok vtedy zlyhá s hláškou, že request nemožno overiť, a nepredstiera kontrolu porovnaním odoslaného tela so sebou samým.

## 5. WireMock stuby: málo pravidiel, priorita, ľubovoľná URL

**Možnosti**

- stuby registrované z Java kódu pre každý scenár,
- WireMock s vlastným rozšírením, ktoré validuje podľa špecifikácie,
- statické JSON mapovania s prioritami.

**Voľba:** statické mapovania v [`wiremock/mappings`](src/test/resources/wiremock/mappings):

| Priorita | Mapovanie | Podmienka | Odpoveď |
| --- | --- | --- | --- |
| 1 | `01-chyba-praceneschopny-od.json` | chýba uzol `…/at0034` | 400 E000003 |
| 1 | `02-chyba-zaznam-vytvoril.json` | chýba uzol `…/at0133` | 400 E000003 |
| 2 | `03-dovod-mimo-ciselnika.json` | `@code` pri at0074 nie je medzi 6 kódmi číselníka 265 | 400 E000004 |
| 2 | `04-zly-format-datumu-vystavenia.json` | `@value` pri at0031 nespĺňa žiadny povolený formát dátumu | 400 E000004 |
| 10 | `99-uspech.json` | čokoľvek iné | 200, `@extension` cez response templating z `rc_id` requestu |

Stuby prijímajú POST na ľubovoľnú cestu. Endpoint je tak naozaj len v konfigurácii (`edpn.endpoint`). To, že request prišiel na správnu cestu, overuje test pri vyhľadávaní prijatého requestu.

**Prečo:** WireMock zastupuje službu, ktorú nemám. Nemá z neho vzniknúť druhá implementácia validácie, ktorú by bolo treba samostatne testovať.

**Čo strácam:**

- Negatívne testy tu neoverujú validáciu služby. Overujú, že framework vyrobí chybný request v správnom uzle a správne vyhodnotí chybovú odpoveď. Validáciu by overili až rovnaké scenáre proti reálnej službe. Tam platia kontroly odpovede, kroky „WireMock prijal request…“ vyžadujú WireMock (bod 4). Autentifikácia, podpis a obálka správy sú mimo rozsahu zadania, takže proti reálnemu prostrediu by bolo treba doplniť aj tie.
- Každý ďalší negatívny scenár potrebuje vlastný stub.
- Pri zmene archetypu treba upraviť aj `archetype_id` v stuboch, sú v nich natvrdo.

## Výber chybových scenárov

Povinný je TC0101 (chýba at0034). Ďalšie tri som vybrala tak, aby každý pokryl inú triedu validácie, mal vysokú prioritu v ANALYZA.md a otestoval inú vlastnosť frameworku:

| Test | Trieda | Prečo tento | Čo overí na frameworku |
| --- | --- | --- | --- |
| TC0102 – „Dôvod“ mimo číselníka 265 | číselník | najčastejšia chyba integrácie a jediný typ obmedzenia, ktorý vyžaduje reálne dáta NCZI | builder pošle kód, ktorý v `codelists.json` nie je (bez `@display_name`) |
| TC0103 – „Dátum vystavenia“ `03.11.2026` | formát | lokálny formát dátumu je typická chyba ambulantného softvéru | hodnota sa pošle bez úprav, builder ju „neopraví“ |
| TC0104 – chýba ENTRY „Záznam vytvoril“ | povinnosť ENTRY so slotom | iná úroveň ako TC0101 (celá ENTRY s vnoreným archetypom, nie element) | vynechanie celého podstromu, keď nemá žiadny element |

**Prečo nie iné:**

- TC0105 (chýba Diagnóza) by testoval to isté ako TC0104.
- TC0107 (chýba element vo vnorenom archetype) je silný kandidát na ďalší test, rovnako TC0112 (správny kód, zlý číselník).
- TC0108 a TC0109 som vynechala zámerne: porušujú výskyt aj kardinalitu naraz a očakávaný kód nepoznám (N19, Q2).
- Biznis pravidlá TC02xx nemajú známy očakávaný výsledok.

## Ako som naložila s nejasnosťami

| Nejasnosť | Kde je predpoklad v kóde |
| --- | --- |
| N01, N02 – kolízia at-kódov a kľúčov | `Spec.archetypeIdOf`, `Spec.findElement` (nejednoznačná cesta zlyhá s ponukou plných ciest) |
| N03, N04 – `archetype_id` slotu | `edpn-v4.json`: slot CLUSTER má at-kód rodiča, jeho `parts` majú archetyp slotu |
| N08 – maximum | `TC0002` posiela všetkých 12 elementov at0013 vrátane `datumUmrtia`, bez at0120 |
| N11 – `@value` ako reťazec | `ValueFactory` (`default` vetva) |
| N16 – čo sa overuje pri CODED_VALUE | `codelists.json`: verzia a názov na jednom mieste, stub 03 porovnáva len `@code` |
| N18 – storno pri zápise | at0120 je v špecifikácii, ale žiadny scenár ho neposiela |
| N19 – poradie chýb | každý negatívny scenár porušuje práve jedno pravidlo |

Pri sťahovaní číselníkov pribudla ďalšia nejasnosť. NCZI má pri každej položke `codeValue` (napr. `02089403436`) a `Skratka` (napr. `J069`). Do `@code` posielam `codeValue`, lebo je to identifikátor položky v číselníku a skratka slúži na zobrazenie. Doplnila som to do ANALYZA.md ako N24.

## Použité technológie

- **Java 17:**
- **Gradle** (Kotlin DSL, wrapper 8.14.3)
- **Java `HttpClient` namiesto RestAssured:** na jeden POST stačí štandardná knižnica.
- **JSONPath:** Jayway `json-path` - rovnaký jazyk aký používajú WireMock matchery.
