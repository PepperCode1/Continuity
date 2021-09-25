package me.pepperbell.continuity.client.processor;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public interface ProcessingPredicate {
	boolean shouldProcessQuad(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos);
}
