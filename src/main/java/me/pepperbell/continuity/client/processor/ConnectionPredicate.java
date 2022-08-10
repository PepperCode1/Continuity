package me.pepperbell.continuity.client.processor;

import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public interface ConnectionPredicate {
	boolean shouldConnect(BlockRenderView blockView, BlockState state, BlockPos pos, BlockState toState, Direction face, Sprite quadSprite);

	default boolean shouldConnect(BlockRenderView blockView, BlockState state, BlockPos pos, BlockPos toPos, Direction face, Sprite quadSprite) {
		return shouldConnect(blockView, state, pos, blockView.getBlockState(toPos), face, quadSprite);
	}

	default boolean shouldConnect(BlockRenderView blockView, BlockState state, BlockPos pos, BlockPos.Mutable toPos, Direction face, Sprite quadSprite, boolean innerSeams) {
		if (shouldConnect(blockView, state, pos, toPos, face, quadSprite)) {
			if (innerSeams) {
				toPos.move(face);
				return !shouldConnect(blockView, state, pos, toPos, face, quadSprite);
			} else {
				return true;
			}
		}
		return false;
	}
}
