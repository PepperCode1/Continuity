package me.pepperbell.continuity.client.util.biome;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public interface BiomeView {
	Biome getBiome(BlockPos pos);
}
