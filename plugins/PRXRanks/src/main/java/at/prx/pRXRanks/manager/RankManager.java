package at.prx.pRXRanks.manager;

import at.prx.pRXRanks.util.UnicodeUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

public class RankManager {

    private final LuckPerms luckPerms;

    public RankManager() {
        this.luckPerms = LuckPermsProvider.get();
    }

    /**
     * Gibt den Prefix eines Spielers zurück (aus LuckPerms)
     * Kann leer sein ("")
     */
    public String getPrefix(Player player) {
        if (player == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        CachedMetaData meta = user.getCachedData().getMetaData();
        String prefix = meta.getPrefix();

//        return prefix != null ? prefix : "";
        return UnicodeUtil.unescapeUnicode(prefix);
    }

    /**
     * Gibt das Weight der Primary Group zurück
     * Höher = wichtiger Rang
     */
    public int getWeight(Player player) {
        if (player == null) return 0;

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return 0;

        return user.getCachedData().getMetaData().getPrefixes().firstKey();
    }

    /**
     * Optional: Name der Primary Group (Debug / Info)
     */
    public String getPrimaryGroup(Player player) {
        if (player == null) return "default";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "default";

        return user.getPrimaryGroup();
    }
}
