package hunternif.mc.impl.atlas.core;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.network.packet.s2c.play.DimensionUpdateS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PlayerEventHandler {
    public static void onPlayerLogin(ServerPlayer player) {
        Level world = player.level();
        int atlasID = player.getUUID().hashCode();

        AtlasData data = AntiqueAtlas.tileData.getData(atlasID, world);
        // On the player join send the map from the server to the client:
        if (!data.isEmpty()) {
            data.syncToPlayer(atlasID, player);
        }

        // Same thing with the local markers:
        MarkersData markers = AntiqueAtlas.markersData.getMarkersData(atlasID, world);
        if (!markers.isEmpty()) {
            markers.syncToPlayer(atlasID, player);
        }
    }

    public static void onPlayerTick(Player player) {
        if (!AntiqueAtlas.CONFIG.itemNeeded && player instanceof ServerPlayer serverPlayer) {
            AtlasData data = AntiqueAtlas.tileData.getData(
                    player.getUUID().hashCode(), player.level());

            var updates = AntiqueAtlas.worldScanner.updateAtlasAroundPlayer(data, player);
            if (!updates.isEmpty()) {
                DimensionUpdateS2CPacket.sendUpdates(player.getUUID().hashCode(),
                        player.level().dimension(), updates, serverPlayer);
            }
        }
    }
}
