SektorCore - partia 1 (Paper 1.21.4, Java 21)

BUDOWANIE:
1. Zainstaluj JDK 21 oraz Gradle 8.x (lub otworz folder w IntelliJ i uruchom task "build").
2. W folderze projektu: gradle build
3. Plik .jar znajdziesz w build/libs/ - wrzuc do plugins/ na serwerze Paper 1.21.4.

KOMENDY:
/sethome [nazwa], /home [nazwa], /delhome <nazwa>, /homes
/warp <nazwa>, /warps, /setwarp, /delwarp (admin)
/spawn, /setspawn (admin)
/tpa <gracz>, /tpaccept, /tpdeny
/rtp
/msg <gracz> <tresc>, /r <tresc>
/ac <tresc>  (perm sektor.adminchat)
/helpop <tresc> (odbiorcy: sektor.helpop)
/invsee <gracz> (sektor.admin)
/kod <kod>
/kodadmin add <nazwa> <maxUzyc|-1> <komenda z %player%>
   np. /kodadmin add start 100 give %player% diamond 5
/kodadmin remove <nazwa>, /kodadmin list
/afk  - po 3s teleport na strefe AFK; wpisz ponownie, aby wrocic
/setafk (admin) - ustawia strefe AFK tam gdzie stoisz
/afkadmin add <tekst> | remove <numer> | list | clear (admin)
   - wiadomosci pokazywane graczowi po teleporcie na AFK (obsluguja &kolory i %player%)

PERMISJE: sektor.admin, sektor.adminchat, sektor.helpop,
sektor.homes.<liczba>, sektor.bypass.delay, sektor.bypass.cooldown, sektor.bypass.afk (brak auto-AFK)

BUDOWANIE BEZ INSTALOWANIA NICZEGO (GitHub):
1. Zaloz darmowe konto na github.com i nowe repozytorium.
2. Wrzuc tam zawartosc tego folderu (razem z .github).
3. Wejdz w zakladke Actions -> Build -> po zakonczeniu pobierz "SektorCore-jar".
