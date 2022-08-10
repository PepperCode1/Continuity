package me.pepperbell.continuity.client.processor;

import java.util.Random;
import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.properties.StandardConnectingCTMProperties;
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
	protected static final int[] SECONDARY_SPRITE_INDEX_MAP = new int[] {
			3, 6, 3, 3, 3, 6, 3, 3, 4, 5, 4, 4, 3, 6, 3, 3,
			3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3,
			3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3,
			3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
	};

	public VerticalHorizontalQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate, boolean innerSeams) {
		super(sprites, processingPredicate, connectionPredicate, innerSeams);
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
			int secondaryConnections = getSecondaryConnections(directions, mutablePos, blockView, state, pos, quad.lightFace(), sprite);
			newSprite = sprites[SECONDARY_SPRITE_INDEX_MAP[secondaryConnections]];
		}
		return SimpleQuadProcessor.process(quad, sprite, newSprite);
	}

	protected int getSecondaryConnections(Direction[] directions, BlockPos.Mutable mutablePos, BlockRenderView blockView, BlockState state, BlockPos pos, Direction face, Sprite quadSprite) {
		int connections = 0;
		for (int i = 0; i < 2; i++) {
			Direction direction = directions[i * 2];
			mutablePos.set(pos, direction);
			if (connectionPredicate.shouldConnect(blockView, state, pos, mutablePos, face, quadSprite, innerSeams)) {
				connections |= 1 << (i * 3);
				for (int j = 0; j < 2; j++) {
					mutablePos.set(pos, direction).move(directions[((i + j) % 2) * 2 + 1]);
					if (connectionPredicate.shouldConnect(blockView, state, pos, mutablePos, face, quadSprite, innerSeams)) {
						connections |= 1 << ((i * 3 + j * 2 + 5) % 6);
					}
				}
			}
		}
		return connections;
	}

	public static class Factory extends AbstractQuadProcessorFactory<StandardConnectingCTMProperties> {
		@Override
		public QuadProcessor createProcessor(StandardConnectingCTMProperties properties, Sprite[] sprites) {
			return new VerticalHorizontalQuadProcessor(sprites, BaseProcessingPredicate.fromProperties(properties), properties.getConnectionPredicate(), properties.getInnerSeams());
		}

		@Override
		public int getTextureAmount(StandardConnectingCTMProperties properties) {
			return 7;
		}
	}
}
