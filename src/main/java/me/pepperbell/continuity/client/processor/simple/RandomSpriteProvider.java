package me.pepperbell.continuity.client.processor.simple;

import java.util.Random;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.HashCommon;
import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.Symmetry;
import me.pepperbell.continuity.client.properties.RandomCTMProperties;
import me.pepperbell.continuity.client.util.RandomIndexProvider;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;

public class RandomSpriteProvider implements SpriteProvider {
	protected Sprite[] sprites;
	protected RandomIndexProvider indexProvider;
	protected int randomLoops;
	protected Symmetry symmetry;
	protected boolean linked;

	public RandomSpriteProvider(Sprite[] sprites, RandomIndexProvider indexProvider, int randomLoops, Symmetry symmetry, boolean linked) {
		this.sprites = sprites;
		this.indexProvider = indexProvider;
		this.randomLoops = randomLoops;
		this.symmetry = symmetry;
		this.linked = linked;
	}

	@Override
	public Sprite getSprite(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, ProcessingDataProvider dataProvider) {
		Sprite newSprite;
		if (sprites.length == 1) {
			newSprite = sprites[0];
		} else {
			Direction face = symmetry.getActualFace(quad.lightFace());

			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			if (linked) {
				Block block = state.getBlock();
				BlockPos.Mutable mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS_KEY).set(pos);
				do {
					mutablePos.move(Direction.DOWN);
				} while (block == blockView.getBlockState(mutablePos).getBlock());
				y = mutablePos.getY() + 1;
			}

			int hash = hash(x, y, z, face.ordinal(), randomLoops);
			newSprite = sprites[indexProvider.getRandomIndex(hash)];
		}
		return newSprite;
	}

	public static int hash(int x, int y, int z, int face, int loops) {
		int hash = Integer.rotateLeft(Long.hashCode(MathHelper.hashCode(x, y, z)), face * 5);
		hash = HashCommon.mix(hash);
		for (int i = 0; i < loops; i++) {
			hash = HashCommon.mix(hash);
		}
		return hash;
	}

	public static class Factory implements SpriteProvider.Factory<RandomCTMProperties> {
		@Override
		public SpriteProvider createSpriteProvider(Sprite[] sprites, RandomCTMProperties properties) {
			return new RandomSpriteProvider(sprites, properties.getIndexProviderFactory().createIndexProvider(sprites.length), properties.getRandomLoops(), properties.getSymmetry(), properties.getLinked());
		}

		@Override
		public int getTextureAmount(RandomCTMProperties properties) {
			return properties.getSpriteIds().size();
		}
	}
}
