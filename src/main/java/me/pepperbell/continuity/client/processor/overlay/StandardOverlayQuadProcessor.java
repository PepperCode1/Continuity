package me.pepperbell.continuity.client.processor.overlay;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.EmptyBlockView;

public class StandardOverlayQuadProcessor extends AbstractQuadProcessor {
	protected Set<Identifier> matchTilesSet;
	protected Predicate<BlockState> matchBlocksPredicate;
	protected Set<Identifier> connectTilesSet;
	protected Predicate<BlockState> connectBlocksPredicate;
	protected ConnectionPredicate connectionPredicate;

	protected int tintIndex;
	protected BlockState tintBlock;
	protected RenderMaterial material;

	public StandardOverlayQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, Set<Identifier> matchTilesSet, Predicate<BlockState> matchBlocksPredicate, Set<Identifier> connectTilesSet, Predicate<BlockState> connectBlocksPredicate, ConnectionPredicate connectionPredicate, int tintIndex, BlockState tintBlock, BlendMode layer) {
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
		OverlayRenderer renderer = getRenderer(blockView, pos, state, lightFace, sprite, DirectionMaps.getMap(lightFace)[0], context);
		if (renderer != null) {
			context.addEmitterConsumer(renderer);
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

	protected OverlayRenderer fromCorner(Direction direction0, Direction direction1, int sprite0, int sprite1, OverlayRenderer renderer, BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, BlockPos.Mutable mutablePos) {
		Sprite[] rendererSprites = prepareRenderer(renderer, lightFace, blockView, pos);
		mutablePos.set(pos, direction0).move(direction1);
		if (appliesOverlay(blockView.getBlockState(mutablePos), blockView, state, pos, lightFace, quadSprite)) {
			mutablePos.move(lightFace);
			if (!blockView.getBlockState(mutablePos).isOpaqueFullCube(blockView, mutablePos)) {
				rendererSprites[1] = sprites[sprite1];
			}
		}
		rendererSprites[0] = sprites[sprite0];
		return renderer;
	}

	protected OverlayRenderer fromOneSide(BlockState state0, BlockState state1, BlockState state2, Direction direction0, Direction direction1, Direction direction2, int sprite0, int sprite1, int sprite2, OverlayRenderer renderer, BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, BlockPos.Mutable mutablePos) {
		boolean s0 = hasSameOverlayUnobscured(state0, direction0, blockView, pos, state, lightFace, quadSprite, mutablePos);
		boolean s1 = hasSameOverlayUnobscured(state1, direction1, blockView, pos, state, lightFace, quadSprite, mutablePos);
		boolean s2 = hasSameOverlayUnobscured(state2, direction2, blockView, pos, state, lightFace, quadSprite, mutablePos);

		Sprite[] rendererSprites = prepareRenderer(renderer, lightFace, blockView, pos);
		rendererSprites[0] = sprites[sprite0];
		if (s0 | s1) {
			if (appliesOverlayCorner(direction0, direction1, blockView, pos, state, lightFace, quadSprite, mutablePos)) {
				rendererSprites[1] = sprites[sprite1];
			}
		}
		if (s1 | s2) {
			if (appliesOverlayCorner(direction1, direction2, blockView, pos, state, lightFace, quadSprite, mutablePos)) {
				rendererSprites[2] = sprites[sprite2];
			}
		}
		return renderer;
	}

	protected static OverlayRenderer getRenderer(ProcessingDataProvider dataProvider) {
		return dataProvider.getData(ProcessingDataKeys.STANDARD_OVERLAY_RENDERER_POOL_KEY).getRenderer();
	}

	protected Sprite[] prepareRenderer(OverlayRenderer renderer, Direction face, BlockRenderView blockView, BlockPos pos) {
		return renderer.prepare(face, RenderUtil.getTintColor(tintBlock, blockView, pos, tintIndex), material);
	}

	protected OverlayRenderer prepareRenderer(OverlayRenderer renderer, Direction face, BlockRenderView blockView, BlockPos pos, int sprite1) {
		Sprite[] rendererSprites = prepareRenderer(renderer, face, blockView, pos);
		rendererSprites[0] = sprites[sprite1];
		return renderer;
	}

	protected OverlayRenderer prepareRenderer(OverlayRenderer renderer, Direction face, BlockRenderView blockView, BlockPos pos, int sprite1, int sprite2) {
		Sprite[] rendererSprites = prepareRenderer(renderer, face, blockView, pos);
		rendererSprites[0] = sprites[sprite1];
		rendererSprites[1] = sprites[sprite2];
		return renderer;
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
	protected OverlayRenderer getRenderer(BlockRenderView blockView, BlockPos pos, BlockState state, Direction lightFace, Sprite quadSprite, Direction[] directions, ProcessingDataProvider dataProvider) {
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
			return prepareRenderer(getRenderer(dataProvider), lightFace, blockView, pos, 8);
		}
		if (left & down & right) {
			return prepareRenderer(getRenderer(dataProvider), lightFace, blockView, pos, 5);
		}
		if (left & down & up) {
			return prepareRenderer(getRenderer(dataProvider), lightFace, blockView, pos, 6);
		}
		if (left & right & up) {
			return prepareRenderer(getRenderer(dataProvider), lightFace, blockView, pos, 13);
		}
		if (down & right & up) {
			return prepareRenderer(getRenderer(dataProvider), lightFace, blockView, pos, 12);
		}

		if (left & right) {
			return prepareRenderer(getRenderer(dataProvider), lightFace, blockView, pos, 9, 7);
		}
		if (up & down) {
			return prepareRenderer(getRenderer(dataProvider), lightFace, blockView, pos, 15, 1);
		}

		if (left & down) {
			return fromCorner(directions[2], directions[3], 4, 14, getRenderer(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (down & right) {
			return fromCorner(directions[0], directions[3], 3, 16, getRenderer(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (right & up) {
			return fromCorner(directions[0], directions[1], 10, 2, getRenderer(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (up & left) {
			return fromCorner(directions[1], directions[2], 11, 0, getRenderer(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}

		//

		if (left) {
			return fromOneSide(state1, state2, state3, directions[1], directions[2], directions[3], 9, 0, 14, getRenderer(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (down) {
			return fromOneSide(state2, state3, state0, directions[2], directions[3], directions[0], 1, 14, 16, getRenderer(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (right) {
			return fromOneSide(state3, state0, state1, directions[3], directions[0], directions[1], 7, 16, 2, getRenderer(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
		}
		if (up) {
			return fromOneSide(state0, state1, state2, directions[0], directions[1], directions[2], 15, 2, 0, getRenderer(dataProvider), blockView, pos, state, lightFace, quadSprite, mutablePos);
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
			OverlayRenderer renderer = getRenderer(dataProvider);
			Sprite[] rendererSprites = prepareRenderer(renderer, lightFace, blockView, pos);
			if (corner0) {
				rendererSprites[0] = sprites[2];
			}
			if (corner1) {
				rendererSprites[1] = sprites[0];
			}
			if (corner2) {
				rendererSprites[2] = sprites[14];
			}
			if (corner3) {
				rendererSprites[3] = sprites[16];
			}
			return renderer;
		}

		//

		return null;
	}

	public static class OverlayRenderer implements Consumer<QuadEmitter> {
		protected static final Sprite[] EMPTY_SPRITES = new Sprite[4];

		protected Sprite[] sprites = new Sprite[4];
		protected Direction face;
		protected int color;
		protected RenderMaterial material;

		@Override
		public void accept(QuadEmitter emitter) {
			for (Sprite sprite : sprites) {
				if (sprite != null && !TextureUtil.isMissingSprite(sprite)) {
					QuadUtil.emitOverlayQuad(emitter, face, sprite, color, material);
				}
			}
		}

		public Sprite[] prepare(Direction face, int color, RenderMaterial material) {
			System.arraycopy(EMPTY_SPRITES, 0, sprites, 0, EMPTY_SPRITES.length);
			this.face = face;
			this.color = color;
			this.material = material;
			return sprites;
		}
	}

	public static class OverlayRendererPool {
		protected final List<OverlayRenderer> list = new ObjectArrayList<>();
		protected int nextIndex = 0;

		public OverlayRenderer getRenderer() {
			if (nextIndex >= list.size()) {
				list.add(new OverlayRenderer());
			}
			OverlayRenderer renderer = list.get(nextIndex);
			nextIndex++;
			return renderer;
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
