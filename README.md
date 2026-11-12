# eDPN – testy služby ZapisPotvrdenieDPN_v4

Malý framework v Gherkine a Jave. Request zostaví z dátovej tabuľky podľa modelu archetypu eDPN.v4, pošle ho na embedded WireMock a overí odpoveď aj request, ktorý WireMock prijal.

| Dokument | Obsah |
| --- | --- |
| [ANALYZA.md](ANALYZA.md) | nejasnosti a predpoklady (N01–N25), otázky pre analytika, návrh testov |
| [ROZHODNUTIA.md](ROZHODNUTIA.md) | kľúčové rozhodnutia frameworku a výber chybových scenárov |
| [SKALOVANIE.md](SKALOVANIE.md) | dopad eDPN.v5, novej služby, rastu a postup pri nestabilnom teste |

## Spustenie

Potrebná je Java 17 alebo novšia. Gradle netreba inštalovať, stačí wrapper.

```bash
./gradlew test
```

Podľa tagu (Gradle odovzdá `-Dcucumber.*` a `-Dedpn.*` do testovacej JVM):

```bash
./gradlew test -Dcucumber.filter.tags="@smoke"
```

```bash
./gradlew test -Dcucumber.filter.tags="@zapisedpn and not @TC0002"
```

Report s requestom, odpoveďou a `rc_id` každého scenára je v `build/reports/cucumber/cucumber.html`.

### Konfigurácia

V [`edpn.properties`](src/test/resources/edpn.properties). Každú hodnotu možno prepísať cez `-D`.

| Kľúč | Predvolená hodnota | Význam |
| --- | --- | --- |
| `edpn.endpoint` | `/edpn/v4/zapis` | cesta služby |
| `edpn.baseUrl` | prázdne = embedded WireMock na náhodnom porte | adresa inej inštancie WireMocku; embedded sa vtedy nespustí a prijaté requesty sa čítajú z jej admin API |
| `edpn.spec` | `spec/edpn-v4.json` | model archetypu |
| `edpn.timeoutSeconds` | `10` | timeout spojenia a odpovede |

## Scenáre

| Tag | Scenár | Súbor |
| --- | --- | --- |
| `@TC0001 @smoke` | minimálne parametre | `ZapisPotvrdenieDPN.feature` |
| `@TC0002` | maximálne parametre | `ZapisPotvrdenieDPN.feature` |
| `@TC0101` | chýba Práceneschopný od → E000003 | `ZapisPotvrdenieDPN_Exception.feature` |
| `@TC0102` | Dôvod mimo číselníka → E000004 | `ZapisPotvrdenieDPN_Exception.feature` |
| `@TC0103` | Dátum vystavenia `03.11.2026` → E000004 | `ZapisPotvrdenieDPN_Exception.feature` |
| `@TC0104` | chýba ENTRY Záznam vytvoril → E000003 | `ZapisPotvrdenieDPN_Exception.feature` |

Všetky nesú aj tag `@zapisedpn`.

## Štruktúra

```
src/test/java/sk/edpn/
  RunCucumberTest.java         JUnit Platform suite
  spec/Spec.java               model archetypu, hľadanie poľa a archetype_id
  spec/SpecNode.java           jeden uzol modelu
  spec/Codelists.java          výťah z číselníkov NCZI
  request/RequestBuilder.java  riadky tabuľky → ehr_extract
  request/ValueFactory.java    tvar "value" podľa dátového typu
  context/ScenarioContext.java rc_id, request, response jedného scenára
  steps/ZapisSteps.java        zostavenie a odoslanie, prílohy do reportu
  steps/OverenieSteps.java     overenie odpovede a prijatého requestu
  support/WireMockSupport.java štart a stop WireMocku
  support/Config.java          edpn.properties + -D
src/test/resources/
  spec/edpn-v4.json            štruktúra eDPN.v4 a vnorených archetypov
  codelists.json               kódy, názvy a verzie z NCZI (stiahnuté 2026-11-11)
  wiremock/mappings/*.json     stuby
  features/*.feature
```

## Čo je hotové a čo chýba

**Hotové:**

- Časť 1 je kompletná.
- V časti 2 je 6 scenárov. Všetky prechádzajú a dajú sa spustiť podľa tagu.
- Overila som, že testy zachytia chybu. Postupne som pokazila stuby a sledovala, či test spadne:

| Zásah | Výsledok |
| --- | --- |
| stub vráti iné `@extension` | padne TC0001 aj TC0002 |
| chýba stub pre at0034 | padne TC0101 (príde 200) |
| preklep v stĺpci `adl` | test zlyhá už pri zostavení requestu |
| builder ignoruje index opakovania | padne TC0002 |
| prázdne bunky sa pošlú s hodnotou | padnú TC0001, TC0101 a TC0104 |

**Chýba alebo je vynechané:**

- **Ďalšie scenáre z ANALYZA.md:** WireMock simuluje len 4 pravidlá a každý ďalší negatívny test potrebuje vlastný stub (ROZHODNUTIA.md, bod 5). Ako ďalšie by som pridala TC0107 a TC0112.
- **Unit testy builderu:** správanie builderu overujú len Cucumber scenáre a pokusy z tabuľky vyššie.
- **Automatická kontrola tvaru celého requestu:** počty uzlov v minime (12) a maxime (32) som skontrolovala ručne.
- **Paralelný beh:** nie je zapnutý. Overenie prijatého requestu je naň pripravené vyhľadávaním podľa `rc_id`.

**Časový rámec:** zadanie uvádza najviac 8 hodín, sprievodný text najviac 24 hodín. Snažila som sa venovať tomu max 8 hodín dokopy.

## Testovacie dáta

- Kódované hodnoty sú reálne položky číselníkov NCZI (`codeValue`), verzie podľa `codingSchemeVersion`.
- Všetky identifikátory, telefóny a texty sú fiktívne. Identifikátory s ľubovoľným OID používajú vetvu `2.999`, ktorá je rezervovaná pre príklady.

## Použitie AI

AI nástroje (Claude) som použila ako podporný nástroj pri analýze špecifikácie, konzultácii návrhu testovacej architektúry, generovaní častí opakujúceho sa kódu a riešení technických problémov pri práci s Cucumber, Jackson a WireMock.
Samostatne som sa zamerala na interpretáciu požiadaviek, identifikáciu nejasností, stanovenie testovacích priorít, výber implementovaných scenárov a rozhodnutia o štruktúre frameworku.
AI-generované návrhy som priebežne kontrolovala voči špecifikácii a overovala spustením testov. Finálne technické rozhodnutia a ich kompromisy sú zdokumentované v ROZHODNUTIA.md.

Cieľom bolo využiť AI na zrýchlenie rutinných činností, nie na nahradenie vlastnej analýzy a rozhodovania.
