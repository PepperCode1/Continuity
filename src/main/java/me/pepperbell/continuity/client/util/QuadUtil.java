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
			quad.sprite(i, 0,
					newMinU + (quad.spriteU(i, 0) - oldMinU) * uFactor,
					newMinV + (quad.spriteV(i, 0) - oldMinV) * vFactor
			);
		}
	}

	public static void assignLerpedUVs(MutableQuadView quad, Sprite sprite) {
		float delta = sprite.getAnimationFrameDelta();
		float centerU = (sprite.getMinU() + sprite.getMaxU()) / 2.0f;
		float centerV = (sprite.getMinV() + sprite.getMaxV()) / 2.0f;
		float lerpedMinU = MathHelper.lerp(delta, sprite.getMinU(), centerU);
		float lerpedMaxU = MathHelper.lerp(delta, sprite.getMaxU(), centerU);
		float lerpedMinV = MathHelper.lerp(delta, sprite.getMinV(), centerV);
		float lerpedMaxV = MathHelper.lerp(delta, sprite.getMaxV(), centerV);
		quad.sprite(0, 0, lerpedMinU, lerpedMinV);
		quad.sprite(1, 0, lerpedMinU, lerpedMaxV);
		quad.sprite(2, 0, lerpedMaxU, lerpedMaxV);
		quad.sprite(3, 0, lerpedMaxU, lerpedMinV);
	}

	public static void emitOverlayQuad(QuadEmitter emitter, Direction face, Sprite sprite, int color, RenderMaterial material) {
		emitter.square(face, 0, 0, 1, 1, 0);
		emitter.spriteColor(0, color, color, color, color);
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
			float u = quad.spriteU(vertexId, 0);
			float v = quad.spriteV(vertexId, 0);
			float distance = u * u + v * v;
			if (distance < minDistance) {
				minDistance = distance;
				minVertex = vertexId;
			}
		}
		return minVertex;
	}

	public static Winding getUVWinding(QuadView quad) {
		float u3 = quad.spriteU(3, 0);
		float v3 = quad.spriteV(3, 0);
		float u0 = quad.spriteU(0, 0);
		float v0 = quad.spriteV(0, 0);
		float u1 = quad.spriteU(1, 0);
		float v1 = quad.spriteV(1, 0);

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
