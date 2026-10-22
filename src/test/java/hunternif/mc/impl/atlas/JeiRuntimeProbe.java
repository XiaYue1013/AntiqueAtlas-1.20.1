package hunternif.mc.impl.atlas;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class JeiRuntimeProbe implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() { return AntiqueAtlas.id("regression_probe"); }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var recipes = runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get()
                .map(recipe -> recipe.getId().toString()).filter(id -> id.startsWith("antiqueatlas:" )).toList();
        AntiqueAtlas.LOG.info("RUNTIME_TEST JEI visible crafting recipes={}", recipes);
        if (!recipes.contains("antiqueatlas:empty_atlas") || !recipes.contains("antiqueatlas:empty_atlas_new")) {
            throw new IllegalStateException("JEI is missing Atlas crafting recipes");
        }
    }
}
