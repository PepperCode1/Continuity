package me.pepperbell.continuity.client.processor.simple;

import java.util.Random;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.properties.BaseCTMProperties;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class FixedSpriteProvider implements SpriteProvider {
	protected Sprite sprite;

	public FixedSpriteProvider(Sprite sprite) {
		this.sprite = sprite;
	}

	@Override
	@Nullable
	public Sprite getSprite(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, ProcessingDataProvider dataProvider) {
		return this.sprite;
	}

	public static class Factory implements SpriteProvider.Factory<BaseCTMProperties> {
		@Override
		public SpriteProvider createSpriteProvider(Sprite[] sprites, BaseCTMProperties properties) {
			return new FixedSpriteProvider(sprites[0]);
		}

		@Override
		public int getTextureAmount(BaseCTMProperties properties) {
			return 1;
		}
	}
}
