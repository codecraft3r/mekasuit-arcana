package dev.vvh.mekasuitarcana.runtime;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import dev.vvh.mekasuitarcana.energy.ArcanaEnergy;
import dev.vvh.mekasuitarcana.registry.ArcanaModules;
import dev.vvh.mekasuitarcana.spell.ArcanaCarrier;
import dev.vvh.mekasuitarcana.spell.SpellSchool;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import mekanism.api.gear.IModuleHelper;
import mekanism.common.attachments.containers.energy.AttachedEnergy;
import mekanism.common.content.gear.ModuleContainer;
import mekanism.common.registries.MekanismDataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Server-thread timing proof for the production ArcanaRuntime path.
 *
 * <p>This intentionally uses a real FakePlayer and native Iron's timers. It does not
 * claim connected-client or rendered-tooltip coverage.</p>
 */
@EventBusSubscriber(modid = "mekasuitarcana", bus = EventBusSubscriber.Bus.GAME)
public final class RuntimeTimingVerification {
    private static final String COOLDOWN_ID = "mekasuitarcana:arcana_timing_probe";

    private RuntimeTimingVerification() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("arcana_timing_test")
                .requires(source -> source.hasPermission(2))
                .executes(context -> run(context.getSource())));
    }

    private static int run(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        Check[] checks = {
                checked("focus_school_charge", () -> focusSchoolCharge(server)),
                checked("cooldown_reduction_zero", () -> cooldownReductionZero(server)),
                checked("cooldown_reduction_partial", () -> cooldownReductionPartial(server)),
                checked("canceled_mana_refund", () -> canceledManaRefund(server)),
                checked("casting_stabilization_instant", () -> castingStabilizationInstant(server)),
                checked("casting_stabilization_concentration", () -> castingStabilizationConcentration(server)),
                checked("movement_binary", () -> movementBinary(server)),
                checked("empty_dry", () -> emptyDry(server))
        };
        boolean allPassed = true;
        for (Check check : checks) {
            allPassed &= check.passed();
            String marker = (check.passed() ? "PASS " : "FAIL ") + "arcana_timing_test." + check.name()
                    + " " + check.detail();
            source.sendSuccess(() -> Component.literal(marker), true);
        }
        String summary = (allPassed ? "PASS " : "FAIL ") + "arcana_timing_test.all";
        source.sendSuccess(() -> Component.literal(summary), true);
        return allPassed ? 1 : 0;
    }

    private static Check checked(String name, Supplier<Check> test) {
        try {
            Check result = test.get();
            return new Check(name, result.passed(), result.detail());
        } catch (Throwable failure) {
            String message = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
            return new Check(name, false, message);
        }
    }

    private static Check focusSchoolCharge(MinecraftServer server) {
        FakePlayer matching = player(server, "focus_matching");
        ItemStack matchingBody = body(matching, 4_000_000L, install(ArcanaModules.FOCUS, 4));
        matching.setItemSlot(EquipmentSlot.CHEST, matchingBody);
        ArcanaRuntime.refresh(matching);
        double fireBase = attributeWithoutCarrier(server, "focus_fire_base", SpellSchool.FIRE.spellPowerAttribute());
        double firePowered = matching.getAttributeValue(SpellSchool.FIRE.spellPowerAttribute());
        double icePowered = matching.getAttributeValue(SpellSchool.ICE.spellPowerAttribute());
        long before = ArcanaEnergy.stored(matchingBody);
        NeoForge.EVENT_BUS.post(new SpellOnCastEvent(matching, "irons_spellbooks:magic_arrow", 1, 0,
                SchoolRegistry.FIRE.get(), CastSource.SPELLBOOK));
        long after = ArcanaEnergy.stored(matchingBody);

        FakePlayer nonmatching = player(server, "focus_nonmatching");
        ItemStack nonmatchingBody = body(nonmatching, 4_000_000L, install(ArcanaModules.FOCUS, 4));
        nonmatching.setItemSlot(EquipmentSlot.CHEST, nonmatchingBody);
        ArcanaRuntime.refresh(nonmatching);
        double nonmatchingBefore = ArcanaEnergy.stored(nonmatchingBody);
        NeoForge.EVENT_BUS.post(new SpellOnCastEvent(nonmatching, "irons_spellbooks:ice_spikes", 1, 0,
                SchoolRegistry.ICE.get(), CastSource.SPELLBOOK));
        double nonmatchingFireAfter = nonmatching.getAttributeValue(SpellSchool.FIRE.spellPowerAttribute());
        long nonmatchingAfter = ArcanaEnergy.stored(nonmatchingBody);
        boolean passed = close(firePowered, fireBase + 4.0D)
                && close(icePowered, attributeWithoutCarrier(server, "focus_ice_base", SpellSchool.ICE.spellPowerAttribute()))
                && after < before
                && nonmatchingAfter == nonmatchingBefore;
        return new Check("", passed, "matchingFE=" + before + "->" + after
                + ", nonmatchingFE=" + nonmatchingBefore + "->" + nonmatchingAfter
                + ", fire=" + fireBase + "->" + firePowered + "->" + nonmatchingFireAfter
                + ", ice=" + icePowered);
    }

    private static Check cooldownReductionZero(MinecraftServer server) {
        FakePlayer player = player(server, "cooldown_zero");
        AbstractSpell spell = findLongSpell();
        if (spell == null) return new Check("", false, "no registered LONG spell found");
        ItemStack body = body(player, 4_000_000L, install(ArcanaModules.COOLDOWN_REDUCTION, 4));
        player.setItemSlot(EquipmentSlot.CHEST, body);
        ArcanaRuntime.refresh(player);
        int baseline = io.redspace.ironsspellbooks.capabilities.magic.MagicManager
                .getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);
        var event = new io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent.Pre(
                baseline, spell, player, CastSource.SPELLBOOK);
        long beforeEnergy = ArcanaEnergy.stored(body);
        NeoForge.EVENT_BUS.post(event);
        long afterEnergy = ArcanaEnergy.stored(body);
        boolean passed = event.isCanceled() && event.getEffectiveCooldown() == 0 && afterEnergy < beforeEnergy;
        return new Check("", passed, "baseline=" + baseline + ", effective=" + event.getEffectiveCooldown()
                + ", canceled=" + event.isCanceled() + ", FE=" + beforeEnergy + "->" + afterEnergy);
    }

    private static Check cooldownReductionPartial(MinecraftServer server) {
        FakePlayer player = player(server, "cooldown_partial");
        AbstractSpell spell = findLongSpell();
        if (spell == null) return new Check("", false, "no registered LONG spell found");
        ItemStack body = body(player, 4_000_000L, install(ArcanaModules.COOLDOWN_REDUCTION, 2));
        player.setItemSlot(EquipmentSlot.CHEST, body);
        ArcanaRuntime.refresh(player);
        int baseline = io.redspace.ironsspellbooks.capabilities.magic.MagicManager
                .getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);
        var event = new io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent.Pre(
                baseline, spell, player, CastSource.SPELLBOOK);
        long beforeEnergy = ArcanaEnergy.stored(body);
        NeoForge.EVENT_BUS.post(event);
        long afterEnergy = ArcanaEnergy.stored(body);
        int expected = (int) Math.round(baseline * 0.5D);
        boolean passed = !event.isCanceled() && event.getEffectiveCooldown() == expected && afterEnergy < beforeEnergy;
        return new Check("", passed, "baseline=" + baseline + ", effective=" + event.getEffectiveCooldown()
                + ", expected=" + expected + ", FE=" + beforeEnergy + "->" + afterEnergy);
    }

    private static Check canceledManaRefund(MinecraftServer server) {
        FakePlayer player = player(server, "mana_refund");
        ItemStack body = body(player, 4_000_000L, install(ArcanaModules.MANA_CONVERSION, 1));
        player.setItemSlot(EquipmentSlot.CHEST, body);
        ArcanaRuntime.refresh(player);
        MagicData magic = magic(player);
        magic.setMana(0);
        long before = ArcanaEnergy.stored(body);
        java.util.function.Consumer<io.redspace.ironsspellbooks.api.events.ChangeManaEvent> cancel = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            ArcanaRuntime.tick(player);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        long after = ArcanaEnergy.stored(body);
        return new Check("", before == after && magic.getMana() == 0,
                "FE=" + before + "->" + after + ", mana=" + magic.getMana());
    }

    private static Check castingStabilizationInstant(MinecraftServer server) {
        FakePlayer player = player(server, "cast_instant");
        ItemStack body = body(player, 4_000_000L, install(ArcanaModules.CASTING_STABILIZATION, 4));
        player.setItemSlot(EquipmentSlot.CHEST, body);
        ArcanaRuntime.refresh(player);
        AbstractSpell spell = findLongSpell();
        if (spell == null) return new Check("", false, "no registered LONG spell found");
        int level = Math.max(spell.getMinLevel(), 1);
        MagicData magic = magic(player);
        magic.initiateCast(spell, level, 100, CastSource.SPELLBOOK, "mainhand");
        int before = magic.getCastDurationRemaining();
        long beforeEnergy = ArcanaEnergy.stored(body);
        ArcanaRuntime.tick(player);
        int after = magic.getCastDurationRemaining();
        long afterEnergy = ArcanaEnergy.stored(body);
        boolean passed = before == 100 && after == 0 && afterEnergy < beforeEnergy;
        return new Check("", passed, "duration=" + before + "->" + after + ", FE=" + beforeEnergy + "->" + afterEnergy);
    }

    private static Check castingStabilizationConcentration(MinecraftServer server) {
        FakePlayer player = player(server, "cast_concentration");
        ItemStack body = body(player, 4_000_000L, install(ArcanaModules.CASTING_STABILIZATION, 1));
        player.setItemSlot(EquipmentSlot.CHEST, body);
        ArcanaRuntime.refresh(player);
        AbstractSpell spell = findLongSpell();
        if (spell == null) return new Check("", false, "no registered LONG spell found");
        int level = Math.max(spell.getMinLevel(), 1);
        MagicData magic = magic(player);
        magic.initiateCast(spell, level, 100, CastSource.SPELLBOOK, "mainhand");
        net.minecraft.world.damagesource.DamageSource source = server.overworld().damageSources().generic();
        net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event =
                new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player,
                        new net.neoforged.neoforge.common.damagesource.DamageContainer(source, 5.0F));
        NeoForge.EVENT_BUS.post(event);
        boolean marked = magic.popMarkedPoison();
        return new Check("", marked && magic.isCasting(), "markedPoison=" + marked + ", casting=" + magic.isCasting());
    }

    private static Check movementBinary(MinecraftServer server) {
        double empty = attributeWithoutCarrier(server, "movement_empty", AttributeRegistry.CASTING_MOVESPEED);
        FakePlayer one = player(server, "movement_one");
        ItemStack oneBody = body(one, 4_000_000L, install(ArcanaModules.CASTING_STABILIZATION, 1));
        one.setItemSlot(EquipmentSlot.CHEST, oneBody);
        ArcanaRuntime.refresh(one);
        double oneValue = one.getAttributeValue(AttributeRegistry.CASTING_MOVESPEED);
        FakePlayer four = player(server, "movement_four");
        ItemStack fourBody = body(four, 4_000_000L, install(ArcanaModules.CASTING_STABILIZATION, 4));
        four.setItemSlot(EquipmentSlot.CHEST, fourBody);
        ArcanaRuntime.refresh(four);
        double fourValue = four.getAttributeValue(AttributeRegistry.CASTING_MOVESPEED);
        boolean passed = finite(empty) && finite(oneValue) && finite(fourValue)
                && oneValue > empty && close(oneValue, fourValue);
        return new Check("", passed, "movement=" + empty + "->" + oneValue + "/" + fourValue);
    }

    private static Check emptyDry(MinecraftServer server) {
        FakePlayer player = player(server, "empty_dry");
        double before = player.getAttributeValue(AttributeRegistry.CASTING_MOVESPEED);
        ArcanaRuntime.refresh(player);
        ArcanaRuntime.tick(player);
        double after = player.getAttributeValue(AttributeRegistry.CASTING_MOVESPEED);
        boolean passed = finite(before) && finite(after) && close(before, after)
                && ArcanaEnergy.stored(player.getItemBySlot(EquipmentSlot.CHEST)) == 0L;
        return new Check("", passed, "movement=" + before + "->" + after + ", FE=0");
    }

    private static double attributeWithoutCarrier(MinecraftServer server, String name, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        FakePlayer player = player(server, name);
        return player.getAttributeValue(attribute);
    }

    private static FakePlayer player(MinecraftServer server, String name) {
        FakePlayer player = FakePlayerFactory.get(server.overworld(),
                new GameProfile(UUID.randomUUID(), "ArcanaTiming-" + name));
        if (player.getHealth() <= 0.0F) player.setHealth(player.getMaxHealth());
        magic(player);
        return player;
    }

    private static MagicData magic(ServerPlayer player) {
        MagicData magic = MagicData.getPlayerMagicData(player);
        magic.setServerPlayer(player);
        if (magic.getSyncedData() == null) {
            magic.setSyncedData(new io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData(player));
        }
        return magic;
    }

    private static int cooldownRemaining(MagicData magic) {
        return magic.getPlayerCooldowns().getSpellCooldowns().get(COOLDOWN_ID).getCooldownRemaining();
    }

    private static AbstractSpell findLongSpell() {
        for (String id : List.of("irons_spellbooks:magic_arrow", "irons_spellbooks:fireball",
                "irons_spellbooks:ice_spikes", "irons_spellbooks:fire_breath")) {
            AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(id));
            if (spell != null && spell.getCastType() == CastType.LONG) return spell;
        }
        return null;
    }

    private static ItemStack body(ServerPlayer player, long joules, ModuleInstall... installs) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:mekasuit_bodyarmor")));
        ModuleContainer container = (ModuleContainer) IModuleHelper.INSTANCE.getModuleContainer(stack);
        if (container == null) throw new IllegalStateException("body has no native module container");
        for (ModuleInstall install : installs) {
            addModule(container, player, stack, install.module(), install.units());
            // addModule writes the stack; the previous container is immutable/stale.
            container = (ModuleContainer) IModuleHelper.INSTANCE.getModuleContainer(stack);
            if (container == null) throw new IllegalStateException("module container disappeared after add");
        }
        stack.set(MekanismDataComponents.ATTACHED_ENERGY.get(), new AttachedEnergy(List.of(joules)));
        return stack;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addModule(ModuleContainer ignored, ServerPlayer player, ItemStack stack,
            Holder<?> module, int units) {
        ModuleContainer current = (ModuleContainer) IModuleHelper.INSTANCE.getModuleContainer(stack);
        current.addModule(player.registryAccess(), stack, (Holder) module, units);
    }

    private static ModuleInstall install(Holder<?> module, int units) {
        return new ModuleInstall(module, units);
    }

    private static boolean close(double left, double right) {
        return finite(left) && finite(right) && Math.abs(left - right) < 0.0001D;
    }

    private static boolean finite(double value) {
        return Double.isFinite(value);
    }

    private record ModuleInstall(Holder<?> module, int units) {}
    private record Check(String name, boolean passed, String detail) {}
}
