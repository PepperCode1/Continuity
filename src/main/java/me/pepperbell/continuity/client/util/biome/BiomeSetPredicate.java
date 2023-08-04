package me.pepperbell.continuity.client.util.biome;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.biome.Biome;

public class BiomeSetPredicate implements Predicate<Biome> {
	private final Set<BiomeHolder> holders;
	private Set<Biome> biomes = Collections.emptySet();

	public BiomeSetPredicate(Set<BiomeHolder> holders) {
		this.holders = holders;
		BiomeHolderManager.addRefreshCallback(this::refresh);
	}

	@Override
	public boolean test(Biome biome) {
		return biomes.contains(biome);
	}

	private void refresh() {
		Set<Biome> biomes = new ObjectOpenHashSet<>(Hash.DEFAULT_INITIAL_SIZE, Hash.FAST_LOAD_FACTOR);
		for (BiomeHolder holder : holders) {
			Biome biome = holder.getBiome();
			if (biome != null) {
				biomes.add(biome);
			}
		}
		this.biomes = biomes;
	}
}
