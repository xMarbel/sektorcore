package pl.sektor.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SektorCore extends JavaPlugin implements Listener {

    public YamlConfiguration data;
    private File dataFile;

    public final Map<UUID, UUID> tpaRequests = new HashMap<>();   // cel -> proszacy
    public final Map<UUID, Long> tpaTime = new HashMap<>();
    public final Map<UUID, UUID> lastMsg = new HashMap<>();
    public final Map<UUID, Long> rtpCooldown = new HashMap<>();
    public final Map<UUID, Long> lastActive = new HashMap<>();
    public final Set<UUID> afk = new HashSet<>();                 // gracze w strefie AFK
    public final Map<UUID, Location> afkReturn = new HashMap<>(); // gdzie wrocic po AFK

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);

        Commands cmds = new Commands(this);
        for (String name : getDescription().getCommands().keySet()) {
            Objects.requireNonNull(getCommand(name)).setExecutor(cmds);
        }
        Bukkit.getPluginManager().registerEvents(this, this);

        // AFK checker
        new BukkitRunnable() {
            @Override public void run() { checkAfk(); }
        }.runTaskTimer(this, 20L, 20L);

        getLogger().info("SektorCore wlaczony.");
    }

    @Override
    public void onDisable() { saveData(); }

    public void saveData() {
        try { data.save(dataFile); }
        catch (IOException e) { getLogger().severe("Nie mozna zapisac data.yml: " + e.getMessage()); }
    }

    public Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    public void msg(CommandSender to, String s) { to.sendMessage(color(s)); }

    public int maxHomes(Player p) {
        int max = getConfig().getInt("max-homes", 3);
        for (int i = 100; i > max; i--) {
            if (p.hasPermission("sektor.homes." + i)) return i;
        }
        return max;
    }

    /** Teleport z opoznieniem z configu; ruch anuluje. */
    public void teleport(Player p, Location target) {
        int delay = getConfig().getInt("teleport-delay", 3);
        teleport(p, target, delay, null);
    }

    /** Teleport z podanym opoznieniem; po udanym teleporcie wywoluje onDone. */
    public void teleport(Player p, Location target, int delay, Runnable onDone) {
        if (delay <= 0 || p.hasPermission("sektor.bypass.delay")) {
            p.teleport(target);
            if (onDone != null) onDone.run();
            return;
        }
        Location start = p.getLocation();
        new BukkitRunnable() {
            int left = delay;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (!p.getLocation().getBlock().equals(start.getBlock())) {
                    msg(p, "&cTeleportacja anulowana - ruszyles sie.");
                    cancel();
                    return;
                }
                if (left <= 0) {
                    p.teleport(target);
                    if (onDone != null) onDone.run();
                    cancel();
                    return;
                }
                p.sendActionBar(color("&eTeleportacja za &6" + left + "s&e... nie ruszaj sie"));
                left--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    /** Wysyla graczowi wiadomosci ustawione przez admina (/afkadmin add). */
    public void sendAfkMessages(Player p) {
        List<String> list = data.getStringList("afk-messages");
        if (list.isEmpty()) {
            msg(p, "&7Przeniesiono na strefe AFK. Wpisz &e/afk&7, aby wrocic.");
            return;
        }
        for (String line : list) msg(p, line.replace("%player%", p.getName()));
    }

    private void checkAfk() {
        if (!getConfig().getBoolean("afk.auto-teleport", true)) return;
        Location zone = data.getLocation("afk");
        if (zone == null) return;
        long limit = getConfig().getLong("afk.seconds", 300) * 1000L;
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (afk.contains(id) || p.hasPermission("sektor.bypass.afk")) continue;
            long last = lastActive.getOrDefault(id, now);
            if (now - last > limit) {
                afkReturn.put(id, p.getLocation());
                afk.add(id);
                p.teleport(zone);
                sendAfkMessages(p);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!e.hasChangedPosition()) return;
        Player p = e.getPlayer();
        lastActive.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        lastActive.remove(id);
        afk.remove(id);
        afkReturn.remove(id);
        tpaRequests.remove(id);
        lastMsg.remove(id);
    }
}
