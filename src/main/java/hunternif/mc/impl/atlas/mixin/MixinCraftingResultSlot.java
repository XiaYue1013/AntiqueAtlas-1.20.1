package hunternif.mc.impl.atlas.mixin;

import hunternif.mc.impl.atlas.item.AtlasItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.crafting.RecipeHolder;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ResultSlot.class)
public class MixinCraftingResultSlot extends Slot {
    @Final
    @Shadow
    private CraftingContainer craftSlots;
    @Final
    @Shadow
    private Player player;

    public MixinCraftingResultSlot(Container inventory_1, int int_1, int int_2, int int_3) {
        super(inventory_1, int_1, int_2, int_3);
    }

    @Inject(at = @At("HEAD"), method = "checkTakeAchievements")
    protected void onCrafted(ItemStack stack, final CallbackInfo info) {
        AtlasItem.finishCombining(stack, this.player.level());
    }
}
