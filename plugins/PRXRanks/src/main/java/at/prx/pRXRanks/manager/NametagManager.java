package at.prx.pRXRanks.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

public class NametagManager {

    private final RankManager rankService;
    private final Scoreboard scoreboard;

    // Cache: Prefix → Team
    private final Map<String, Team> teams = new HashMap<>();

    public NametagManager(RankManager rankService) {
        this.rankService = rankService;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void update(Player player) {
        if (player == null) return;

        String prefix = rankService.getPrefix(player);

        // Anzeige-Prefix bauen
        String teamPrefix = "";
        if (!prefix.isEmpty()) {
            teamPrefix = prefix + " §8| ";
        }

        // Team holen oder erstellen
        Team team = teams.get(teamPrefix);
        if (team == null) {
            String teamName = createTeamName(teamPrefix);
            team = scoreboard.getTeam(teamName);

            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                team.setPrefix(teamPrefix);
            }

            teams.put(teamPrefix, team);
        }

        // Spieler aus anderen PRX-Teams entfernen
        for (Team t : scoreboard.getTeams()) {
            if (t.getName().startsWith("prx_")) {
                t.removeEntry(player.getName());
            }
        }

        team.addEntry(player.getName());
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            update(p);
        }
    }

    // Kurzer, sicherer Teamname (max 16 Zeichen)
    private String createTeamName(String prefix) {
        return "prx_" + Math.abs(prefix.hashCode());
    }
}
