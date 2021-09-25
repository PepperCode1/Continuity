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

	public static float average(float a, float b) {
		return (a + b) * 0.5f;
	}

	public static int average(int a, int b) {
		return (a + b) / 2;
	}

	public static int averageColor(int color1, int color2) {
		return (average((color1 >>> 24) & 0xFF, (color2 >>> 24) & 0xFF) << 24)
				| (average((color1 >>> 16) & 0xFF, (color2 >>> 16) & 0xFF) << 16)
				| (average((color1 >>> 8) & 0xFF, (color2 >>> 8) & 0xFF) << 8)
				| (average(color1 & 0xFF, color2 & 0xFF));
	}

	public static int averageLight(int light1, int light2) {
		return (average((light1 >> 20) & 0xFF, (light2 >> 20) & 0xFF) << 20)
				| (average((light1 >> 4) & 0xFF, (light2 >> 4) & 0xFF) << 4);
	}
}
