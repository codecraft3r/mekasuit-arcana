package dev.vvh.mekasuitarcana.limit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Tracks entity UUIDs per player in FIFO order and determines evictions when capacity limits are exceeded.
 *
 * <p>Pure Java, completely loader-free so it can be verified in standard JUnit test runs.</p>
 */
public final class SummonLimitTracker {

    private final Map<UUID, Set<UUID>> entries = new ConcurrentHashMap<>();

    /**
     * Records a new entity UUID for the given player.
     *
     * @param playerUuid player's UUID
     * @param entityUuid newly spawned/assigned entity UUID
     * @param maxLimit maximum concurrent entities allowed for this player (0 or negative means unlimited)
     * @param isStale predicate returning true if an existing UUID is stale, dead, or removed
     * @return list of evicted entity UUIDs in oldest-first order that must be dismissed (empty if none)
     */
    public synchronized List<UUID> track(UUID playerUuid, UUID entityUuid, int maxLimit, Predicate<UUID> isStale) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(entityUuid, "entityUuid");
        Objects.requireNonNull(isStale, "isStale");

        Set<UUID> set = entries.computeIfAbsent(playerUuid, ignored -> new LinkedHashSet<>());
        set.removeIf(isStale);

        if (set.contains(entityUuid)) {
            return Collections.emptyList();
        }

        List<UUID> evicted = new ArrayList<>();
        if (maxLimit > 0 && set.size() >= maxLimit) {
            Iterator<UUID> it = set.iterator();
            while (it.hasNext() && (set.size() - evicted.size()) >= maxLimit) {
                UUID oldest = it.next();
                evicted.add(oldest);
                it.remove();
            }
        }

        set.add(entityUuid);
        return Collections.unmodifiableList(evicted);
    }

    public synchronized void remove(UUID playerUuid, UUID entityUuid) {
        Set<UUID> set = entries.get(playerUuid);
        if (set != null) {
            set.remove(entityUuid);
        }
    }

    public synchronized void clearPlayer(UUID playerUuid) {
        entries.remove(playerUuid);
    }

    public synchronized void clearAll() {
        entries.clear();
    }

    public synchronized int count(UUID playerUuid, Predicate<UUID> isStale) {
        Set<UUID> set = entries.get(playerUuid);
        if (set == null) return 0;
        set.removeIf(isStale);
        return set.size();
    }

    public synchronized Set<UUID> getTracked(UUID playerUuid) {
        Set<UUID> set = entries.get(playerUuid);
        return set == null ? Collections.emptySet() : Set.copyOf(set);
    }
}
