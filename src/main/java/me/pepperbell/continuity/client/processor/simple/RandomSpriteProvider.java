package me.pepperbell.continuity.client.processor.simple;

import java.util.Random;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.Symmetry;
import me.pepperbell.continuity.client.properties.RandomCTMProperties;
import me.pepperbell.continuity.client.util.MathUtil;
import me.pepperbell.continuity.client.util.RandomIndexProvider;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
	@Nullable
	public Sprite getSprite(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, ProcessingDataProvider dataProvider) {
		Direction face = symmetry.getActualFace(quad.lightFace());

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		if (linked) {
			Block block = state.getBlock();
			BlockPos.Mutable mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS_KEY).set(pos);

			int i = 0;
			do {
				mutablePos.setY(mutablePos.getY() - 1);
				i++;
			} while (i < 3 && block == blockView.getBlockState(mutablePos).getBlock());
			y = mutablePos.getY() + 1;
		}

		int seed = MathUtil.mix(x, y, z, face.ordinal(), randomLoops);
		return sprites[indexProvider.getRandomIndex(seed)];
	}

	public static class Factory implements SpriteProvider.Factory<RandomCTMProperties> {
		@Override
		public SpriteProvider createSpriteProvider(Sprite[] sprites, RandomCTMProperties properties) {
			if (sprites.length == 1) {
				return new FixedSpriteProvider(sprites[0]);
			}
			return new RandomSpriteProvider(sprites, properties.getIndexProviderFactory().createIndexProvider(sprites.length), properties.getRandomLoops(), properties.getSymmetry(), properties.getLinked());
		}

		@Override
		public int getTextureAmount(RandomCTMProperties properties) {
			return properties.getSpriteIds().size();
		}
	}
}
