package me.pepperbell.continuity.client.util;

public final class MathUtil {
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
		return (lerp(delta, colorA >> 24 & 0xFF, colorB >> 24 & 0xFF) << 24)
				| (lerp(delta, colorA >> 16 & 0xFF, colorB >> 16 & 0xFF) << 16)
				| (lerp(delta, colorA >> 8 & 0xFF, colorB >> 8 & 0xFF) << 8)
				| (lerp(delta, colorA & 0xFF, colorB & 0xFF));
	}

	public static int lerpLight(float delta, int lightA, int lightB) {
		return (lerp(delta, lightA >> 20 & 0xF, lightB >> 20 & 0xF) << 20)
				| (lerp(delta, lightA >> 4 & 0xF, lightB >> 4 & 0xF) << 4);
	}
}
