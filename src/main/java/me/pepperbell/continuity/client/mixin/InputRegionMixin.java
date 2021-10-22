package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import grondag.canvas.terrain.region.input.AbstractInputRegion;
import grondag.canvas.terrain.region.input.InputRegion;
import me.pepperbell.continuity.client.util.biome.BiomeView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

@Pseudo
@Mixin(value = InputRegion.class, remap = false)
public class InputRegionMixin extends AbstractInputRegion implements BiomeView {
	@Override
	public Biome getBiome(BlockPos pos) {
		return world.getBiome(pos);
	}
}
