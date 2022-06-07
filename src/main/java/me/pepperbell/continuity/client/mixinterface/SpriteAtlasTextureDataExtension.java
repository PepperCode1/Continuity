package me.pepperbell.continuity.client.mixinterface;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

public interface SpriteAtlasTextureDataExtension {
	@Nullable
	Map<Identifier, Identifier> getEmissiveIdMap();

	void setEmissiveIdMap(Map<Identifier, Identifier> map);
}
