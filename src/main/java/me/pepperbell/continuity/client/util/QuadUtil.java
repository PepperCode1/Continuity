package me.pepperbell.continuity.client.util;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class QuadUtil {
	public static void interpolate(MutableQuadView quad, Sprite oldSprite, Sprite newSprite) {
		float oldMinU = oldSprite.getMinU();
		float oldMinV = oldSprite.getMinV();
		float newMinU = newSprite.getMinU();
		float newMinV = newSprite.getMinV();
		float uFactor = (newSprite.getMaxU() - newMinU) / (oldSprite.getMaxU() - oldMinU);
		float vFactor = (newSprite.getMaxV() - newMinV) / (oldSprite.getMaxV() - oldMinV);
		for (int i = 0; i < 4; i++) {
			quad.uv(i,
					newMinU + (quad.u(i) - oldMinU) * uFactor,
					newMinV + (quad.v(i) - oldMinV) * vFactor
			);
		}
	}

	public static void assignLerpedUVs(MutableQuadView quad, Sprite sprite) {
		float delta = sprite.getUvScaleDelta();
		float centerU = (sprite.getMinU() + sprite.getMaxU()) * 0.5f;
		float centerV = (sprite.getMinV() + sprite.getMaxV()) * 0.5f;
		float lerpedMinU = MathHelper.lerp(delta, sprite.getMinU(), centerU);
		float lerpedMaxU = MathHelper.lerp(delta, sprite.getMaxU(), centerU);
		float lerpedMinV = MathHelper.lerp(delta, sprite.getMinV(), centerV);
		float lerpedMaxV = MathHelper.lerp(delta, sprite.getMaxV(), centerV);
		quad.uv(0, lerpedMinU, lerpedMinV);
		quad.uv(1, lerpedMinU, lerpedMaxV);
		quad.uv(2, lerpedMaxU, lerpedMaxV);
		quad.uv(3, lerpedMaxU, lerpedMinV);
	}

	public static void emitOverlayQuad(QuadEmitter emitter, Direction face, Sprite sprite, int color, RenderMaterial material) {
		emitter.square(face, 0, 0, 1, 1, 0);
		emitter.color(color, color, color, color);
		assignLerpedUVs(emitter, sprite);
		emitter.material(material);
		emitter.emit();
	}

	public static boolean isQuadUnitSquare(QuadView quad) {
		int indexA;
		int indexB;
		switch (quad.lightFace().getAxis()) {
			case X:
				indexA = 1;
				indexB = 2;
				break;
			case Y:
				indexA = 0;
				indexB = 2;
				break;
			case Z:
				indexA = 1;
				indexB = 0;
				break;
			default:
				return false;
		}

		for (int i = 0; i < 4; i++) {
			float a = quad.posByIndex(i, indexA);
			if ((a >= 0.0001f || a <= -0.0001f) && (a >= 1.0001f || a <= 0.9999f)) {
				return false;
			}
			float b = quad.posByIndex(i, indexB);
			if ((b >= 0.0001f || b <= -0.0001f) && (b >= 1.0001f || b <= 0.9999f)) {
				return false;
			}
		}
		return true;
	}

	public static int getTextureOrientation(QuadView quad) {
		int rotation = getUVRotation(quad);
		if (getUVWinding(quad) == Winding.CLOCKWISE) {
			return rotation + 4;
		}
		return rotation;
	}

	public static int getUVRotation(QuadView quad) {
		int minVertex = 0;
		float minDistance = 3.0f;
		for (int vertexId = 0; vertexId < 4; vertexId++) {
			float u = quad.u(vertexId);
			float v = quad.v(vertexId);
			float distance = u * u + v * v;
			if (distance < minDistance) {
				minDistance = distance;
				minVertex = vertexId;
			}
		}
		return minVertex;
	}

	public static Winding getUVWinding(QuadView quad) {
		float u3 = quad.u(3);
		float v3 = quad.v(3);
		float u0 = quad.u(0);
		float v0 = quad.v(0);
		float u1 = quad.u(1);
		float v1 = quad.v(1);

		float value = (u3 - u0) * (v1 - v0) - (v3 - v0) * (u1 - u0);
		if (value > 0) {
			return Winding.COUNTERCLOCKWISE;
		} else if (value < 0) {
			return Winding.CLOCKWISE;
		}
		return Winding.UNDEFINED;
	}

	public enum Winding {
		COUNTERCLOCKWISE,
		CLOCKWISE,
		UNDEFINED;

		public Winding reverse() {
			if (this == UNDEFINED) {
				return this;
			}
			return this == CLOCKWISE ? COUNTERCLOCKWISE : CLOCKWISE;
		}
	}
}
