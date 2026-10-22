package hunternif.mc.impl.atlas.client;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.AntiqueAtlasClientSegment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AntiqueAtlas.ID, value = Dist.CLIENT)
public final class ClientConnectionEvents {
    @SubscribeEvent
    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        resetLocalCaches();
        boolean remote = !event.getConnection().isMemoryConnection();
        AntiqueAtlas.globalMarkersData.onClientConnectedToServer(remote);
        if (remote) AntiqueAtlas.globalTileData.clearClientCache();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetLocalCaches();
    }

    private static void resetLocalCaches() {
        AntiqueAtlas.tileData.onClientConnectedToServer(true);
        AntiqueAtlas.markersData.onClientConnectedToServer(true);
        AntiqueAtlasClientSegment.resetAtlasGUI();
    }
}
