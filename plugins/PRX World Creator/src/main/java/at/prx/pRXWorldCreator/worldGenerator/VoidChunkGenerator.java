package at.prx.pRXWorldCreator.worldGenerator;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.jspecify.annotations.NonNull;

import java.util.Random;

public class VoidChunkGenerator extends ChunkGenerator {

    public @NonNull ChunkData generateChunkData(
            @NonNull World world,
            @NonNull Random random,
            int x,
            int z,
            @NonNull BiomeGrid biome
    ) {
        return createChunkData(world);
    }
}

