package me.pepperbell.continuity.client.util.biome;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;

public interface BiomeView {
	RegistryEntry<Biome> continuity$getBiome(BlockPos pos);
}
