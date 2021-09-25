package me.pepperbell.continuity.client.processor;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.api.client.QuadProcessorFactory;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.simple.CTMSpriteProvider;
import me.pepperbell.continuity.client.properties.BaseCTMProperties;
import me.pepperbell.continuity.client.properties.CompactConnectingCTMProperties;
import me.pepperbell.continuity.client.util.DirectionMaps;
import me.pepperbell.continuity.client.util.MathUtil;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.TextureUtil;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class CompactCTMQuadProcessor extends ConnectingQuadProcessor {
	protected static int[][] QUAD_INDEX_MAP = new int[8][];
	static {
		int[][] map = QUAD_INDEX_MAP;

		map[0] = new int[] { 0, 1, 2, 3 }; // 0 - 0 1 2 3
		map[1] = map[0].clone(); // 1 - 3 0 1 2
		ArrayUtils.shift(map[1], 1);
		map[2] = map[1].clone(); // 2 - 2 3 0 1
		ArrayUtils.shift(map[2], 1);
		map[3] = map[2].clone(); // 3 - 1 2 3 0
		ArrayUtils.shift(map[3], 1);

		map[4] = map[3].clone(); // 4 - 0 3 2 1
		ArrayUtils.reverse(map[4]);
		map[5] = map[4].clone(); // 5 - 1 0 3 2
		ArrayUtils.shift(map[5], 1);
		map[6] = map[5].clone(); // 6 - 2 1 0 3
		ArrayUtils.shift(map[6], 1);
		map[7] = map[6].clone(); // 7 - 3 2 1 0
		ArrayUtils.shift(map[7], 1);
	}

	protected boolean innerSeams;
	protected Sprite[] replacementSprites;

	public CompactCTMQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate, boolean innerSeams, Sprite[] replacementSprites) {
		super(sprites, processingPredicate, connectionPredicate);
		this.innerSeams = innerSeams;
		this.replacementSprites = replacementSprites;
	}

	@Override
	public ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, int processorIndex, ProcessingContext context) {
		int orientation = QuadUtil.getTextureOrientation(quad);
		Direction[] directions = DirectionMaps.getMap(quad.lightFace())[orientation];
		BlockPos.Mutable mutablePos = context.getData(ProcessingDataKeys.MUTABLE_POS_KEY);
		int connections = CTMSpriteProvider.getConnections(connectionPredicate, innerSeams, directions, mutablePos, blockView, state, pos, quad.lightFace(), sprite);

		//

		if (replacementSprites != null) {
			int ctmIndex = CTMSpriteProvider.SPRITE_INDEX_MAP[connections];
			Sprite replacementSprite = replacementSprites[ctmIndex];
			if (replacementSprite != null) {
				QuadUtil.interpolate(quad, sprite, replacementSprite);
				return ProcessingResult.STOP;
			}
		}

		//

		MeshBuilder meshBuilder = context.getData(ProcessingDataKeys.MESH_BUILDER_KEY);
		QuadEmitter quadEmitter = meshBuilder.getEmitter();

		int[] quadIndices = QUAD_INDEX_MAP[orientation];

		//

		Direction cullFace = quad.cullFace();
		RenderMaterial material = quad.material();

		boolean n0 = quad.hasNormal(0);
		boolean n1 = quad.hasNormal(1);
		boolean n2 = quad.hasNormal(2);
		boolean n3 = quad.hasNormal(3);
		boolean n01 = n0 && n1;
		boolean n12 = n1 && n2;
		boolean n23 = n2 && n3;
		boolean n30 = n3 && n0;
		boolean n4 = n0 && n2;

		float x01 = MathUtil.average(quad.x(0), quad.x(1));
		float y01 = MathUtil.average(quad.y(0), quad.y(1));
		float z01 = MathUtil.average(quad.z(0), quad.z(1));
		int c01 = MathUtil.averageColor(quad.spriteColor(0, 0), quad.spriteColor(1, 0));
		int l01 = MathUtil.averageLight(quad.lightmap(0), quad.lightmap(1));
		float u01 = MathUtil.average(quad.spriteU(0, 0), quad.spriteU(1, 0));
		float v01 = MathUtil.average(quad.spriteV(0, 0), quad.spriteV(1, 0));
		float nx01 = 0;
		float ny01 = 0;
		float nz01 = 0;
		if (n01) {
			nx01 = MathUtil.average(quad.normalX(0), quad.normalX(1));
			ny01 = MathUtil.average(quad.normalY(0), quad.normalY(1));
			nz01 = MathUtil.average(quad.normalZ(0), quad.normalZ(1));
			float scale = 1 / (float) Math.sqrt(nx01 * nx01 + ny01 * ny01 + nz01 * nz01);
			nx01 *= scale;
			ny01 *= scale;
			nz01 *= scale;
		}

		float x12 = MathUtil.average(quad.x(1), quad.x(2));
		float y12 = MathUtil.average(quad.y(1), quad.y(2));
		float z12 = MathUtil.average(quad.z(1), quad.z(2));
		int c12 = MathUtil.averageColor(quad.spriteColor(1, 0), quad.spriteColor(2, 0));
		int l12 = MathUtil.averageLight(quad.lightmap(1), quad.lightmap(2));
		float u12 = MathUtil.average(quad.spriteU(1, 0), quad.spriteU(2, 0));
		float v12 = MathUtil.average(quad.spriteV(1, 0), quad.spriteV(2, 0));
		float nx12 = 0;
		float ny12 = 0;
		float nz12 = 0;
		if (n12) {
			nx12 = MathUtil.average(quad.normalX(1), quad.normalX(2));
			ny12 = MathUtil.average(quad.normalY(1), quad.normalY(2));
			nz12 = MathUtil.average(quad.normalZ(1), quad.normalZ(2));
			float scale = 1 / (float) Math.sqrt(nx12 * nx12 + ny12 * ny12 + nz12 * nz12);
			nx12 *= scale;
			ny12 *= scale;
			nz12 *= scale;
		}

		float x23 = MathUtil.average(quad.x(2), quad.x(3));
		float y23 = MathUtil.average(quad.y(2), quad.y(3));
		float z23 = MathUtil.average(quad.z(2), quad.z(3));
		int c23 = MathUtil.averageColor(quad.spriteColor(2, 0), quad.spriteColor(3, 0));
		int l23 = MathUtil.averageLight(quad.lightmap(2), quad.lightmap(3));
		float u23 = MathUtil.average(quad.spriteU(2, 0), quad.spriteU(3, 0));
		float v23 = MathUtil.average(quad.spriteV(2, 0), quad.spriteV(3, 0));
		float nx23 = 0;
		float ny23 = 0;
		float nz23 = 0;
		if (n23) {
			nx23 = MathUtil.average(quad.normalX(2), quad.normalX(3));
			ny23 = MathUtil.average(quad.normalY(2), quad.normalY(3));
			nz23 = MathUtil.average(quad.normalZ(2), quad.normalZ(3));
			float scale = 1 / (float) Math.sqrt(nx23 * nx23 + ny23 * ny23 + nz23 * nz23);
			nx23 *= scale;
			ny23 *= scale;
			nz23 *= scale;
		}

		float x30 = MathUtil.average(quad.x(3), quad.x(0));
		float y30 = MathUtil.average(quad.y(3), quad.y(0));
		float z30 = MathUtil.average(quad.z(3), quad.z(0));
		int c30 = MathUtil.averageColor(quad.spriteColor(3, 0), quad.spriteColor(0, 0));
		int l30 = MathUtil.averageLight(quad.lightmap(3), quad.lightmap(0));
		float u30 = MathUtil.average(quad.spriteU(3, 0), quad.spriteU(0, 0));
		float v30 = MathUtil.average(quad.spriteV(3, 0), quad.spriteV(0, 0));
		float nx30 = 0;
		float ny30 = 0;
		float nz30 = 0;
		if (n30) {
			nx30 = MathUtil.average(quad.normalX(3), quad.normalX(0));
			ny30 = MathUtil.average(quad.normalY(3), quad.normalY(0));
			nz30 = MathUtil.average(quad.normalZ(3), quad.normalZ(0));
			float scale = 1 / (float) Math.sqrt(nx30 * nx30 + ny30 * ny30 + nz30 * nz30);
			nx30 *= scale;
			ny30 *= scale;
			nz30 *= scale;
		}

		float x4 = MathUtil.average(quad.x(0), quad.x(2));
		float y4 = MathUtil.average(quad.y(0), quad.y(2));
		float z4 = MathUtil.average(quad.z(0), quad.z(2));
		int c4 = MathUtil.averageColor(quad.spriteColor(0, 0), quad.spriteColor(2, 0));
		int l4 = MathUtil.averageLight(quad.lightmap(0), quad.lightmap(2));
		float u4 = MathUtil.average(quad.spriteU(0, 0), quad.spriteU(2, 0));
		float v4 = MathUtil.average(quad.spriteV(0, 0), quad.spriteV(2, 0));
		float nx4 = 0;
		float ny4 = 0;
		float nz4 = 0;
		if (n4) {
			nx4 = MathUtil.average(quad.normalX(0), quad.normalX(2));
			ny4 = MathUtil.average(quad.normalY(0), quad.normalY(2));
			nz4 = MathUtil.average(quad.normalZ(0), quad.normalZ(2));
			float scale = 1 / (float) Math.sqrt(nx4 * nx4 + ny4 * ny4 + nz4 * nz4);
			nx4 *= scale;
			ny4 *= scale;
			nz4 *= scale;
		}

		//

		// Quad 0
		quad.copyTo(quadEmitter);
		quadEmitter.cullFace(cullFace);
		quadEmitter.material(material);
		quadEmitter.pos(1, x01, y01, z01);
		quadEmitter.pos(2, x4, y4, z4);
		quadEmitter.pos(3, x30, y30, z30);
		quadEmitter.spriteColor(1, 0, c01);
		quadEmitter.spriteColor(2, 0, c4);
		quadEmitter.spriteColor(3, 0, c30);
		quadEmitter.lightmap(1, l01);
		quadEmitter.lightmap(2, l4);
		quadEmitter.lightmap(3, l30);
		quadEmitter.sprite(1, 0, u01, v01);
		quadEmitter.sprite(2, 0, u4, v4);
		quadEmitter.sprite(3, 0, u30, v30);
		if (n01) quadEmitter.normal(1, nx01, ny01, nz01);
		if (n4) quadEmitter.normal(2, nx4, ny4, nz4);
		if (n30) quadEmitter.normal(3, nx30, ny30, nz30);
		QuadUtil.interpolate(quadEmitter, sprite, sprites[getSpriteIndex(quadIndices[0], connections)]);
		quadEmitter.emit();

		// Quad 1
		quad.copyTo(quadEmitter);
		quadEmitter.cullFace(cullFace);
		quadEmitter.material(material);
		quadEmitter.pos(0, x01, y01, z01);
		quadEmitter.pos(2, x12, y12, z12);
		quadEmitter.pos(3, x4, y4, z4);
		quadEmitter.spriteColor(0, 0, c01);
		quadEmitter.spriteColor(2, 0, c12);
		quadEmitter.spriteColor(3, 0, c4);
		quadEmitter.lightmap(0, l01);
		quadEmitter.lightmap(2, l12);
		quadEmitter.lightmap(3, l4);
		quadEmitter.sprite(0, 0, u01, v01);
		quadEmitter.sprite(2, 0, u12, v12);
		quadEmitter.sprite(3, 0, u4, v4);
		if (n01) quadEmitter.normal(0, nx01, ny01, nz01);
		if (n12) quadEmitter.normal(2, nx12, ny12, nz12);
		if (n4) quadEmitter.normal(3, nx4, ny4, nz4);
		QuadUtil.interpolate(quadEmitter, sprite, sprites[getSpriteIndex(quadIndices[1], connections)]);
		quadEmitter.emit();

		// Quad 2
		quad.copyTo(quadEmitter);
		quadEmitter.cullFace(cullFace);
		quadEmitter.material(material);
		quadEmitter.pos(0, x4, y4, z4);
		quadEmitter.pos(1, x12, y12, z12);
		quadEmitter.pos(3, x23, y23, z23);
		quadEmitter.spriteColor(0, 0, c4);
		quadEmitter.spriteColor(1, 0, c12);
		quadEmitter.spriteColor(3, 0, c23);
		quadEmitter.lightmap(0, l4);
		quadEmitter.lightmap(1, l12);
		quadEmitter.lightmap(3, l23);
		quadEmitter.sprite(0, 0, u4, v4);
		quadEmitter.sprite(1, 0, u12, v12);
		quadEmitter.sprite(3, 0, u23, v23);
		if (n4) quadEmitter.normal(0, nx4, ny4, nz4);
		if (n12) quadEmitter.normal(1, nx12, ny12, nz12);
		if (n23) quadEmitter.normal(3, nx23, ny23, nz23);
		QuadUtil.interpolate(quadEmitter, sprite, sprites[getSpriteIndex(quadIndices[2], connections)]);
		quadEmitter.emit();

		// Quad 3
		quad.copyTo(quadEmitter);
		quadEmitter.cullFace(cullFace);
		quadEmitter.material(material);
		quadEmitter.pos(0, x30, y30, z30);
		quadEmitter.pos(1, x4, y4, z4);
		quadEmitter.pos(2, x23, y23, z23);
		quadEmitter.spriteColor(0, 0, c30);
		quadEmitter.spriteColor(1, 0, c4);
		quadEmitter.spriteColor(2, 0, c23);
		quadEmitter.lightmap(0, l30);
		quadEmitter.lightmap(1, l4);
		quadEmitter.lightmap(2, l23);
		quadEmitter.sprite(0, 0, u30, v30);
		quadEmitter.sprite(1, 0, u4, v4);
		quadEmitter.sprite(2, 0, u23, v23);
		if (n30) quadEmitter.normal(0, nx30, ny30, nz30);
		if (n4) quadEmitter.normal(1, nx4, ny4, nz4);
		if (n23) quadEmitter.normal(2, nx23, ny23, nz23);
		QuadUtil.interpolate(quadEmitter, sprite, sprites[getSpriteIndex(quadIndices[3], connections)]);
		quadEmitter.emit();

		//

		context.addMesh(meshBuilder.build());
		return ProcessingResult.ABORT_AND_CANCEL_QUAD;
	}

	/*
	0 - Unconnected
	1 - Fully connected
	2 - Up and down / vertical
	3 - Left and right / horizontal
	4 - Unconnected corners
	 */
	protected int getSpriteIndex(int quadIndex, int connections) {
		int index1 = quadIndex;
		int index2 = (quadIndex + 3) % 4;
		boolean connected1 = ((connections >> index1 * 2) & 1) == 1;
		boolean connected2 = ((connections >> index2 * 2) & 1) == 1;
		if (connected1 && connected2) {
			if (((connections >> (index2 * 2 + 1)) & 1) == 1) {
				return 1;
			}
			return 4;
		}
		int swap = quadIndex % 2;
		if (connected1) { // 0 - h, 1 - v, 2 - h, 3 - v
			return 3 - swap;
		}
		if (connected2) { // 0 - v, 1 - h, 2 - v, 3 - h
			return 2 + swap;
		}
		return 0;
	}

	// TODO
	public static class Factory implements QuadProcessorFactory<CompactConnectingCTMProperties> {
		@Override
		public QuadProcessor createProcessor(CompactConnectingCTMProperties properties, Function<SpriteIdentifier, Sprite> textureGetter) {
			int textureAmount = getTextureAmount(properties);
			List<SpriteIdentifier> spriteIds = properties.getSpriteIds();
			int provided = spriteIds.size();
			int max = provided;
			Sprite[] replacementSprites = null;
			if (provided > textureAmount) {
				Int2IntMap replacementMap = properties.getTileReplacementMap();
				if (replacementMap != null) {
					int replacementTextureAmount = getReplacementTextureAmount(properties);
					replacementSprites = new Sprite[replacementTextureAmount];
					for (Int2IntMap.Entry entry : replacementMap.int2IntEntrySet()) {
						int key = entry.getIntKey();
						if (key < replacementTextureAmount) {
							int value = entry.getIntValue();
							if (value < provided) {
								replacementSprites[key] = textureGetter.apply(spriteIds.get(value));
							} else {
								ContinuityClient.LOGGER.warn("Cannot replace tile " + key + " with tile " + value + " as only " + provided + " tiles were provided in file '" + properties.getId() + "' in pack '" + properties.getPackName() + "'");
							}
						} else {
							ContinuityClient.LOGGER.warn("Cannot replace tile " + key + " as method '" + properties.getMethod() + "' only supports " + replacementTextureAmount + " replacement tiles in file '" + properties.getId() + "' in pack '" + properties.getPackName() + "'");
						}
					}
				} else {
					ContinuityClient.LOGGER.warn("Method '" + properties.getMethod() + "' requires " + textureAmount + " tiles but " + provided + " were provided in file '" + properties.getId() + "' in pack '" + properties.getPackName() + "'");
				}
				max = textureAmount;
			}
			Sprite[] sprites = new Sprite[textureAmount];
			Sprite missingSprite = textureGetter.apply(TextureUtil.MISSING_SPRITE_ID);
			boolean supportsNullSprites = supportsNullSprites(properties);
			for (int i = 0; i < max; i++) {
				Sprite sprite;
				SpriteIdentifier spriteId = spriteIds.get(i);
				if (spriteId.equals(BaseCTMProperties.SPECIAL_SKIP_SPRITE_ID)) {
					sprite = missingSprite;
				} else if (spriteId.equals(BaseCTMProperties.SPECIAL_DEFAULT_SPRITE_ID)) {
					sprite = supportsNullSprites ? null : missingSprite;
				} else {
					sprite = textureGetter.apply(spriteId);
				}
				sprites[i] = sprite;
			}
			if (provided < textureAmount) {
				ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires at least " + textureAmount + " tiles but only " + provided + " were provided in file '" + properties.getId() + "' in pack '" + properties.getPackName() + "'");
				for (int i = provided; i < textureAmount; i++) {
					sprites[i] = missingSprite;
				}
			}
			return createProcessor(properties, sprites, replacementSprites);
		}

		public QuadProcessor createProcessor(CompactConnectingCTMProperties properties, Sprite[] sprites, Sprite[] replacementSprites) {
			return new CompactCTMQuadProcessor(sprites, BaseProcessingPredicate.fromProperties(properties), properties.getConnectionPredicate(), properties.getInnerSeams(), replacementSprites);
		}

		public int getTextureAmount(CompactConnectingCTMProperties properties) {
			return 5;
		}

		public int getReplacementTextureAmount(CompactConnectingCTMProperties properties) {
			return 47;
		}

		public boolean supportsNullSprites(CompactConnectingCTMProperties properties) {
			return false;
		}
	}
}
