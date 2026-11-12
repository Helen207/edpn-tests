# Škálovanie

## 1. Nová verzia archetypu eDPN.v5

Žiadna Java trieda sa nemení. Zmeny sú v dátach, feature súboroch a stuboch:

| # | Súbor | Zmena |
| --- | --- | --- |
| 1 | nový `spec/edpn-v5.json` (kópia v4) | `archetype` → `…eDPN.v5`; `telefon` `occ` → `1..1`; riadok `datumUmrtia` zmazať; do `trvaniePracovnejNeschopnosti` pridať `miestoLiecby` (CODED_VALUE, nový at-kód, `codeSystem`) |
| 2 | `codelists.json` | pridať číselník pre Miesto liečby: OID, verzia, 1–2 kódy z NCZI |
| 3 | `edpn.properties` | `edpn.spec=spec/edpn-v5.json`, prípadne nový `edpn.endpoint` |
| 4 | `ZapisPotvrdenieDPN.feature` | riadok `telefon`: `adl` → `!TEXT [at0038]`, hodnota povinná aj v minime; zmazať riadok a stĺpec `DatumUmrtia`; pridať riadok a stĺpec `MiestoLiecby` |
| 5 | `ZapisPotvrdenieDPN_Exception.feature` | pridať riadok `telefon`, inak by všetky chybové scenáre padali na chýbajúcom telefóne namiesto testovanej chyby |
| 6–9 | 4 mapovania v `wiremock/mappings` | `…eDPN.v4/atNNNN` → `…eDPN.v5/atNNNN` |
| 10 | nové scenáre | chýba Telefón → E000003; Miesto liečby mimo číselníka → E000004 (+ stuby) |

Spolu 9 súborov na úpravu a 2 nové scenáre.

**Čo framework zachytí:** zmenu existujúceho riadku. Riadok `datumUmrtia` zlyhá na „pole nie je v špecifikácii“, starý `adl` pri telefóne na nezhode povinnosti. Stub s v4 at-kódom vráti 400 E000003 aj pozitívnym scenárom.

**Čo nezachytí:** chýbajúci riadok. `RequestBuilder` zámerne neoveruje povinnosť, takže zabudnutý `telefon` odíde ticho. Pomohla by kontrola povinných polí v builderi, vypnutá v `*_Exception.feature`.

**Ako počet miest zmenšiť:**

- **Stuby generovať z modelu** v `WireMockSupport` cez `Spec.archetypeIdOf("at0034")`. Riadky 6–9 odpadnú.
- **Verziu vybrať v scenári** (`… podľa archetypu eDPN.v5`), aby v4 a v5 bežali počas prechodu vedľa seba.
- **Feature súbory nezmenšujem:** zadanie chce vstupy viditeľné v Gherkine, preto nový povinný element znamená úpravu každej tabuľky.

## 2. Nová služba ZapisLekarskejSpravy_v1

| Znova použijem bez zmeny | Upravím | Napíšem nanovo |
| --- | --- | --- |
| `Spec`, `SpecNode`, `Codelists`, `RequestBuilder`, `ScenarioContext`, `WireMockSupport`, `Config`, kroky `uzol [..] má hodnotu/hodnoty`, prílohy do reportu | pozri nižšie | `spec/lekarskaSprava-v1.json`, feature súbory, stuby, položky v `codelists.json` |

**Čo upravím:** dnes je služba eDPN natvrdo v troch miestach.

1. **Text kroku** `zostavím request ZapisPotvrdenieDPN_v4` v `ZapisSteps` sa zmení na `zostavím request {word}`. Názov služby z kroku vyberie `spec` aj `endpoint` z konfigurácie, napr. `services.ZapisLekarskejSpravy_v1.endpoint`. Tým sa `Spec` presunie z injekcie do kontextu scenára.
2. **Konštanta `REQUEST_RC_ID`** v `OverenieSteps` obsahuje `.edpn.`. Nahradí ju `spec.compositionKey()`.
3. **Cesta k ID v odpovedi** (`$.data.zapisPotvrdenieDpn.idPotvrdeniaDpn`) sa presunie do konfigurácie služby, prípadne do parametra kroku.

**Ďalšie rozšírenia:**

- Ak archetyp používa dátové typy, ktoré eDPN nemá (napr. `PHYSICAL_QUANTITY`, `ORDINAL`), pribudne vetva v `ValueFactory.of` a v `OverenieSteps.comparableValue`.
- Ak má služba inú obálku ako `ehr_extract.compositions[0]`, treba zovšeobecniť `RequestBuilder.build`.

## 3. Rast: 150 služieb, 2 000 scenárov, hodiny sériovo

V tomto poradí:

1. **Nezávislé scenáre a paralelný beh.** Reťazec Zápis → Storno prerobím na prípravný krok `Za predpokladu, že existuje zapísané eDPN`, ktorý zapíše vlastný záznam cez `RequestBuilder` a jeho `rc_id` uloží do `ScenarioContext`. Potom stačí `cucumber.execution.parallel.enabled=true` v `junit-platform.properties`. Framework je na to pripravený: kontext je per scenár a `OverenieSteps` hľadá prijatý request podľa `rc_id`. Cena: viac volaní.
2. **Model generovaný z ADL.** Ručný prepis 150 služieb do `spec/*.json` sa nedá udržať a framework mu slepo verí. Generátor v CI zastaví build pri rozdiele voči commitnutému súboru a vyrobí aj stuby pre povinné uzly.
3. **Tagy a sharding v CI.** Tag služby (`@zapisedpn`) a úrovne (`@smoke`, `@regression`). Pri commite `@smoke` všetkého a celá sada zmenených služieb, v noci celá sada rozdelená podľa služby na viac agentov. Bez bodu 1 to nejde.

Iný DI kontajner ani vlastný reportovací server neriešim, nie sú úzkym hrdlom.

## 4. Nestabilný test: v CI padá zhruba 1× z 10, lokálne prechádza

1. **Neskryť.** Tag `@flaky` s odkazom na úlohu, scenár beží a reportuje sa ďalej. Rerun slúži len na rozlíšenie „nestabilný / rozbitý“.
2. **Porovnať zlyhania** z histórie behov: rovnaký krok a hláška? Súvis s agentom, časom alebo súbežnými jobmi?
3. **Reprodukovať v CI:** 50× na tom istom agentovi s rovnakou paralelizáciou a JVM.
4. **Hypotézy pre tento framework:** časová zóna (CI v UTC, časy s `+01:00`, „dnes“ okolo polnoci), závislosť od dát iného scenára, `edpn.timeoutSeconds=10` na pomalom agentovi, výber requestu inak ako podľa `rc_id`.
5. **Opraviť príčinu** (napr. pevná `-Duser.timezone`), potvrdiť 50 behmi bez zlyhania, až potom odobrať `@flaky`.

**Report:** dnes `ZapisSteps.prilozKReportu` prikladá `rc_id`, request a odpoveď. Doplnila by som trvanie krokov a HTTP volaní, vlákno, agent a zónu JVM, zachytený stub (`ServeEvent.getStubMapping()`) a históriu výsledkov z archivovaných `cucumber.json`, prípadne Allure.
