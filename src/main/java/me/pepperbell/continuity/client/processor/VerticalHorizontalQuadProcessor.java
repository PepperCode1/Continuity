package me.pepperbell.continuity.client.processor;

import java.util.Random;
import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.properties.ConnectingCTMProperties;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class VerticalHorizontalQuadProcessor extends VerticalQuadProcessor {
	// Indices for this array are formed from these bit values:
	// 32     16
	// 1   *   8
	// 2       4
	protected static final int[] SPRITE_INDEX_MAP_1 = new int[] {
			3, 6, 3, 3, 3, 6, 3, 3, 4, 5, 4, 4, 3, 6, 3, 3,
			3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3,
			3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3,
			3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
	};

	public VerticalHorizontalQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate) {
		super(sprites, processingPredicate, connectionPredicate);
	}

	@Override
	public ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, int processorIndex, ProcessingContext context) {
		Direction[] directions = DirectionMaps.getDirections(quad);
		BlockPos.Mutable mutablePos = context.getData(ProcessingDataKeys.MUTABLE_POS_KEY);
		int connections = getConnections(directions, mutablePos, blockView, state, pos, quad.lightFace(), sprite);
		Sprite newSprite;
		if (connections != 0) {
			newSprite = sprites[SPRITE_INDEX_MAP[connections]];
		} else {
			connections = getConnections1(directions, mutablePos, blockView, state, pos, quad.lightFace(), sprite);
			newSprite = sprites[SPRITE_INDEX_MAP_1[connections]];
		}
		return SimpleQuadProcessor.process(quad, sprite, newSprite);
	}

	protected int getConnections1(Direction[] directions, BlockPos.Mutable mutablePos, BlockRenderView blockView, BlockState state, BlockPos pos, Direction face, Sprite quadSprite) {
		mutablePos.set(pos);
		int connections = 0;
		for (int i = 0; i < 2; i++) {
			mutablePos.move(directions[i * 2]);
			if (connectionPredicate.shouldConnect(state, quadSprite, pos, mutablePos, face, blockView)) {
				connections |= 1 << i * 3;
			}
			mutablePos.set(pos);
		}
		for (int i = 0; i < 4; i++) {
			int shift = (i / 2) * 3;
			int index1 = i;
			int index2 = (i + 1) % 4;
			if (((connections >> shift) & 1) == 1) {
				mutablePos.move(directions[index1]).move(directions[index2]);
				if (connectionPredicate.shouldConnect(state, quadSprite, pos, mutablePos, face, blockView)) {
					connections |= 1 << (shift + i % 2 + 1);
				}
				mutablePos.set(pos);
			}
		}
		return connections;
	}

	public static class Factory extends AbstractQuadProcessorFactory<ConnectingCTMProperties> {
		@Override
		public QuadProcessor createProcessor(ConnectingCTMProperties properties, Sprite[] sprites) {
			return new VerticalHorizontalQuadProcessor(sprites, BaseProcessingPredicate.fromProperties(properties), properties.getConnectionPredicate());
		}

		@Override
		public int getTextureAmount(ConnectingCTMProperties properties) {
			return 7;
		}
	}
}
