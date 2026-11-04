# Nur Vaktija — Android / Google TV

## Testni APK
Na GitHub kartici **Actions → Nur Vaktija — Android TV APK** otvori zadnje uspješno izvršavanje i preuzmi `Nur-Vaktija-Preview-APK`. U ZIP-u je `app-debug.apk`. Preview je potpisan privremenim Android debug certifikatom i sljedeća GitHub Actions gradnja ne mora prihvatiti ažuriranje preko postojeće instalacije. Za trajna ažuriranja koristi release APK s vlastitim nepromjenjivim keystoreom.

## Direktni Downloader link (nakon prve release verzije)
https://github.com/keldin94/keldin-vaktija-/releases/latest/download/Nur-Vaktija.apk

Instalacija: Downloader → unijeti URL → dozvoliti instaliranje nepoznatih aplikacija → Install → Launch. Prvo pokretanje zahtijeva internet.

## Ažuriranja
Web-dizajn vaktije dolazi sa https://keldin-vaktija.vercel.app/, pa izmjene stranice ne zahtijevaju novo instaliranje APK-a. Nativna verzija se provjerava iz GitHub Releases, a instalacija se nudi uz standardnu Android potvrdu. Ne podržavamo tihu instalaciju APK-a bez dozvole korisnika.

## Potpisivanje release verzije (obavezno za stabilne nadogradnje)
Lokalno kreiraj **jedan** privatni keystore i sačuvaj sigurnosnu kopiju. Ne stavljaj ga u javni repozitorij.

```bash
keytool -genkeypair -v -keystore nur-vaktija.jks -alias nurvaktija -keyalg RSA -keysize 3072 -validity 12000
base64 -w 0 nur-vaktija.jks
```

Postavi ove GitHub **Actions → Settings → Secrets and variables → Actions** repository secrets:
- `VAKTIJA_KEYSTORE_BASE64` = cjelokupni base64 string
- `VAKTIJA_STORE_PASSWORD` = lozinka keystorea
- `VAKTIJA_KEY_ALIAS` = `nurvaktija`
- `VAKTIJA_KEY_PASSWORD` = lozinka ključa

Zatim ažuriraj `versionCode` i `versionName` u `android/app/build.gradle`, napravi tag `v0.1.0` (za prvu verziju) i GitHub Actions će izgraditi i objaviti potpisani release. Svaka naredna verzija mora koristiti **isti** keystore i veći `versionCode`.

## TV pokretanje
Aplikacija ima Android TV launcher i HOME intent filter, te `FLAG_KEEP_SCREEN_ON` i immersive fullscreen dok radi. Neki televizori ne dopuštaju podešavanje alternativnog HOME launchera. Na takvom modelu automatsko pokretanje nakon ručnog uključivanja mora se konfigurirati individualno ili zasebnim kiosk launcherom. Vaktija ne može sama nametnuti sistemsko auto-pokretanje na svim Android TV uređajima.

## Vaktija i odgovornost
Ekran koristi javni API vaktija.ba. Prije javnog korištenja treba provjeriti tačnost vaktova, lokalnu vremensku zonu i dogovor džemata o početku džematskih namaza. Kad podaci nisu dostupni, ekran prikazuje upozorenje; ne smije prikazivati izmišljena vremena.
