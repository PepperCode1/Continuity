package me.pepperbell.continuity.client.resource;

import me.pepperbell.continuity.client.model.CtmBlockStateModel;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BlockStateModel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.util.Identifier;

public class ModelWrappingHandler {
	@Nullable
	private static volatile ModelWrappingHandler instance;

	private final boolean wrapCtm;
	private final boolean wrapEmissive;

	private ModelWrappingHandler(boolean wrapCtm, boolean wrapEmissive) {
		this.wrapCtm = wrapCtm;
		this.wrapEmissive = wrapEmissive;
	}

	@Nullable
	public static ModelWrappingHandler getInstance() {
		return instance;
	}

	public static void setInstance(boolean wrapCtm, boolean wrapEmissive) {
		if (!wrapCtm && !wrapEmissive) {
			return;
		}
		instance = new ModelWrappingHandler(wrapCtm, wrapEmissive);
	}

	public static void resetInstance() {
		instance = null;
	}

	public ItemModel wrapItem(ItemModel model, Identifier id) {
//		if (!id.equals(MissingModel.ID)) {
//			if (wrapEmissive) {
//				model = new EmissiveBakedModel(model);
//			}
//		}
		return model;
	}

	public BlockStateModel wrapBlock(BlockStateModel model) {
		if (wrapCtm) {
			model = new CtmBlockStateModel(model);
		}
//		if (wrapEmissive) {
//			model = new EmissiveBakedModel(model);
//		}
		return model;
	}

	@ApiStatus.Internal
	public static void init() {
		ModelLoadingPlugin.register(pluginCtx -> {
			pluginCtx.modifyItemModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, ctx) -> {
				ModelWrappingHandler wrappingHandler = getInstance();
				if (wrappingHandler != null) {
					return wrappingHandler.wrapItem(model, ctx.itemId());
				}
				return model;
			});
			pluginCtx.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, ctx) -> {
				ModelWrappingHandler wrappingHandler = getInstance();
				if (wrappingHandler != null) {
					return wrappingHandler.wrapBlock(model);
				}
				return model;
			});
		});
	}
}
