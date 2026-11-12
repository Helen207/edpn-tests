# language: sk
@zapisedpn
Požiadavka: ZapisPotvrdenieDPN_v4 – zápis eDPN

  Prázdna bunka v stĺpci hodnota znamená, že sa element do requestu nepošle.
  ENTRY a CLUSTER bez elementov sa nepošlú. Stĺpec adl: ! = povinný, typ, at-kód (overuje sa voči špecifikácii).
  INSTANCE_IDENTIFIER s ľubovoľným OID sa zapisuje ako root:extension, ATTACHMENT ako mediaType:obsah.
  OID 2.999 je rezervovaný pre príklady, všetky identifikátory a telefóny sú fiktívne.

  Osnova Scenára: Zápis eDPN – <Scenar>
    Keď zostavím request ZapisPotvrdenieDPN_v4 s údajmi
      | pole                                         | hodnota              | adl                  |
      | datumVystavenia                              | 2026-11-03           | !DATE [at0031]       |
      | datumZmeny                                   | 2026-11-03           | !DATE [at0032]       |
      | liecbaVCudzine                               | false                | !BOOLEAN [at0033]    |
      | praceneschopnyOd                             | <PraceneschopnyOd>   | !DATE [at0034]       |
      | pobytVCasePracovnejNeschopnosti              | <Pobyt>              | ATTACHMENT [at0035]  |
      | potvrdenieDocasnejPraceneschopnosti.poznamka | <Poznamka>           | TEXT [at0036]        |
      | schopnyPraceOd                               | <SchopnyPraceOd>     | DATE [at0037]        |
      | telefon                                      | <Telefon>            | TEXT [at0038]        |
      | idZaznamuVysetrenia                          | <IdVysetrenia>       | II [at0039]          |
      | datumUmrtia                                  | <DatumUmrtia>        | DATE [at0040]        |
      | automatickyUkoncenaSystemom                  | false                | !BOOLEAN [at0085]    |
      | verziaSluzbyeDpn                             | 4                    | !INTEGER [at0056]    |
      | diagnoza                                     | <Diagnoza>           | !CV [at0019]         |
      | diagnoza.platnostOd                          | <DiagnozaOd>         | DATE [at0020]        |
      | dovod                                        | <Dovod>              | !CV [at0074]         |
      | dovodPracovnejNeschopnosti.platnostOd        | <DovodOd>            | DATE [at0075]        |
      | pozitieAlkoholu                              | 02090417805          | !CV [at0076]         |
      | datumPredpokladanehoUkoncenia                | 2026-11-17           | !DATE [at0088]       |
      | trvaniePracovnejNeschopnosti.poznamka        | <TrvaniePoznamka>    | TEXT [at0089]        |
      | informaciaONasledujucejKontrole              | <Kontrola>           | CV [at0127]          |
      | povolenaVychadzka[1].casOd                   | <V1Od>               | !TIME [at0102]       |
      | povolenaVychadzka[1].casDo                   | <V1Do>               | !TIME [at0103]       |
      | povolenaVychadzka[1].datumOd                 | <V1Datum>            | DATE [at0104]        |
      | povolenaVychadzka[2].casOd                   | <V2Od>               | !TIME [at0102]       |
      | povolenaVychadzka[2].casDo                   | <V2Do>               | !TIME [at0103]       |
      | povolenaVychadzka[2].datumOd                 | <V2Datum>            | DATE [at0104]        |
      | cisloPoistenia[1]                            | <Poistenie1>         | II [at0114]          |
      | cisloPoistenia[2]                            | <Poistenie2>         | II [at0114]          |
      | zaznamVytvoril.identifikaciaOuPzs            | <OuPzs>              | II [at0003]          |
      | zaznamVytvoril.identifikaciaZprac            | A99999901            | !II [at0004]         |
      | zaznamVytvoril.nezmluvnyLekar                | <Nezmluvny>          | BOOLEAN [at0005]     |
      | zaznamVytvoril.specializaciaLekara           | 00000461517          | !CV [at0006]         |
    A odošlem request
    Potom odpoveď má HTTP status 200 a result.code "0"
    A odpoveď obsahuje idPotvrdeniaDpn s @root "1.3.158.00165387.100.40.400" a @extension z rc_id requestu
    A WireMock prijal request, kde uzol [at0034] má hodnotu "<PraceneschopnyOd>"
    A WireMock prijal request, kde uzol [at0019] má hodnotu "<Diagnoza>"
    A WireMock prijal request, kde uzol [at0074] má hodnotu "<Dovod>"
    A WireMock prijal request, kde uzol [at0037] má hodnotu "<SchopnyPraceOd>"
    A WireMock prijal request, kde uzol [at0035] má hodnotu '<Pobyt>'
    A WireMock prijal request, kde uzol [at0102] má hodnoty "<V1Od>, <V2Od>"
    A WireMock prijal request, kde uzol [at0114] má hodnoty "<Poistenie1>, <Poistenie2>"
    A WireMock prijal request, kde uzol [Zdravotnicky_pracovnik_odborneho_utvaru.v2/at0003] má hodnotu "<OuPzs>"
    A WireMock prijal request, kde uzol [Zdravotnicky_pracovnik_odborneho_utvaru.v2/at0004] má hodnotu "A99999901"

    @TC0001 @smoke
    Príklady: Minimálne parametre
      | Scenar              | PraceneschopnyOd | Pobyt | Poznamka | SchopnyPraceOd | Telefon | IdVysetrenia | DatumUmrtia | Diagnoza    | DiagnozaOd | Dovod       | DovodOd | TrvaniePoznamka | Kontrola | V1Od | V1Do | V1Datum | V2Od | V2Do | V2Datum | Poistenie1 | Poistenie2 | OuPzs | Nezmluvny |
      | minimálne parametre | 2026-11-03       |       |          |                |         |              |             | 02089403436 |            | 02089917385 |         |                 |          |      |      |         |      |      |         |            |            |       |           |

    @TC0002
    Príklady: Maximálne parametre
      | Scenar               | PraceneschopnyOd | Pobyt                                       | Poznamka                       | SchopnyPraceOd | Telefon          | IdVysetrenia           | DatumUmrtia | Diagnoza    | DiagnozaOd | Dovod       | DovodOd    | TrvaniePoznamka         | Kontrola    | V1Od  | V1Do  | V1Datum    | V2Od        | V2Do        | V2Datum    | Poistenie1       | Poistenie2       | OuPzs        | Nezmluvny |
      | maximálne parametre  | 20261103         | JSON:{"adresa":"Fiktívna 1, 999 99 Testovo"} | Pacient – „pokoj“ na lôžku & <1 týždeň> | 2026-11-19     | +421 999 000 000 | 2.999.1:VYS-2026-00001 | 2026-11-20  | 02089403519 | 2026-11    | 02089917401 | 2026-11-03 | Kontrola o 14 dní       | 02090388493 | 10:00 | 12:00 | 2026-11-04 | 15:00+01:00 | 17:00+01:00 | 2026-11-04 | 2.999.2:POI-0001 | 2.999.2:POI-0002 | P99999999001 | false     |
