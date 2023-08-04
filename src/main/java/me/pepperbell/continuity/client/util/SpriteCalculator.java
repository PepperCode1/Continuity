package me.pepperbell.continuity.client.util;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public final class SpriteCalculator {
	private static final BlockModels MODELS = MinecraftClient.getInstance().getBakedModelManager().getBlockModels();

	private static final EnumMap<Direction, SpriteCache> SPRITE_CACHES = new EnumMap<>(Direction.class);
	static {
		for (Direction direction : Direction.values()) {
			SPRITE_CACHES.put(direction, new SpriteCache(direction));
		}
	}

	public static Sprite getSprite(BlockState state, Direction face) {
		return SPRITE_CACHES.get(face).getSprite(state);
	}

	public static Sprite calculateSprite(BlockState state, Direction face, Supplier<Random> randomSupplier) {
		BakedModel model = MODELS.getModel(state);
		try {
			List<BakedQuad> quads = model.getQuads(state, face, randomSupplier.get());
			if (!quads.isEmpty()) {
				return quads.get(0).getSprite();
			}
			quads = model.getQuads(state, null, randomSupplier.get());
			if (!quads.isEmpty()) {
				int amount = quads.size();
				for (int i = 0; i < amount; i++) {
					BakedQuad quad = quads.get(i);
					if (quad.getFace() == face) {
						return quad.getSprite();
					}
				}
			}
		} catch (Exception e) {
			//
		}
		return model.getParticleSprite();
	}

	public static void clearCache() {
		for (SpriteCache cache : SPRITE_CACHES.values()) {
			cache.clear();
		}
	}

	private static class SpriteCache {
		private final Direction face;
		private final Map<BlockState, Sprite> sprites = new Reference2ReferenceOpenHashMap<>();
		private final Supplier<Random> randomSupplier = new Supplier<>() {
			private final Random random = Random.create();

			@Override
			public Random get() {
				// Use item rendering seed for consistency
				random.setSeed(42L);
				return random;
			}
		};
		private final StampedLock lock = new StampedLock();

		public SpriteCache(Direction face) {
			this.face = face;
		}

		public Sprite getSprite(BlockState state) {
			Sprite sprite;
			long readStamp = lock.readLock();
			try {
				sprite = sprites.get(state);
			} finally {
				lock.unlockRead(readStamp);
			}
			if (sprite == null) {
				long writeStamp = lock.writeLock();
				try {
					sprite = calculateSprite(state, face, randomSupplier);
					sprites.put(state, sprite);
				} finally {
					lock.unlockWrite(writeStamp);
				}
			}
			return sprite;
		}

		public void clear() {
			long writeStamp = lock.writeLock();
			try {
				sprites.clear();
			} finally {
				lock.unlockWrite(writeStamp);
			}
		}
	}
}
