package me.pepperbell.continuity.client.handler;

import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public class ClientPlayJoinHandler implements ClientPlayConnectionEvents.Join {
	@Override
	public void onPlayReady(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
		BiomeHolderManager.setup(handler.getRegistryManager());
	}
}
