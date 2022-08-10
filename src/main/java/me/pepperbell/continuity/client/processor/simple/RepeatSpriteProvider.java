package me.pepperbell.continuity.client.processor.simple;

import java.util.Random;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.Symmetry;
import me.pepperbell.continuity.client.properties.RepeatCTMProperties;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class RepeatSpriteProvider implements SpriteProvider {
	protected Sprite[] sprites;
	protected int width;
	protected int height;
	protected Symmetry symmetry;

	public RepeatSpriteProvider(Sprite[] sprites, int width, int height, Symmetry symmetry) {
		this.sprites = sprites;
		this.width = width;
		this.height = height;
		this.symmetry = symmetry;
	}

	@Override
	@Nullable
	public Sprite getSprite(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, ProcessingDataProvider dataProvider) {
		Direction face = symmetry.getActualFace(quad.lightFace());

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		int spriteX;
		int spriteY;
		switch (face) {
			case DOWN -> {
				// MCPatcher uses a different formula for the down face.
				// It is not used here to maintain Optifine parity.
				// spriteX = -x;
				// spriteY = -z;
				spriteX = x;
				spriteY = -z - 1;
			}
			case UP -> {
				spriteX = x;
				spriteY = z;
			}
			case NORTH -> {
				spriteX = -x - 1;
				spriteY = -y;
			}
			case SOUTH -> {
				spriteX = x;
				spriteY = -y;
			}
			case WEST -> {
				spriteX = z;
				spriteY = -y;
			}
			case EAST -> {
				spriteX = -z - 1;
				spriteY = -y;
			}
			default -> {
				spriteX = 0;
				spriteY = 0;
			}
		}

		spriteX %= width;
		if (spriteX < 0) {
			spriteX += width;
		}
		spriteY %= height;
		if (spriteY < 0) {
			spriteY += height;
		}

		return sprites[width * spriteY + spriteX];
	}

	public static class Factory implements SpriteProvider.Factory<RepeatCTMProperties> {
		@Override
		public SpriteProvider createSpriteProvider(Sprite[] sprites, RepeatCTMProperties properties) {
			return new RepeatSpriteProvider(sprites, properties.getWidth(), properties.getHeight(), properties.getSymmetry());
		}

		@Override
		public int getTextureAmount(RepeatCTMProperties properties) {
			return properties.getWidth() * properties.getHeight();
		}
	}
}
