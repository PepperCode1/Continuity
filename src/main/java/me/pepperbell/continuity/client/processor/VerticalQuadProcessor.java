package me.pepperbell.continuity.client.processor;

import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.properties.ConnectingCTMProperties;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class VerticalQuadProcessor extends ConnectingQuadProcessor {
	// Indices for this array are formed from these bit values:
	// 2
	// *
	// 1
	protected static final int[] SPRITE_INDEX_MAP = new int[] {
			3, 2, 0, 1,
	};

	public VerticalQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate) {
		super(sprites, processingPredicate, connectionPredicate);
	}

	@Override
	public ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, int processorIndex, ProcessingContext context) {
		Direction[] directions = DirectionMaps.getDirections(quad);
		BlockPos.Mutable mutablePos = context.getData(ProcessingDataKeys.MUTABLE_POS_KEY);
		int connections = getConnections(directions, mutablePos, blockView, state, pos, quad.lightFace(), sprite);
		Sprite newSprite = sprites[SPRITE_INDEX_MAP[connections]];
		return SimpleQuadProcessor.process(quad, sprite, newSprite);
	}

	protected int getConnections(Direction[] directions, BlockPos.Mutable mutablePos, BlockRenderView blockView, BlockState state, BlockPos pos, Direction face, Sprite quadSprite) {
		mutablePos.set(pos);
		int connections = 0;
		for (int i = 0; i < 2; i++) {
			mutablePos.move(directions[i * 2 + 1]);
			if (connectionPredicate.shouldConnect(state, quadSprite, pos, mutablePos, face, blockView)) {
				connections |= 1 << i;
			}
			mutablePos.set(pos);
		}
		return connections;
	}

	public static class Factory extends AbstractQuadProcessorFactory<ConnectingCTMProperties> {
		@Override
		public QuadProcessor createProcessor(ConnectingCTMProperties properties, Sprite[] sprites) {
			return new VerticalQuadProcessor(sprites, BaseProcessingPredicate.fromProperties(properties), properties.getConnectionPredicate());
		}

		@Override
		public int getTextureAmount(ConnectingCTMProperties properties) {
			return 4;
		}
	}
}
