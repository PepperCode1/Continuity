package me.pepperbell.continuity.client.processor.overlay;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.AbstractQuadProcessor;
import me.pepperbell.continuity.client.processor.AbstractQuadProcessorFactory;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.processor.DirectionMaps;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.ProcessingPredicate;
import me.pepperbell.continuity.client.properties.overlay.OverlayPropertiesSection;
import me.pepperbell.continuity.client.properties.overlay.StandardOverlayCTMProperties;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.client.util.SpriteCalculator;
import me.pepperbell.continuity.client.util.TextureUtil;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.EmptyBlockView;

public class StandardOverlayQuadProcessor extends AbstractQuadProcessor {
	@Nullable
	protected Set<Identifier> matchTilesSet;
	@Nullable
	protected Predicate<BlockState> matchBlocksPredicate;
	@Nullable
	protected Set<Identifier> connectTilesSet;
	@Nullable
	protected Predicate<BlockState> connectBlocksPredicate;
	protected ConnectionPredicate connectionPredicate;

	protected int tintIndex;
	@Nullable
	protected BlockState tintBlock;
	protected RenderMaterial material;

	public StandardOverlayQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, @Nullable Set<Identifier> matchTilesSet, @Nullable Predicate<BlockState> matchBlocksPredicate, @Nullable Set<Identifier> connectTilesSet, @Nullable Predicate<BlockState> connectBlocksPredicate, ConnectionPredicate connectionPredicate, int tintIndex, @Nullable BlockState tintBlock, BlendMode layer) {
		super(sprites, processingPredicate);
		this.matchTilesSet = matchTilesSet;
		this.matchBlocksPredicate = matchBlocksPredicate;
		this.connectTilesSet = connectTilesSet;
		this.connectBlocksPredicate = connectBlocksPredicate;
		this.connectionPredicate = connectionPredicate;

		this.tintIndex = tintIndex;
		this.tintBlock = tintBlock;
		material = RenderUtil.getMaterialFinder().blendMode(0, layer).find();
	}

	@Override
	public ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, int processorIndex, ProcessingContext context) {
		Direction lightFace = quad.lightFace();
		OverlayEmitter emitter = getEmitter(blockView, pos, state, lightFace, sprite, DirectionMaps.getMap(lightFace)[0], context);
		if (emitter != null) {
			context.addEmitterConsumer(emitter);
		}
		return ProcessingResult.CONTINUE;
	}

	protected boolean appliesOverlay(BlockState other, BlockRenderView blockView, BlockState state, BlockPos pos, Direction face, Sprite quadSprite) {
		if (other.getBlock().hasDynamicBounds()) {
			return false;
		}
		if (!other.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
			return false;
		}
		if (connectBlocksPredicate != null) {
			if (!connectBlocksPredicate.test(other)) {
				return false;
			}
		}
		if (connectTilesSet != null) {
			if (!connectTilesSet.contains(SpriteCalculator.getSprite(other, face).getId())) {
				return false;
			}
		}
		return !connectionPredicate.shouldConnect(blockView, state, pos, other, face, quadSprite);
	}

	protected boolean hasSameOverlay(BlockState other, BlockRenderView blockView, BlockState state, BlockPos pos, Direction face, Sprite quadSprite) {
		if (matchBlocksPredicate != null) {
			if (!matchBlocksPredicate.test(other)) {
				return false;
			}
		}
		if (matchTilesSet != null) {
			if (!matchTilesSet.contains(SpriteCalculator.getSprite(other, face).getId())) {
				return false;
			}
		}
		return true;
	}

	protected boolean appliesOverlayUnobscured(BlockState state0, Direction direction0, BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, BlockPos.Mutable mutablePos) {
		boolean a0 = appliesOverlay(state0, blockView, state, pos, lightFace, quadSprite);
		if (a0) {
			mutablePos.set(pos, direction0).move(lightFace);
			a0 = !blockView.getBlockState(mutablePos).isOpaqueFullCube(blockView, mutablePos);
		}
		return a0;
	}

	protected boolean hasSameOverlayUnobscured(BlockState state0, Direction direction0, BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, BlockPos.Mutable mutablePos) {
		boolean s0 = hasSameOverlay(state0, blockView, state, pos, lightFace, quadSprite);
		if (s0) {
			mutablePos.set(pos, direction0).move(lightFace);
			s0 = !blockView.getBlockState(mutablePos).isOpaqueFullCube(blockView, mutablePos);
		}
		return s0;
	}

	protected boolean appliesOverlayCorner(Direction direction0, Direction direction1, BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, BlockPos.Mutable mutablePos) {
		mutablePos.set(pos, direction0).move(direction1);
		boolean corner0 = appliesOverlay(blockView.getBlockState(mutablePos), blockView, state, pos, lightFace, quadSprite);
		if (corner0) {
			mutablePos.move(lightFace);
			corner0 = !blockView.getBlockState(mutablePos).isOpaqueFullCube(blockView, mutablePos);
		}
		return corner0;
	}

	protected OverlayEmitter fromCorner(Direction direction0, Direction direction1, int sprite0, int sprite1, OverlayEmitter emitter, BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, BlockPos.Mutable mutablePos) {
		prepareEmitter(emitter, lightFace, blockView, pos);
		emitter.addSprite(sprites[sprite0]);
		mutablePos.set(pos, direction0).move(direction1);
		if (appliesOverlay(blockView.getBlockState(mutablePos), blockView, state, pos, lightFace, quadSprite)) {
			mutablePos.move(lightFace);
			if (!blockView.getBlockState(mutablePos).isOpaqueFullCube(blockView, mutablePos)) {
				emitter.addSprite(sprites[sprite1]);
			}
		}
		return emitter;
	}

	protected OverlayEmitter fromOneSide(BlockState state0, BlockState state1, BlockState state2, Direction direction0, Direction direction1, Direction direction2, int sprite0, int sprite1, int sprite2, OverlayEmitter emitter, BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, BlockPos.Mutable mutablePos) {
		boolean s0 = hasSameOverlayUnobscured(state0, direction0, blockView, pos, state, lightFace, quadSprite, mutablePos);
		boolean s1 = hasSameOverlayUnobscured(state1, direction1, blockView, pos, state, lightFace, quadSprite, mutablePos);
		boolean s2 = hasSameOverlayUnobscured(state2, direction2, blockView, pos, state, lightFace, quadSprite, mutablePos);

		prepareEmitter(emitter, lightFace, blockView, pos);
		emitter.addSprite(sprites[sprite0]);
		if (s0 | s1) {
			if (appliesOverlayCorner(direction0, direction1, blockView, pos, state, lightFace, quadSprite, mutablePos)) {
				emitter.addSprite(sprites[sprite1]);
			}
		}
		if (s1 | s2) {
			if (appliesOverlayCorner(direction1, direction2, blockView, pos, state, lightFace, quadSprite, mutablePos)) {
				emitter.addSprite(sprites[sprite2]);
			}
		}
		return emitter;
	}

	protected static OverlayEmitter getEmitter(ProcessingDataProvider dataProvider) {
		return dataProvider.getData(ProcessingDataKeys.STANDARD_OVERLAY_EMITTER_POOL_KEY).get();
	}

	protected void prepareEmitter(OverlayEmitter emitter, Direction face, BlockRenderView blockView, BlockPos pos) {
		emitter.prepare(face, RenderUtil.getTintColor(tintBlock, blockView, pos, tintIndex), material);
	}

	protected OverlayEmitter prepareEmitter(OverlayEmitter emitter, Direction face, BlockRenderView blockView, BlockPos pos, int sprite1) {
		prepareEmitter(emitter, face, blockView, pos);
		emitter.addSprite(sprites[sprite1]);
		return emitter;
	}

	protected OverlayEmitter prepareEmitter(OverlayEmitter emitter, Direction face, BlockRenderView blockView, BlockPos pos, int sprite1, int sprite2) {
		prepareEmitter(emitter, face, blockView, pos);
		emitter.addSprite(sprites[sprite1]);
		emitter.addSprite(sprites[sprite2]);
		return emitter;
	}

	/*
	0:	D R (CORNER)
	1:	D
	2:	L D (CORNER)
	3:	D R
	4:	L D
	5:	L D R
	6:	L D T
	7:	R
	8:	L D R U
	9:	L
	10:	R U
	11:	L U
	12:	D R U
	13:	L R U
	14:	R U (CORNER)
	15:	U
	16:	L U (CORNER)
	 */
	@Nullable
	protected OverlayEmitter getEmitter(BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, Direction[] directions, ProcessingDataProvider dataProvider) {
		BlockPos.Mutable mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS_KEY);

		//

		mutablePos.set(pos, directions[0]);
		BlockState state0 = blockView.getBlockState(mutablePos);
		boolean left = appliesOverlayUnobscured(state0, directions[0], blockView, pos, state, lightFace, quadSprite, mutablePos);
		mutablePos.set(pos, directions[1]);
		BlockState state1 = blockView.getBlockState(mutablePos);
		boolean down = appliesOverlayUnobscured(state1, directions[1], blockView, pos, state, lightFace, quadSprite, mutablePos);
		mutablePos.set(pos, directions[2]);
		BlockState state2 = blockView.getBlockState(mutablePos);
		boolean right = appliesOverlayUnobscured(state2, directions[2], blockView, pos, state, lightFace, quadSprite, mutablePos);
		mutablePos.set(pos, directions[3]);
		BlockState state3 = blockView.getBlockState(mutablePos);
		boolean up = appliesOverlayUnobscured(state3, directions[3], blockView, pos, state, lightFace, quadSprite, mutablePos);

		//

		if (left & down & right & up) {
			return prepareEmitter(getEmitter(dataProvider), lightFace, blockView, pos, 8);
		}
		if (left & down & right) {
			return prepareEmitter(getEmitter(dataProvider), lightFace, blockView, pos, 5);
		}
		if (left & down & up) {
			return prepareEmitter(getEmitter(dataProvider), lightFace, blockView, pos, 6);
		}
		if (left & right & up) {
			return prepareEmitter(getEmitter(dataProvider), lightFace, blockView, pos, 13);
		}
		if (down & right & up) {
			return prepareEmitter(getEmitter(dataProvider), lightFace, blockView, pos, 12);
		}

		if (left & right) {
			return prepareEmitter(getEmitter(dataProvider), lightFace, blockView, pos, 9, 7);
		}
		if (up & down) {
			return prepareEmitter(getEmitter(dataProvider), lightFace, blockView, pos, 15, 1);
		}

		if (left & down) {
			return fromCorner(directions[2], directions[3], 4, 14, getEmitter(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (down & right) {
			return fromCorner(directions[0], directions[3], 3, 16, getEmitter(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (right & up) {
			return fromCorner(directions[0], directions[1], 10, 2, getEmitter(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (up & left) {
			return fromCorner(directions[1], directions[2], 11, 0, getEmitter(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}

		//

		if (left) {
			return fromOneSide(state1, state2, state3, directions[1], directions[2], directions[3], 9, 0, 14, getEmitter(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (down) {
			return fromOneSide(state2, state3, state0, directions[2], directions[3], directions[0], 1, 14, 16, getEmitter(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (right) {
			return fromOneSide(state3, state0, state1, directions[3], directions[0], directions[1], 7, 16, 2, getEmitter(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (up) {
			return fromOneSide(state0, state1, state2, directions[0], directions[1], directions[2], 15, 2, 0, getEmitter(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}

		//

		boolean s0 = hasSameOverlayUnobscured(state0, directions[0], blockView, pos, state, lightFace, quadSprite, mutablePos);
		boolean s1 = hasSameOverlayUnobscured(state1, directions[1], blockView, pos, state, lightFace, quadSprite, mutablePos);
		boolean s2 = hasSameOverlayUnobscured(state2, directions[2], blockView, pos, state, lightFace, quadSprite, mutablePos);
		boolean s3 = hasSameOverlayUnobscured(state3, directions[3], blockView, pos, state, lightFace, quadSprite, mutablePos);

		boolean corner0 = false;
		boolean corner1 = false;
		boolean corner2 = false;
		boolean corner3 = false;
		if (s0 | s1) {
			corner0 = appliesOverlayCorner(directions[0], directions[1], blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (s1 | s2) {
			corner1 = appliesOverlayCorner(directions[1], directions[2], blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (s2 | s3) {
			corner2 = appliesOverlayCorner(directions[2], directions[3], blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (s3 | s0) {
			corner3 = appliesOverlayCorner(directions[3], directions[0], blockView, pos, state, lightFace, quadSprite, mutablePos);
		}

		if (corner0 | corner1 | corner2 | corner3) {
			OverlayEmitter emitter = getEmitter(dataProvider);
			prepareEmitter(emitter, lightFace, blockView, pos);
			if (corner0) {
				emitter.addSprite(sprites[2]);
			}
			if (corner1) {
				emitter.addSprite(sprites[0]);
			}
			if (corner2) {
				emitter.addSprite(sprites[14]);
			}
			if (corner3) {
				emitter.addSprite(sprites[16]);
			}
			return emitter;
		}

		//

		return null;
	}

	public static class OverlayEmitter implements Consumer<QuadEmitter> {
		protected static final Sprite[] EMPTY_SPRITES = new Sprite[4];

		protected Sprite[] sprites = new Sprite[4];
		protected int spriteAmount;
		protected Direction face;
		protected int color;
		protected RenderMaterial material;

		@Override
		public void accept(QuadEmitter emitter) {
			for (int i = 0; i < spriteAmount; i++) {
				QuadUtil.emitOverlayQuad(emitter, face, sprites[i], color, material);
			}
		}

		public void prepare(Direction face, int color, RenderMaterial material) {
			System.arraycopy(EMPTY_SPRITES, 0, sprites, 0, EMPTY_SPRITES.length);
			spriteAmount = 0;
			this.face = face;
			this.color = color;
			this.material = material;
		}

		public void addSprite(Sprite sprite) {
			if (sprite != null && !TextureUtil.isMissingSprite(sprite)) {
				sprites[spriteAmount] = sprite;
				spriteAmount++;
			}
		}
	}

	public static class OverlayEmitterPool {
		protected final List<OverlayEmitter> list = new ObjectArrayList<>();
		protected int nextIndex = 0;

		public OverlayEmitter get() {
			if (nextIndex >= list.size()) {
				list.add(new OverlayEmitter());
			}
			OverlayEmitter emitter = list.get(nextIndex);
			nextIndex++;
			return emitter;
		}

		public void reset() {
			nextIndex = 0;
		}
	}

	public static class Factory extends AbstractQuadProcessorFactory<StandardOverlayCTMProperties> {
		@Override
		public QuadProcessor createProcessor(StandardOverlayCTMProperties properties, Sprite[] sprites) {
			OverlayPropertiesSection overlaySection = properties.getOverlayPropertiesSection();
			return new StandardOverlayQuadProcessor(sprites, OverlayProcessingPredicate.fromProperties(properties), properties.getMatchTilesSet(), properties.getMatchBlocksPredicate(), properties.getConnectTilesSet(), properties.getConnectBlocksPredicate(), properties.getConnectionPredicate(), overlaySection.getTintIndex(), overlaySection.getTintBlock(), overlaySection.getLayer());
		}

		@Override
		public int getTextureAmount(StandardOverlayCTMProperties properties) {
			return 17;
		}

		@Override
		public boolean supportsNullSprites(StandardOverlayCTMProperties properties) {
			return false;
		}
	}
}
