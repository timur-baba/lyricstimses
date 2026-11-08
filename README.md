# TimLyrics (Fabric 1.21.11)

TimSesBot'ta çalan şarkının sözlerini oyunda havada, **herkes için aynı koordinatta** gösterir.
Yalnızca istemci modudur: normal sunucuda, Essential'da ya da tek kişilik dünyada çalışır, sunucuya mod gerekmez.

## Jar'ı derleme (GitHub)
1. Bu klasörü yeni bir GitHub deposuna yükle (`git push`).
2. Depoda **Actions → Jar derle** çalışır (ya da "Run workflow").
3. Bitince çalıştırmanın altındaki **timlyrics-jar** dosyasını indir, zip'ten çıkan `timlyrics-*.jar` dosyasını `mods` klasörüne at.
   Fabric Loader ve Fabric API gerekir (LyricsFlow'da zaten kullanıyorsun).

Yerelde derlemek için: `gradle build` (Java 21) → `build/libs/`.
"Could not resolve" hatası olursa `gradle.properties` içindeki sürümleri https://fabricmc.net/develop adresindeki güncel değerlerle değiştir.

## Kullanım
1. Botun arkadaş paneli adresini al (ücretsiz tünel adresi, `runtime/arkadas-paneli-*.txt` içinde).
2. Oyunda **J** tuşu → menü. Adresi yapıştır, **Kaydet**, sonra **Discord ile eşleştir**.
3. Panoya kopyalanan `!panel KOD` komutunu, ses kanalındayken Discord'da yaz.
4. Sözler otomatik görünür: bakış yönünün sağında/solunda havada süzülür, dönünce ve yürüyünce seni takip eder.
   Herkes kendi ekranında görür; ortak nokta yoktur.
5. Menüden: **Konum** (Sağ / Sol / İki yan / Ortada), boyut, satır sayısı, zaman farkı (±100 ms).
6. Söz çıkmadı ya da yanlış şarkıyı buldu mu? Menüdeki kutuya `şarkı - sanatçı` yaz, **Ara**'ya bas.

## Notlar
- Bot sözleri sırayla Spotify (tanıma) → LRCLIB → NetEase kaynaklarında arar.
- Hiçbir yerde zaman damgalı söz yoksa ama düz söz varsa satırlar şarkı süresine yayılır (tahmini).
- Tuş ataması Ayarlar → Kontroller → TimLyrics bölümünden değişir.
