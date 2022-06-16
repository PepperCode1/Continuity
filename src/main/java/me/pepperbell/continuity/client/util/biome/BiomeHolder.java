package me.pepperbell.continuity.client.util.biome;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

public class BiomeHolder {
	protected Identifier id;
	protected Biome biome;

	protected BiomeHolder(Identifier id) {
		this.id = id;
	}

	public Identifier getId() {
		return id;
	}

	@Nullable
	public Biome getBiome() {
		return biome;
	}

	public void refresh(Registry<Biome> biomeRegistry, Map<Identifier, Identifier> compressedIdMap) {
		Identifier realId = compressedIdMap.get(id);
		if (realId == null) {
			realId = id;
		}
		if (biomeRegistry.containsId(realId)) {
			biome = biomeRegistry.get(realId);
		} else {
			ContinuityClient.LOGGER.warn("Unknown biome '" + id + "'");
		}
	}
}
