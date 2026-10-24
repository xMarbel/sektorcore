package pl.sektor.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Commands implements CommandExecutor {

    private final SektorCore pl;

    public Commands(SektorCore pl) { this.pl = pl; }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("sektor.admin")) return true;
        pl.msg(s, "&cBrak uprawnien.");
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String n = cmd.getName().toLowerCase();
        if (n.equals("kodadmin")) return kodAdmin(sender, args);
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy.");
            return true;
        }
        String uid = p.getUniqueId().toString();

        switch (n) {
            // ---------- HOME ----------
            case "sethome" -> {
                String name = args.length > 0 ? args[0].toLowerCase() : "home";
                ConfigurationSection s = pl.data.getConfigurationSection("homes." + uid);
                int count = s == null ? 0 : s.getKeys(false).size();
                boolean exists = s != null && s.contains(name);
                int max = pl.maxHomes(p);
                if (!exists && count >= max) {
                    pl.msg(p, "&cOsiagnales limit domow (" + max + ").");
                    return true;
                }
                pl.data.set("homes." + uid + "." + name, p.getLocation());
                pl.saveData();
                pl.msg(p, "&aUstawiono dom &e" + name + "&a.");
            }
            case "home" -> {
                String name = args.length > 0 ? args[0].toLowerCase() : "home";
                Location l = pl.data.getLocation("homes." + uid + "." + name);
                if (l == null) { pl.msg(p, "&cNie masz domu o nazwie &e" + name + "&c."); return true; }
                pl.teleport(p, l);
            }
            case "delhome" -> {
                if (args.length < 1) { pl.msg(p, "&cUzycie: /delhome <nazwa>"); return true; }
                String name = args[0].toLowerCase();
                if (pl.data.get("homes." + uid + "." + name) == null) { pl.msg(p, "&cNie ma takiego domu."); return true; }
                pl.data.set("homes." + uid + "." + name, null);
                pl.saveData();
                pl.msg(p, "&aUsunieto dom &e" + name + "&a.");
            }
            case "homes" -> {
                ConfigurationSection s = pl.data.getConfigurationSection("homes." + uid);
                if (s == null || s.getKeys(false).isEmpty()) { pl.msg(p, "&7Nie masz zadnych domow."); return true; }
                pl.msg(p, "&6Twoje domy (&e" + s.getKeys(false).size() + "&6/&e" + pl.maxHomes(p) + "&6): &f" + String.join("&7, &f", s.getKeys(false)));
            }

            // ---------- WARP ----------
            case "warp" -> {
                if (args.length < 1) { listWarps(p); return true; }
                Location l = pl.data.getLocation("warps." + args[0].toLowerCase());
                if (l == null) { pl.msg(p, "&cNie ma takiego warpa."); return true; }
                pl.teleport(p, l);
            }
            case "warps" -> listWarps(p);
            case "setwarp" -> {
                if (!admin(p)) return true;
                if (args.length < 1) { pl.msg(p, "&cUzycie: /setwarp <nazwa>"); return true; }
                pl.data.set("warps." + args[0].toLowerCase(), p.getLocation());
                pl.saveData();
                pl.msg(p, "&aUstawiono warp &e" + args[0] + "&a.");
            }
            case "delwarp" -> {
                if (!admin(p)) return true;
                if (args.length < 1) { pl.msg(p, "&cUzycie: /delwarp <nazwa>"); return true; }
                pl.data.set("warps." + args[0].toLowerCase(), null);
                pl.saveData();
                pl.msg(p, "&aUsunieto warp &e" + args[0] + "&a.");
            }

            // ---------- SPAWN ----------
            case "spawn" -> {
                Location l = pl.data.getLocation("spawn");
                if (l == null) l = p.getWorld().getSpawnLocation();
                pl.teleport(p, l);
            }
            case "setspawn" -> {
                if (!admin(p)) return true;
                pl.data.set("spawn", p.getLocation());
                pl.saveData();
                pl.msg(p, "&aUstawiono spawn.");
            }

            // ---------- TPA ----------
            case "tpa" -> {
                if (args.length < 1) { pl.msg(p, "&cUzycie: /tpa <gracz>"); return true; }
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) { pl.msg(p, "&cGracz nie jest online."); return true; }
                if (t.equals(p)) { pl.msg(p, "&cNie mozesz teleportowac sie do siebie."); return true; }
                pl.tpaRequests.put(t.getUniqueId(), p.getUniqueId());
                pl.tpaTime.put(t.getUniqueId(), System.currentTimeMillis());
                pl.msg(p, "&aWyslano prosbe o teleport do &e" + t.getName() + "&a.");
                pl.msg(t, "&e" + p.getName() + " &7chce sie do Ciebie teleportowac. &a/tpaccept &7lub &c/tpdeny &7(60s)");
            }
            case "tpaccept" -> {
                Player req = takeRequest(p);
                if (req == null) { pl.msg(p, "&cBrak aktywnych prosb o teleport."); return true; }
                pl.msg(p, "&aZaakceptowano prosbe.");
                pl.msg(req, "&a" + p.getName() + " zaakceptowal prosbe.");
                pl.teleport(req, p.getLocation());
            }
            case "tpdeny" -> {
                Player req = takeRequest(p);
                if (req == null) { pl.msg(p, "&cBrak aktywnych prosb o teleport."); return true; }
                pl.msg(p, "&7Odrzucono prosbe.");
                pl.msg(req, "&c" + p.getName() + " odrzucil prosbe.");
            }

            // ---------- RTP ----------
            case "rtp" -> randomTp(p);

            // ---------- CHAT ----------
            case "msg" -> {
                if (args.length < 2) { pl.msg(p, "&cUzycie: /msg <gracz> <tresc>"); return true; }
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) { pl.msg(p, "&cGracz nie jest online."); return true; }
                sendPm(p, t, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            }
            case "r" -> {
                if (args.length < 1) { pl.msg(p, "&cUzycie: /r <tresc>"); return true; }
                UUID last = pl.lastMsg.get(p.getUniqueId());
                Player t = last == null ? null : Bukkit.getPlayer(last);
                if (t == null) { pl.msg(p, "&cNie masz komu odpowiedziec."); return true; }
                sendPm(p, t, String.join(" ", args));
            }
            case "ac" -> {
                if (!p.hasPermission("sektor.adminchat")) { pl.msg(p, "&cBrak uprawnien."); return true; }
                if (args.length < 1) { pl.msg(p, "&cUzycie: /ac <tresc>"); return true; }
                String m = "&4[AdminChat] &c" + p.getName() + "&7: &f" + String.join(" ", args);
                for (Player o : Bukkit.getOnlinePlayers())
                    if (o.hasPermission("sektor.adminchat")) pl.msg(o, m);
            }
            case "helpop" -> {
                if (args.length < 1) { pl.msg(p, "&cUzycie: /helpop <tresc>"); return true; }
                String m = "&6[HelpOp] &e" + p.getName() + "&7: &f" + String.join(" ", args);
                int sent = 0;
                for (Player o : Bukkit.getOnlinePlayers())
                    if (o.hasPermission("sektor.helpop")) { pl.msg(o, m); sent++; }
                pl.msg(p, sent > 0 ? "&aWyslano zgloszenie do administracji." : "&cAktualnie nie ma administracji online.");
            }

            // ---------- ADMIN ----------
            case "invsee" -> {
                if (!admin(p)) return true;
                if (args.length < 1) { pl.msg(p, "&cUzycie: /invsee <gracz>"); return true; }
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) { pl.msg(p, "&cGracz nie jest online."); return true; }
                p.openInventory(t.getInventory());
            }

            // ---------- KODY ----------
            case "kod" -> redeem(p, args);

            // ---------- AFK ----------
            case "afk" -> {
                Location zone = pl.data.getLocation("afk");
                if (zone == null) { pl.msg(p, "&cStrefa AFK nie zostala jeszcze ustawiona."); return true; }
                int delay = pl.getConfig().getInt("afk.delay", 3);
                UUID id = p.getUniqueId();
                if (pl.afk.contains(id)) {
                    // powrot ze strefy AFK
                    Location back = pl.afkReturn.get(id);
                    if (back == null) back = pl.data.getLocation("spawn");
                    if (back == null) back = p.getWorld().getSpawnLocation();
                    pl.teleport(p, back, delay, () -> {
                        pl.afk.remove(id);
                        pl.afkReturn.remove(id);
                        pl.lastActive.put(id, System.currentTimeMillis());
                        pl.msg(p, "&aWrociles ze strefy AFK.");
                    });
                } else {
                    Location from = p.getLocation();
                    pl.msg(p, "&7Teleportacja na strefe AFK za &e" + delay + "s&7...");
                    pl.teleport(p, zone, delay, () -> {
                        pl.afkReturn.put(id, from);
                        pl.afk.add(id);
                        pl.sendAfkMessages(p);
                    });
                }
            }
            case "setafk" -> {
                if (!admin(p)) return true;
                pl.data.set("afk", p.getLocation());
                pl.saveData();
                pl.msg(p, "&aUstawiono strefe AFK w Twojej lokalizacji.");
            }
            case "afkadmin" -> {
                if (!admin(p)) return true;
                List<String> list = pl.data.getStringList("afk-messages");
                String sub = args.length > 0 ? args[0].toLowerCase() : "";
                switch (sub) {
                    case "add" -> {
                        if (args.length < 2) { pl.msg(p, "&cUzycie: /afkadmin add <wiadomosc> (kolory: &a, %player%)"); return true; }
                        list.add(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                        pl.data.set("afk-messages", list);
                        pl.saveData();
                        pl.msg(p, "&aDodano wiadomosc nr &e" + list.size() + "&a.");
                    }
                    case "remove" -> {
                        try {
                            int i = Integer.parseInt(args[1]);
                            if (i < 1 || i > list.size()) throw new NumberFormatException();
                            list.remove(i - 1);
                            pl.data.set("afk-messages", list);
                            pl.saveData();
                            pl.msg(p, "&aUsunieto wiadomosc nr &e" + i + "&a.");
                        } catch (Exception e) { pl.msg(p, "&cUzycie: /afkadmin remove <numer>"); }
                    }
                    case "list" -> {
                        if (list.isEmpty()) { pl.msg(p, "&7Brak wiadomosci (uzywana jest domyslna)."); return true; }
                        for (int i = 0; i < list.size(); i++) pl.msg(p, "&e" + (i + 1) + ". &r" + list.get(i));
                    }
                    case "clear" -> {
                        pl.data.set("afk-messages", null);
                        pl.saveData();
                        pl.msg(p, "&aWyczyszczono wiadomosci AFK.");
                    }
                    default -> pl.msg(p, "&e/afkadmin add <tekst> | remove <numer> | list | clear");
                }
            }
            default -> { return false; }
        }
        return true;
    }

    // ===== pomocnicze =====

    private void listWarps(Player p) {
        ConfigurationSection s = pl.data.getConfigurationSection("warps");
        if (s == null || s.getKeys(false).isEmpty()) { pl.msg(p, "&7Brak warpow."); return; }
        pl.msg(p, "&6Warpy: &f" + String.join("&7, &f", s.getKeys(false)));
    }

    private Player takeRequest(Player target) {
        UUID id = target.getUniqueId();
        UUID reqId = pl.tpaRequests.remove(id);
        Long time = pl.tpaTime.remove(id);
        if (reqId == null || time == null || System.currentTimeMillis() - time > 60_000) return null;
        return Bukkit.getPlayer(reqId);
    }

    private void sendPm(Player from, Player to, String text) {
        pl.msg(from, "&7[&eJa &7-> &e" + to.getName() + "&7] &f" + text);
        pl.msg(to, "&7[&e" + from.getName() + " &7-> &eJa&7] &f" + text);
        pl.lastMsg.put(from.getUniqueId(), to.getUniqueId());
        pl.lastMsg.put(to.getUniqueId(), from.getUniqueId());
    }

    private void randomTp(Player p) {
        long now = System.currentTimeMillis();
        long cd = pl.getConfig().getLong("rtp.cooldown", 60) * 1000L;
        Long until = pl.rtpCooldown.get(p.getUniqueId());
        if (until != null && until > now && !p.hasPermission("sektor.bypass.cooldown")) {
            pl.msg(p, "&cMusisz poczekac jeszcze &e" + ((until - now) / 1000 + 1) + "s&c.");
            return;
        }
        World w = p.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) {
            pl.msg(p, "&cRTP dziala tylko w zwyklym swiecie.");
            return;
        }
        int radius = pl.getConfig().getInt("rtp.radius", 3000);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 20; i++) {
            int x = r.nextInt(-radius, radius + 1);
            int z = r.nextInt(-radius, radius + 1);
            int y = w.getHighestBlockYAt(x, z);
            Location l = new Location(w, x + 0.5, y + 1, z + 0.5);
            if (!w.getWorldBorder().isInside(l)) continue;
            if (w.getBlockAt(x, y, z).isLiquid()) continue;
            pl.rtpCooldown.put(p.getUniqueId(), now + cd);
            pl.teleport(p, l);
            return;
        }
        pl.msg(p, "&cNie znaleziono bezpiecznego miejsca, sprobuj ponownie.");
    }

    private void redeem(Player p, String[] args) {
        if (args.length < 1) { pl.msg(p, "&cUzycie: /kod <kod>"); return; }
        String base = "codes." + args[0].toLowerCase();
        if (!pl.data.contains(base)) { pl.msg(p, "&cNieprawidlowy kod."); return; }
        String uid = p.getUniqueId().toString();
        List<String> used = pl.data.getStringList(base + ".used");
        if (used.contains(uid)) { pl.msg(p, "&cJuz uzyles tego kodu."); return; }
        int max = pl.data.getInt(base + ".maxUses", -1);
        int uses = pl.data.getInt(base + ".uses", 0);
        if (max >= 0 && uses >= max) { pl.msg(p, "&cTen kod zostal juz w pelni wykorzystany."); return; }

        used.add(uid);
        pl.data.set(base + ".used", used);
        pl.data.set(base + ".uses", uses + 1);
        pl.saveData();
        for (String c : pl.data.getStringList(base + ".commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.replace("%player%", p.getName()));
        }
        pl.msg(p, "&aKod zostal zrealizowany!");
    }

    private boolean kodAdmin(CommandSender s, String[] a) {
        if (!admin(s)) return true;
        if (a.length == 0) {
            pl.msg(s, "&e/kodadmin add <nazwa> <maxUzyc|-1> <komenda...> &7(uzyj %player%)");
            pl.msg(s, "&e/kodadmin remove <nazwa>  &7| &e/kodadmin list");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "add" -> {
                if (a.length < 4) { pl.msg(s, "&cUzycie: /kodadmin add <nazwa> <maxUzyc|-1> <komenda...>"); return true; }
                int max;
                try { max = Integer.parseInt(a[2]); }
                catch (NumberFormatException e) { pl.msg(s, "&cMaxUzyc musi byc liczba."); return true; }
                String base = "codes." + a[1].toLowerCase();
                String command = String.join(" ", Arrays.copyOfRange(a, 3, a.length));
                List<String> cmds = pl.data.getStringList(base + ".commands");
                cmds.add(command);
                pl.data.set(base + ".commands", cmds);
                pl.data.set(base + ".maxUses", max);
                pl.saveData();
                pl.msg(s, "&aDodano komende do kodu &e" + a[1] + "&a: &f" + command);
            }
            case "remove" -> {
                if (a.length < 2) { pl.msg(s, "&cUzycie: /kodadmin remove <nazwa>"); return true; }
                pl.data.set("codes." + a[1].toLowerCase(), null);
                pl.saveData();
                pl.msg(s, "&aUsunieto kod.");
            }
            case "list" -> {
                ConfigurationSection c = pl.data.getConfigurationSection("codes");
                pl.msg(s, c == null || c.getKeys(false).isEmpty() ? "&7Brak kodow." : "&6Kody: &f" + String.join("&7, &f", c.getKeys(false)));
            }
            default -> pl.msg(s, "&cNieznana podkomenda.");
        }
        return true;
    }
}
