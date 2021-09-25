package me.pepperbell.continuity.client.processor;

import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public interface ConnectionPredicate {
	boolean shouldConnect(BlockState state, Sprite quadSprite, BlockPos pos, BlockState to, Direction face, BlockRenderView blockView);

	default boolean shouldConnect(BlockState state, Sprite quadSprite, BlockPos pos, BlockPos toPos, Direction face, BlockRenderView blockView) {
		return shouldConnect(state, quadSprite, pos, blockView.getBlockState(toPos), face, blockView);
	}
}
