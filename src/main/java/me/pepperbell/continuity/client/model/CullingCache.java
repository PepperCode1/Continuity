package me.pepperbell.continuity.client.model;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class CullingCache {
	protected final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

	protected int completionFlags;
	protected int resultFlags;

	public boolean shouldCull(BlockRenderView blockView, BlockPos pos, BlockState state, Direction cullFace) {
		int mask = 1 << cullFace.ordinal();
		if ((completionFlags & mask) == 0) {
			completionFlags |= mask;
			if (Block.shouldDrawSide(state, blockView, pos, cullFace, mutablePos.set(pos, cullFace))) {
				resultFlags |= mask;
				return false;
			} else {
				return true;
			}
		} else {
			return (resultFlags & mask) == 0;
		}
	}

	public boolean shouldCull(QuadView quad, BlockRenderView blockView, BlockPos pos, BlockState state) {
		Direction cullFace = quad.cullFace();
		if (cullFace == null) {
			return false;
		}
		return shouldCull(blockView, pos, state, cullFace);
	}

	public void prepare() {
		completionFlags = 0;
		resultFlags = 0;
	}
}
