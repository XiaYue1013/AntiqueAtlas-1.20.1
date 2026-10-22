package hunternif.mc.impl.atlas;

import hunternif.mc.impl.atlas.core.*;
import hunternif.mc.impl.atlas.item.*;
import hunternif.mc.impl.atlas.marker.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AntiqueAtlas.ID)
@PrefixGameTestTemplate(false)
public final class AtlasRegressionTests {
    private static final ResourceLocation PLAINS = new ResourceLocation("minecraft", "plains_low");

    private static CraftingMenu menu(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        return new CraftingMenu(1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO)));
    }

    @GameTest(template = "empty")
    public static void survivalCraftingAndCloning(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        for (var book : new net.minecraft.world.item.Item[]{Items.BOOK, Items.WRITABLE_BOOK}) {
            var menu = menu(helper);
            menu.getSlot(1).set(new ItemStack(book));
            menu.getSlot(2).set(new ItemStack(Items.COMPASS));
            helper.assertTrue(menu.getSlot(0).getItem().is(AntiqueAtlasItems.Items.EMPTY_ATLAS), "Atlas recipe must load through RecipeManager");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().is(AntiqueAtlasItems.Items.EMPTY_ATLAS), "Survival result must be collectible");
            helper.assertTrue(menu.getSlot(1).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty(), "Ingredients must be consumed");
        }
        var menu = menu(helper);
        var original = AntiqueAtlasItems.getAtlasFromId(123);
        original.setHoverName(Component.literal("Regression Atlas"));
        original.getOrCreateTag().putString("compatibility_test", "preserve");
        menu.getSlot(1).set(original.copy());
        menu.getSlot(2).set(new ItemStack(AntiqueAtlasItems.Items.EMPTY_ATLAS));
        menu.getSlot(3).set(new ItemStack(AntiqueAtlasItems.Items.EMPTY_ATLAS));
        helper.assertTrue(menu.getSlot(0).getItem().getCount() == 3, "Two blanks must produce three copies");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        var result = menu.getCarried();
        helper.assertTrue(result.getCount() == 3 && AtlasItem.getAtlasID(result) == 123, "Clone result must preserve ID and count");
        helper.assertTrue(original.getTag().equals(result.getTag()), "Cloning must preserve full NBT");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void combiningPreservesData(GameTestHelper helper) {
        var world = helper.getLevel();
        int first = AntiqueAtlas.getAtlasIdData(world).getNextAtlasId();
        int second = AntiqueAtlas.getAtlasIdData(world).getNextAtlasId();
        AntiqueAtlas.tileData.getData(first, world).setTile(Level.OVERWORLD, -17, 20, PLAINS);
        AntiqueAtlas.tileData.getData(second, world).setTile(Level.NETHER, 25, -34, PLAINS);
        AntiqueAtlas.markersData.getMarkersData(first, world).createAndSaveMarker(AntiqueAtlas.id("red_x_small"), Level.OVERWORLD, -25, 300, true, Component.literal("A"));
        AntiqueAtlas.markersData.getMarkersData(second, world).createAndSaveMarker(AntiqueAtlas.id("red_x_small"), Level.NETHER, 85, -44, true, Component.literal("B"));
        var menu = menu(helper);
        menu.getSlot(1).set(AntiqueAtlasItems.getAtlasFromId(first));
        menu.getSlot(2).set(AntiqueAtlasItems.getAtlasFromId(second));
        menu.getSlot(3).set(new ItemStack(Items.DIAMOND));
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "Combining must reject unrelated ingredients");
        menu.getSlot(3).set(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, FakePlayerFactory.getMinecraft(world));
        var result = menu.getCarried();
        int id = AtlasItem.getAtlasID(result);
        helper.assertTrue(id != first && id != second, "Combining must allocate an independent atlas");
        var data = AntiqueAtlas.tileData.getData(id, world);
        helper.assertTrue(PLAINS.equals(data.getWorldData(Level.OVERWORLD).getTile(-17, 20)), "Overworld tiles missing");
        helper.assertTrue(PLAINS.equals(data.getWorldData(Level.NETHER).getTile(25, -34)), "Nether tiles missing");
        var markers = AntiqueAtlas.markersData.getMarkersData(id, world);
        helper.assertTrue(markers.getMarkersInWorld(Level.OVERWORLD).size() == 1 && markers.getMarkersInWorld(Level.NETHER).size() == 1, "Markers must survive combining");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void savedDataMigrationRoundTrip(GameTestHelper helper) {
        var canonical = new AtlasData();
        canonical.setTile(Level.OVERWORLD, -17, 25, PLAINS);
        var legacy = new AtlasData();
        legacy.setTile(Level.NETHER, -30, -40, PLAINS);
        canonical.importLegacyDimension(Level.NETHER, legacy);
        var restored = AtlasData.fromNbt(canonical.save(new CompoundTag()));
        helper.assertTrue(canonical.equals(restored), "Tile data must survive NBT round-trip");
        helper.assertTrue(restored.hasImportedDimension(Level.NETHER), "Migration must be recorded with data");
        var a = new MarkersData();
        var b = new MarkersData();
        a.createAndSaveMarker(AntiqueAtlas.id("red_x_small"), Level.OVERWORLD, -1, -1, true, Component.literal("overworld"));
        b.createAndSaveMarker(AntiqueAtlas.id("red_x_small"), Level.NETHER, 1, 1, true, Component.literal("nether"));
        a.importLegacyDimension(Level.NETHER, b);
        var copy = MarkersData.fromNbt(a.save(new CompoundTag()));
        copy.importLegacyDimension(Level.NETHER, b);
        helper.assertTrue(copy.getMarkersInWorld(Level.OVERWORLD).size() == 1 && copy.getMarkersInWorld(Level.NETHER).size() == 1, "Colliding legacy IDs must survive without repeated imports");
        int next = copy.createAndSaveMarker(AntiqueAtlas.id("red_x_small"), Level.END, 0, 0, true, Component.literal("new")).getId();
        helper.assertTrue(next > 2, "IDs must advance after migration and reload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sharedStorageAcrossDimensions(GameTestHelper helper) {
        var world = helper.getLevel();
        var overworld = world.getServer().overworld();
        var nether = world.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether must exist");
        int id = AntiqueAtlas.getAtlasIdData(overworld).getNextAtlasId();
        helper.assertTrue(AntiqueAtlas.tileData.getData(id, overworld) == AntiqueAtlas.tileData.getData(id, nether), "Tiles must use one canonical SavedData");
        helper.assertTrue(AntiqueAtlas.markersData.getMarkersData(id, overworld) == AntiqueAtlas.markersData.getMarkersData(id, nether), "Markers must use one canonical SavedData");
        helper.assertTrue(AntiqueAtlas.getAtlasIdData(overworld) == AntiqueAtlas.getAtlasIdData(nether), "Atlas IDs must use one allocator");
        var handler = new MarkersDataHandler();
        helper.assertTrue(handler.getMarkersDataCached(id, Level.OVERWORLD) == handler.getMarkersDataCached(id, Level.NETHER), "Client marker cache must span dimensions");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void negativeMarkersAndReducedLimit(GameTestHelper helper) {
        var data = new MarkersData();
        var marker = data.createAndSaveMarker(AntiqueAtlas.id("red_x_small"), Level.OVERWORLD,
                -1, -129, true, Component.literal("negative"));
        helper.assertTrue(data.getMarkersAtChunk(Level.OVERWORLD, -1, -2).contains(marker), "Negative marker chunks must use floor division");
        int limit = AntiqueAtlas.CONFIG.markerLimit;
        try {
            AntiqueAtlas.CONFIG.markerLimit = 0;
            var restored = MarkersData.fromNbt(data.save(new CompoundTag()));
            helper.assertTrue(restored.getMarkersInWorld(Level.OVERWORLD).size() == 1, "A reduced limit must never discard saved markers");
            restored.removeMarker(marker.getId());
            helper.assertTrue(restored.isEmpty(), "Marker deletion must update both indexes");
        } finally { AntiqueAtlas.CONFIG.markerLimit = limit; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scanningUnloadedChunksDoesNotLoadThem(GameTestHelper helper) {
        var world = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(world);
        player.setPos(1600000, 100, 1600000);
        int loaded = world.getChunkSource().getLoadedChunksCount();
        float interval = AntiqueAtlas.CONFIG.newScanInterval;
        try {
            AntiqueAtlas.CONFIG.newScanInterval = 0;
            var updates = AntiqueAtlas.worldScanner.updateAtlasAroundPlayer(new AtlasData(), player);
            helper.assertTrue(updates.isEmpty(), "Unloaded terrain must be skipped");
            helper.assertTrue(loaded == world.getChunkSource().getLoadedChunksCount(), "Scanning must not force chunk loads");
        } finally { AntiqueAtlas.CONFIG.newScanInterval = interval; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryShiftCombiningDoesNotDuplicateMarkers(GameTestHelper helper) {
        var world = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(world);
        player.getInventory().clearContent();
        int id = AntiqueAtlas.getAtlasIdData(world).getNextAtlasId();
        AntiqueAtlas.markersData.getMarkersData(id, world).createAndSaveMarker(AntiqueAtlas.id("red_x_small"),
                Level.OVERWORLD, 20, 40, true, Component.literal("one marker"));
        var inventory = new InventoryMenu(player.getInventory(), false, player);
        inventory.getSlot(1).set(AntiqueAtlasItems.getAtlasFromId(id));
        inventory.getSlot(2).set(AntiqueAtlasItems.getAtlasFromId(id));
        inventory.clicked(0, 0, ClickType.QUICK_MOVE, player);
        var result = player.getInventory().items.stream().filter(stack -> stack.is(AntiqueAtlasItems.Items.ATLAS))
                .findFirst().orElseThrow();
        result.getItem().inventoryTick(result, world, player, 0, false);
        int combined = AtlasItem.getAtlasID(result);
        helper.assertTrue(combined != id, "Inventory shift-click must create independent map data");
        helper.assertTrue(!result.getOrCreateTag().contains(RecipeAtlasCombining.COMBINE_SOURCES), "Result must be finalized");
        helper.assertTrue(AntiqueAtlas.markersData.getMarkersData(combined, world).getMarkersInWorld(Level.OVERWORLD).size() == 1,
                "Combining copies of one atlas must not duplicate markers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deletedTilesRoundTripThroughPacket(GameTestHelper helper) {
        var world = helper.getLevel();
        int id = AntiqueAtlas.getAtlasIdData(world).getNextAtlasId();
        var data = AntiqueAtlas.tileData.getData(id, world);
        data.setTile(Level.NETHER, -17, 20, PLAINS);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            new hunternif.mc.impl.atlas.network.packet.s2c.play.DeleteTileS2CPacket(id, Level.NETHER, -17, 20).encode(buffer);
            new hunternif.mc.impl.atlas.network.packet.s2c.play.DeleteTileS2CPacket(buffer)
                    .runOnClient(FakePlayerFactory.getMinecraft(world));
        } finally { buffer.release(); }
        var restored = AtlasData.fromNbt(data.save(new CompoundTag()));
        helper.assertFalse(restored.getWorldData(Level.NETHER).hasTileAt(-17, 20), "Deleted tiles must stay removed after save/reload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemlessScanningSendsUpdates(GameTestHelper helper) {
        var world = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(world,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "AtlasItemlessTest"));
        class RecordingConnection extends net.minecraft.network.Connection {
            int sent;
            RecordingConnection() { super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND); }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { sent++; }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) { sent++; }
        }
        var connection = new RecordingConnection();
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(world.getServer(), connection, player);
        player.setPos(helper.absolutePos(net.minecraft.core.BlockPos.ZERO).getX(), 100,
                helper.absolutePos(net.minecraft.core.BlockPos.ZERO).getZ());
        boolean itemNeeded = AntiqueAtlas.CONFIG.itemNeeded;
        float interval = AntiqueAtlas.CONFIG.newScanInterval;
        int radius = AntiqueAtlas.CONFIG.scanRadius;
        try {
            AntiqueAtlas.CONFIG.itemNeeded = false;
            AntiqueAtlas.CONFIG.newScanInterval = 0;
            AntiqueAtlas.CONFIG.scanRadius = 0;
            PlayerEventHandler.onPlayerTick(player);
            var data = AntiqueAtlas.tileData.getData(player.getUUID().hashCode(), world);
            helper.assertFalse(data.getSeenChunksInWorld(world.dimension()).isEmpty(), "Itemless scanning must find loaded terrain");
            helper.assertTrue(connection.sent > 0, "Itemless scanning must emit a clientbound update");
        } finally {
            AntiqueAtlas.CONFIG.itemNeeded = itemNeeded;
            AntiqueAtlas.CONFIG.newScanInterval = interval;
            AntiqueAtlas.CONFIG.scanRadius = radius;
        }
        helper.succeed();
    }
}

