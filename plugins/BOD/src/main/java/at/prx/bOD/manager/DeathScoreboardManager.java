package at.prx.bOD.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class DeathScoreboardManager {

    private final JavaPlugin plugin;
    private final DeathBanManager manager;

    // gleiche Werte wie im DeathBanListener nutzen:
    private final long baseSeconds = 60;                  // 1 Minute
    private final long maxBanSeconds = 3 * 24 * 3600;     // 3 Tage

    private int taskId = -1;

    public DeathScoreboardManager(JavaPlugin plugin, DeathBanManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        // alle 20 Ticks (1 Sekunde) updaten
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                updateFor(p);
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void updateFor(Player player) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;

        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("deathban");

        if (obj == null || board == sm.getMainScoreboard()) {
            // neues Scoreboard für den Spieler erstellen
            board = sm.getNewScoreboard();
            obj = board.registerNewObjective("deathban", Criteria.DUMMY, "§c§lDeath§6Ban");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(board);
        }

        // Displayname ggf. updaten (Design B angelehnt)
        obj.setDisplayName("§c§l✦ §6Death§eBan §c✦");

        // erst alte Einträge löschen
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        UUID uuid = player.getUniqueId();
        int deaths = manager.getDeaths(uuid);
        int banShields = manager.getBanShields(player);

        // NÄCHSTE Bannlänge berechnen (wenn er JETZT nochmal stirbt)
        long nextBanSeconds = calcNextBanSeconds(deaths + 1);

        String nextBanFormatted = formatDuration(nextBanSeconds);
        String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        // Jetzt sauber die Zeilen setzen (Score-Werte von oben nach unten)
        int line = 10;

        // TOP LINE
        addLine(obj, "§8§m━━━━━━━━━━━━━━━", line--);

        addLine(obj, "§7Name:", line--);
        addLine(obj, "§f" + player.getName(), line--);

        addLine(obj, "§7Tode:", line--);
        addLine(obj, "§e" + deaths, line--);

        addLine(obj, "§7Ban Shields:", line--);
        addLine(obj, "§0§e" + banShields, line--);

        addLine(obj, "§7Nächste Strafe:", line--);
        addLine(obj, "§6" + nextBanFormatted, line--);

        addLine(obj, "§7Zeit: §b" + timeNow, line--);

        // BOTTOM LINE (unique)
        addLine(obj, "§8§m━━━━━━━━━━━━━━━§0", line--);
    }

    private void addLine(Objective obj, String text, int score) {
        // Jede Zeile muss unique sein. Wenn du gleiche Texte brauchst,
        // kannst du unsichtbare Farbcodes anhängen, z.B. §0, §1, ...
        obj.getScore(text).setScore(score);
    }

    private long calcNextBanSeconds(int deathsForNextBan) {
        long seconds = (long) (baseSeconds * Math.pow(2, deathsForNextBan - 1));
        return Math.min(seconds, maxBanSeconds);
    }

    private String formatDuration(long sec) {
        long days = sec / 86400;
        long hours = (sec % 86400) / 3600;
        long minutes = (sec % 3600) / 60;
        long seconds = sec % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}

