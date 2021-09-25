package me.pepperbell.continuity.client.mixinterface;

import net.minecraft.util.Identifier;

public interface ReloadableResourceManagerImplExtension {
	void addRedirect(Identifier from, Identifier to);
}
