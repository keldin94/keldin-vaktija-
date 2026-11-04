# Nur Vaktija — digitalna vaktija za džamije

Odvojeni projekat od `keldin-iptv`. Moderni namaski TV ekran s punim prikazom i podrškom za Android/Google TV.

**Pregled u pregledniku:** https://keldin-vaktija.vercel.app/

## Prva testna verzija
- 16:9 moderni tamno-tirkizni i zlatni dizajn, prilagodljiv za manji ekran.
- Uživo sat, naredni namaz i odbrojavanje, svih šest vaktova.
- Grad i ime džamije iz postavki (čuvaju se lokalno).
- Podaci preko vaktija.ba API-ja, korištenjem /api/times serverless posrednika.
- Android TV aplikacija i GitHub Actions gradnja: vidi `android/` (nakon dodavanja).
- Nije službena vaktija Islamske zajednice. Prije upotrebe u džamiji imam treba potvrditi vremena namaza.

## Status
Web prototip. Podatke treba testirati za odabrani grad i provjeriti dostupnost vaktija.ba servisa. Instalacijski APK, kiosk podešavanje i ažuriranja su posebni koraci i moraju se provjeriti na konkretnom Android TV-u.
