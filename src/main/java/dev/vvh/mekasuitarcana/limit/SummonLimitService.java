package dev.vvh.mekasuitarcana.limit;

import dev.vvh.mekasuitarcana.config.ArcanaConfig;
import io.redspace.ironsspellbooks.api.events.SetSummonOwnerEvent;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;
import io.redspace.ironsspellbooks.entity.spells.scapegoat.ScapegoatEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Enforces per-player caps on concurrent Black Holes and summoned entities to prevent PC performance degradation.
 */
public final class SummonLimitService {

    private static final SummonLimitTracker BLACK_HOLES = new SummonLimitTracker();
    private static final SummonLimitTracker SUMMONS = new SummonLimitTracker();

    private SummonLimitService() {}

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.NORMAL, SummonLimitService::onEntityJoinLevel);
        bus.addListener(EventPriority.NORMAL, SummonLimitService::onSetSummonOwner);
        bus.addListener(SummonLimitService::onPlayerLogout);
        bus.addListener(SummonLimitService::onServerStopping);
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof BlackHole blackHole) {
            if (blackHole.getOwner() instanceof ServerPlayer player) {
                handleBlackHole(player, blackHole);
            }
        } else if (entity instanceof ScapegoatEntity scapegoat) {
            if (scapegoat.getOwner() instanceof ServerPlayer player) {
                handleSummon(player, scapegoat);
            }
        }
    }

    public static void onSetSummonOwner(SetSummonOwnerEvent event) {
        if (event.getOwner() instanceof ServerPlayer player) {
            Entity summon = event.getSummon();
            if (summon != null && !summon.isRemoved() && !(summon instanceof Projectile)) {
                handleSummon(player, summon);
            }
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerUuid = event.getEntity().getUUID();
        BLACK_HOLES.clearPlayer(playerUuid);
        SUMMONS.clearPlayer(playerUuid);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        BLACK_HOLES.clearAll();
        SUMMONS.clearAll();
    }

    public static void handleBlackHole(ServerPlayer player, BlackHole blackHole) {
        int max = ArcanaConfig.maxBlackHoles();
        MinecraftServer server = player.getServer();
        List<UUID> evicted = BLACK_HOLES.track(player.getUUID(), blackHole.getUUID(), max,
                uuid -> isEntityStale(server, uuid));

        for (UUID evictedId : evicted) {
            Entity entity = findEntity(server, evictedId);
            if (entity != null) {
                entity.discard();
            }
        }

        if (!evicted.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.mekasuitarcana.black_hole_limit", max),
                    true);
        }
    }

    public static void handleSummon(ServerPlayer player, Entity summon) {
        int max = ArcanaConfig.maxSummons();
        MinecraftServer server = player.getServer();
        List<UUID> evicted = SUMMONS.track(player.getUUID(), summon.getUUID(), max,
                uuid -> isEntityStale(server, uuid));

        for (UUID evictedId : evicted) {
            Entity entity = findEntity(server, evictedId);
            if (entity instanceof IMagicSummon magicSummon) {
                magicSummon.onUnSummon();
            } else if (entity != null) {
                entity.discard();
            }
        }

        if (!evicted.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.mekasuitarcana.summon_limit", max),
                    true);
        }
    }

    private static boolean isEntityStale(MinecraftServer server, UUID uuid) {
        Entity entity = findEntity(server, uuid);
        return entity == null || entity.isRemoved() || !entity.isAlive();
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    public static SummonLimitTracker blackHoleTracker() {
        return BLACK_HOLES;
    }

    public static SummonLimitTracker summonTracker() {
        return SUMMONS;
    }
}