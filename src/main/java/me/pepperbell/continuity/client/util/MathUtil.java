package me.pepperbell.continuity.client.util;

import net.minecraft.util.math.MathHelper;

public final class MathUtil {
	// Borrowed from SplittableRandom
	public static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;

	public static int signum(int value) {
		if (value > 0) {
			return 1;
		}
		if (value < 0) {
			return -1;
		}
		return 0;
	}

	public static int lerp(float delta, int start, int end) {
		return start + (int) (delta * (end - start));
	}

	public static int lerpColor(float delta, int colorA, int colorB) {
		return (lerp(delta, colorA >>> 24 & 0xFF, colorB >>> 24 & 0xFF) << 24)
				| (lerp(delta, colorA >>> 16 & 0xFF, colorB >>> 16 & 0xFF) << 16)
				| (lerp(delta, colorA >>> 8 & 0xFF, colorB >>> 8 & 0xFF) << 8)
				| (lerp(delta, colorA & 0xFF, colorB & 0xFF));
	}

	public static int lerpLight(float delta, int lightA, int lightB) {
		return (lerp(delta, lightA >>> 20 & 0xF, lightB >>> 20 & 0xF) << 20)
				| (lerp(delta, lightA >>> 4 & 0xF, lightB >>> 4 & 0xF) << 4);
	}

	// Borrowed from SplittableRandom
	public static long mix64(long z) {
		z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
		z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
		return z ^ (z >>> 31);
	}

	// Borrowed from SplittableRandom
	public static int mix32(long z) {
		z = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L;
		return (int) (((z ^ (z >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
	}

	public static int mix(int x, int y, int z, int face, int loops) {
		return mix32((MathHelper.hashCode(x, y, z) ^ mix64(GOLDEN_GAMMA * (1 + face))) + GOLDEN_GAMMA * (1 + loops));
	}
}
