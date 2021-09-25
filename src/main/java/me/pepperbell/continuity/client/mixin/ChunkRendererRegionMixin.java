package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import me.pepperbell.continuity.client.util.biome.BiomeCaches;
import me.pepperbell.continuity.client.util.biome.BiomeView;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

@Mixin(ChunkRendererRegion.class)
public class ChunkRendererRegionMixin implements BiomeView {
	@Shadow
	@Final
	protected BlockPos offset;
	@Shadow
	@Final
	protected int sizeX;
	@Shadow
	@Final
	protected int sizeZ;
	@Shadow
	@Final
	protected World world;

	@Unique
	protected Biome[] biomeCache;

	@Override
	public Biome getBiome(BlockPos pos) {
		if (biomeCache == null) {
			if (sizeX == 20 && sizeZ == 20) {
				biomeCache = BiomeCaches.getStandardCache();
			} else {
				biomeCache = BiomeCaches.createCache(sizeX, sizeZ);
			}
		}
		int index = BiomeCaches.getBiomeIndex(pos.getX() - offset.getX(), pos.getZ() - offset.getZ(), sizeX);
		Biome biome = biomeCache[index];
		if (biome == null) {
			biome = world.getBiome(pos);
			biomeCache[index] = biome;
		}
		return biome;
	}
}
