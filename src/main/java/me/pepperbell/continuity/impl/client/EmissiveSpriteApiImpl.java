package me.pepperbell.continuity.impl.client;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.mixinterface.SpriteExtension;
import net.minecraft.client.texture.Sprite;

public final class EmissiveSpriteApiImpl implements EmissiveSpriteApi {
	public static final EmissiveSpriteApiImpl INSTANCE = new EmissiveSpriteApiImpl();

	@Override
	@Nullable
	public Sprite getEmissiveSprite(Sprite sprite) {
		return ((SpriteExtension) sprite).getEmissiveSprite();
	}
}
