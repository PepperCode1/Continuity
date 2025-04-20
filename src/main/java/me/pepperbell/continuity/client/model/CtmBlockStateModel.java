package me.pepperbell.continuity.client.model;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CtmBlockStateModel extends WrapperBlockStateModel {
    public static final int PASSES = 4;

    protected volatile Function<Sprite, QuadProcessors.Slice> defaultSliceFunc;

    public CtmBlockStateModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, BlockState state, Random random, Predicate<@Nullable Direction> cullTest) {
        if (!ContinuityConfig.INSTANCE.connectedTextures.get()) {
            super.emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }

        ModelObjectsContainer container = ModelObjectsContainer.get();
        if (!container.featureStates.getConnectedTexturesState().isEnabled()) {
            super.emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }

        CtmQuadTransform quadTransform = container.ctmQuadTransform;
        if (quadTransform.isActive()) {
            super.emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }

        // The correct way to get the appearance of the origin state from within a block model is to (1) call
        // getAppearance on the result of blockView.getBlockState(pos) instead of the passed state and (2) pass the
        // pos and world state of the adjacent block as the source pos and source state.
        // (1) is not followed here because at this point in execution, within this call to
        // CtmBakedModel#emitBlockQuads, the state parameter must already contain the world state. Even if this
        // CtmBakedModel is wrapped, then the wrapper must pass the same state as it received because not doing so can
        // cause crashes when the wrapped model is a vanilla multipart model or delegates to one. Thus, getting the
        // world state again is inefficient and unnecessary.
        // (2) is not possible here because the appearance state is necessary to get the slice and only the processors
        // within the slice actually perform checks on adjacent blocks. Likewise, the processors themselves cannot
        // retrieve the appearance state since the correct processors can only be chosen with the initially correct
        // appearance state.
        // Additionally, the side is chosen to always be the first constant of the enum (DOWN) for simplicity. Querying
        // the appearance for all six sides would be more correct, but less efficient. This may be fixed in the future,
        // especially if there is an actual use case for it.
        BlockState appearanceState = state.getAppearance(blockView, pos, Direction.DOWN, state, pos);

        quadTransform.prepare(blockView, appearanceState, state, pos, () -> random, cullTest, getSliceFunc(appearanceState));

        emitter.pushTransform(quadTransform);
        super.emitQuads(emitter, blockView, pos, state, random, cullTest);
        emitter.popTransform();

        quadTransform.processingContext.outputTo(emitter);
        quadTransform.reset();
    }

    protected Function<Sprite, QuadProcessors.Slice> getSliceFunc(BlockState state) {
        Function<Sprite, QuadProcessors.Slice> sliceFunc = defaultSliceFunc;
        if (sliceFunc == null) {
            synchronized (this) {
                sliceFunc = defaultSliceFunc;
                if (sliceFunc == null) {
                    sliceFunc = QuadProcessors.getCache(state);
                    defaultSliceFunc = sliceFunc;
                }
            }
        }
        return sliceFunc;
    }

    protected static class CtmQuadTransform implements QuadTransform {
        protected final ProcessingContextImpl processingContext = new ProcessingContextImpl();

        protected BlockRenderView blockView;
        protected BlockState appearanceState;
        protected BlockState state;
        protected BlockPos pos;
        protected Supplier<Random> randomSupplier;
        protected Predicate<@Nullable Direction> cullTest;
        protected Function<Sprite, QuadProcessors.Slice> sliceFunc;

        protected boolean active;

        @Override
        public boolean transform(MutableQuadView quad) {
            if (cullTest.test(quad.cullFace())) {
                return false;
            }

            for (int pass = 0; pass < PASSES; pass++) {
                Boolean result = transformOnce(quad, pass);
                if (result != null) {
                    return result;
                }
            }

            return true;
        }

        protected Boolean transformOnce(MutableQuadView quad, int pass) {
            Sprite sprite = RenderUtil.getSpriteFinder().find(quad);
            QuadProcessors.Slice slice = sliceFunc.apply(sprite);
            QuadProcessor[] processors = pass == 0 ? slice.processors() : slice.multipassProcessors();
            for (QuadProcessor processor : processors) {
                QuadProcessor.ProcessingResult result = processor.processQuad(quad, sprite, blockView, appearanceState, state, pos, randomSupplier, pass, processingContext);
                if (result == QuadProcessor.ProcessingResult.NEXT_PROCESSOR) {
                    continue;
                }
                if (result == QuadProcessor.ProcessingResult.NEXT_PASS) {
                    return null;
                }
                if (result == QuadProcessor.ProcessingResult.STOP) {
                    return true;
                }
                if (result == QuadProcessor.ProcessingResult.DISCARD) {
                    return false;
                }
            }
            return true;
        }

        public boolean isActive() {
            return active;
        }

        public void prepare(BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, Predicate<@Nullable Direction> cullTest, Function<Sprite, QuadProcessors.Slice> sliceFunc) {
            this.blockView = blockView;
            this.appearanceState = appearanceState;
            this.state = state;
            this.pos = pos;
            this.randomSupplier = randomSupplier;
            this.cullTest = cullTest;
            this.sliceFunc = sliceFunc;

            active = true;
        }

        public void reset() {
            blockView = null;
            appearanceState = null;
            state = null;
            pos = null;
            randomSupplier = null;
            cullTest = null;
            sliceFunc = null;

            active = false;

            processingContext.reset();
        }
    }
}
