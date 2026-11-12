# language: sk
@zapisedpn
Požiadavka: ZapisPotvrdenieDPN_v4 – chybové scenáre

  Každý scenár vychádza z minimálneho requestu a porušuje práve jedno pravidlo.
  Posledný krok overuje, že framework chybu naozaj vyrobil v správnom uzle:
  prázdna očakávaná hodnota znamená, že uzol v requeste chýba.

  Osnova Scenára: Zápis eDPN – <Scenar>
    Keď zostavím request ZapisPotvrdenieDPN_v4 s údajmi
      | pole                               | hodnota            | adl               |
      | datumVystavenia                    | <DatumVystavenia>  | !DATE [at0031]    |
      | datumZmeny                         | 2026-11-03         | !DATE [at0032]    |
      | liecbaVCudzine                     | false              | !BOOLEAN [at0033] |
      | praceneschopnyOd                   | <PraceneschopnyOd> | !DATE [at0034]    |
      | automatickyUkoncenaSystemom        | false              | !BOOLEAN [at0085] |
      | verziaSluzbyeDpn                   | 4                  | !INTEGER [at0056] |
      | diagnoza                           | 02089403436        | !CV [at0019]      |
      | dovod                              | <Dovod>            | !CV [at0074]      |
      | pozitieAlkoholu                    | 02090417805        | !CV [at0076]      |
      | datumPredpokladanehoUkoncenia      | 2026-11-17         | !DATE [at0088]    |
      | zaznamVytvoril.identifikaciaZprac  | <Zprac>            | !II [at0004]      |
      | zaznamVytvoril.specializaciaLekara | <Specializacia>    | !CV [at0006]      |
    A odošlem request
    Potom odpoveď má HTTP status <Status> a result.code "<Kod>"
    A WireMock prijal request, kde uzol [<Uzol>] má hodnotu "<Hodnota>"

    @TC0101
    Príklady: Chýba povinný element
      | Scenar                      | DatumVystavenia | PraceneschopnyOd | Dovod       | Zprac     | Specializacia | Status | Kod     | Uzol   | Hodnota |
      | chýba Práceneschopný od     | 2026-11-03      |                  | 02089917385 | A99999901 | 00000461517   | 400    | E000003 | at0034 |         |

    @TC0102
    Príklady: Kód mimo číselníka
      | Scenar                      | DatumVystavenia | PraceneschopnyOd | Dovod       | Zprac     | Specializacia | Status | Kod     | Uzol   | Hodnota     |
      | Dôvod mimo číselníka 265    | 2026-11-03      | 2026-11-03       | 02089917999 | A99999901 | 00000461517   | 400    | E000004 | at0074 | 02089917999 |

    @TC0103
    Príklady: Nesprávny formát dátumu
      | Scenar                      | DatumVystavenia | PraceneschopnyOd | Dovod       | Zprac     | Specializacia | Status | Kod     | Uzol   | Hodnota    |
      | Dátum vystavenia DD.MM.RRRR | 03.11.2026      | 2026-11-03       | 02089917385 | A99999901 | 00000461517   | 400    | E000004 | at0031 | 03.11.2026 |

    @TC0104
    Príklady: Chýba povinná ENTRY so slotom
      | Scenar                      | DatumVystavenia | PraceneschopnyOd | Dovod       | Zprac | Specializacia | Status | Kod     | Uzol   | Hodnota |
      | chýba Záznam vytvoril       | 2026-11-03      | 2026-11-03       | 02089917385 |       |               | 400    | E000003 | at0133 |         |
