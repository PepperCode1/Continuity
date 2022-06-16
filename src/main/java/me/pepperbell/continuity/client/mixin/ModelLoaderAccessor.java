package me.pepperbell.continuity.client.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.util.Identifier;

@Mixin(ModelLoader.class)
public interface ModelLoaderAccessor {
	@Accessor("unbakedModels")
	Map<Identifier, UnbakedModel> getUnbakedModels();
}
