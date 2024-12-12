package me.pepperbell.continuity.neoforge;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.config.ContinuityConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ContinuityClient.ID, dist = Dist.CLIENT)
public class ContinuityNeoForge {

    public ContinuityNeoForge(IEventBus eventBus, ModContainer container) {
        if (!FMLEnvironment.dist.isClient()) return;

        ContinuityClient continuityClient = new ContinuityClient();
        continuityClient.onInitializeClient();

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (ignored, parent) -> new ContinuityConfigScreen(parent, ContinuityConfig.INSTANCE));
    }
}
