package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.terrain.region.input.AbstractInputRegion;
import grondag.canvas.terrain.region.input.InputRegion;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import me.pepperbell.continuity.client.util.biome.BiomeCaches;
import me.pepperbell.continuity.client.util.biome.BiomeView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

@Pseudo
@Mixin(value = InputRegion.class, remap = false)
public class InputRegionMixin extends AbstractInputRegion implements BiomeView {
	@Unique
	protected Biome[] biomeCache = BiomeCaches.createStandardCache();

	@Inject(method = "prepare(Lgrondag/canvas/terrain/region/input/PackedInputRegion;)V", at = @At("HEAD"))
	private void onPrepare(PackedInputRegion packedInputRegion, CallbackInfo ci) {
		BiomeCaches.clearStandardCache(biomeCache);
	}

	@Override
	public Biome getBiome(BlockPos pos) {
		int index = BiomeCaches.getBiomeIndex(pos.getX() - originX + 2, pos.getZ() - originZ + 2, 20);
		Biome biome = biomeCache[index];
		if (biome == null) {
			biome = world.getBiome(pos);
			biomeCache[index] = biome;
		}
		return biome;
	}
}
