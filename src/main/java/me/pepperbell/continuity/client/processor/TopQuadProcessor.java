package me.pepperbell.continuity.client.processor;

import java.util.Random;
import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.properties.StandardConnectingCTMProperties;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class TopQuadProcessor extends ConnectingQuadProcessor {
	public TopQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate, boolean innerSeams) {
		super(sprites, processingPredicate, connectionPredicate, innerSeams);
	}

	@Override
	public ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, int processorIndex, ProcessingContext context) {
		Direction lightFace = quad.lightFace();
		Direction.Axis axis;
		if (state.contains(Properties.AXIS)) {
			axis = state.get(Properties.AXIS);
		} else {
			axis = Direction.Axis.Y;
		}
		if (lightFace.getAxis() != axis) {
			Direction up = Direction.from(axis, Direction.AxisDirection.POSITIVE);
			BlockPos.Mutable mutablePos = context.getData(ProcessingDataKeys.MUTABLE_POS_KEY).set(pos, up);
			if (connectionPredicate.shouldConnect(blockView, state, pos, mutablePos, lightFace, sprite, innerSeams)) {
				return SimpleQuadProcessor.process(quad, sprite, sprites[0]);
			}
		}
		return ProcessingResult.CONTINUE;
	}

	public static class Factory extends AbstractQuadProcessorFactory<StandardConnectingCTMProperties> {
		@Override
		public QuadProcessor createProcessor(StandardConnectingCTMProperties properties, Sprite[] sprites) {
			return new TopQuadProcessor(sprites, BaseProcessingPredicate.fromProperties(properties), properties.getConnectionPredicate(), properties.getInnerSeams());
		}

		@Override
		public int getTextureAmount(StandardConnectingCTMProperties properties) {
			return 1;
		}
	}
}
