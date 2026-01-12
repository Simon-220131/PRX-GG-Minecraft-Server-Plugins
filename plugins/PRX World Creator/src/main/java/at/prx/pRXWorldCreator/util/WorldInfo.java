package at.prx.pRXWorldCreator.util;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldType;

public record WorldInfo(
        String name,
        boolean loaded,
        WorldType type,
        World.Environment environment,
        int players,
        Location spawn,
        long time,
        String weather,
        Difficulty difficulty,
        long seed
) {}

