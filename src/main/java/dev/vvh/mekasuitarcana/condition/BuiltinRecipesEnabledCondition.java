package dev.vvh.mekasuitarcana.condition;

import com.mojang.serialization.MapCodec;
import dev.vvh.mekasuitarcana.MekaSuitArcana;
import dev.vvh.mekasuitarcana.config.ArcanaConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * NeoForge recipe condition that checks whether the mod's built-in crafting recipes are enabled.
 *
 * <p>Allows modpack creators to set {@code enable_builtin_recipes = false} in the server config
 * so all five module recipes are suppressed, enabling custom recipes via KubeJS or CraftTweaker.</p>
 */
public final class BuiltinRecipesEnabledCondition implements ICondition {

    public static final BuiltinRecipesEnabledCondition INSTANCE = new BuiltinRecipesEnabledCondition();
    public static final MapCodec<BuiltinRecipesEnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, MekaSuitArcana.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<BuiltinRecipesEnabledCondition>> BUILTIN_RECIPES =
            CONDITION_CODECS.register("builtin_recipes_enabled", () -> CODEC);

    public static void register(IEventBus modBus) {
        CONDITION_CODECS.register(modBus);
    }

    private BuiltinRecipesEnabledCondition() {}

    @Override
    public boolean test(IContext context) {
        return ArcanaConfig.enableBuiltinRecipes();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "builtin_recipes_enabled";
    }
}