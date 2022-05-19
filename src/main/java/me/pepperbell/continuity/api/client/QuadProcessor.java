package me.pepperbell.continuity.api.client;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.AbstractRandom;
import net.minecraft.world.BlockRenderView;

public interface QuadProcessor {
	ProcessingResult processQuad(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<AbstractRandom> randomSupplier, int pass, int processorIndex, ProcessingContext context);

	interface ProcessingContext extends ProcessingDataProvider {
		void addEmitterConsumer(Consumer<QuadEmitter> consumer);

		void addMesh(Mesh mesh);
	}

	enum ProcessingResult {
		CONTINUE,
		STOP,
		ABORT_AND_RENDER_QUAD,
		ABORT_AND_CANCEL_QUAD;
	}
}
