package me.pepperbell.continuity.client.util.biome;

import net.minecraft.world.biome.Biome;

public final class BiomeCaches {
	private static final Biome[] EMPTY = createCache(20);
	private static final ThreadLocal<Biome[]> CACHES = ThreadLocal.withInitial(BiomeCaches::createStandardCache);

	public static Biome[] createCache(int sizeX, int sizeZ) {
		return new Biome[sizeX * sizeZ];
	}

	public static Biome[] createCache(int size) {
		return createCache(size, size);
	}

	public static Biome[] createStandardCache() {
		return createCache(20);
	}

	public static void clearStandardCache(Biome[] cache) {
		System.arraycopy(EMPTY, 0, cache, 0, EMPTY.length);
	}

	/**
	 * Gets a standard 20x20 biome cache.
	 *
	 * @return a Biome array of length 400.
	 */
	public static Biome[] getStandardCache() {
		Biome[] cache = CACHES.get();
		clearStandardCache(cache);
		return cache;
	}

	public static int getBiomeIndex(int posX, int posZ, int sizeX) {
		return posZ * sizeX + posX;
	}
}
