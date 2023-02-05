package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import me.pepperbell.continuity.client.util.biome.BiomeView;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

@Mixin(ChunkRendererRegion.class)
public class ChunkRendererRegionMixin implements BiomeView {
	@Shadow
	@Final
	protected World world;

	@Override
	public RegistryEntry<Biome> continuity$getBiome(BlockPos pos) {
		return world.getBiome(pos);
	}
}
