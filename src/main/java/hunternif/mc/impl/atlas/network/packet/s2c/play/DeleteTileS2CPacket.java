package hunternif.mc.impl.atlas.network.packet.s2c.play;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Removes one rescanned atlas tile without changing existing packet formats. */
public class DeleteTileS2CPacket extends ClientboundUnionPacket {
    public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "tile", "delete");
    private final int atlasID, chunkX, chunkZ;
    private final ResourceKey<Level> world;

    public DeleteTileS2CPacket(int atlasID, ResourceKey<Level> world, int chunkX, int chunkZ) {
        super(AntiqueAtlas.instance.channel);
        this.atlasID = atlasID;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public DeleteTileS2CPacket(FriendlyByteBuf buffer) {
        super(buffer, AntiqueAtlas.instance.channel);
        atlasID = buffer.readVarInt();
        world = ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation());
        chunkX = buffer.readVarInt();
        chunkZ = buffer.readVarInt();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(atlasID);
        buffer.writeResourceLocation(world.location());
        buffer.writeVarInt(chunkX);
        buffer.writeVarInt(chunkZ);
    }

    @Override
    public boolean runOnClient(Player player) {
        AntiqueAtlas.tileData.getData(atlasID, player.level()).getWorldData(world).removeTile(chunkX, chunkZ);
        return true;
    }

    @Override
    public ResourceLocation id() { return ID; }
}
