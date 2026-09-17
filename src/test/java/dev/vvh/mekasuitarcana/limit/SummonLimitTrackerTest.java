package dev.vvh.mekasuitarcana.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SummonLimitTrackerTest {

    private SummonLimitTracker tracker;
    private UUID player1;
    private UUID player2;

    @BeforeEach
    void setUp() {
        tracker = new SummonLimitTracker();
        player1 = UUID.randomUUID();
        player2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("under limit, no evictions occur")
    void underLimitNoEvictions() {
        UUID entity1 = UUID.randomUUID();
        UUID entity2 = UUID.randomUUID();

        List<UUID> evicted1 = tracker.track(player1, entity1, 3, uuid -> false);
        List<UUID> evicted2 = tracker.track(player1, entity2, 3, uuid -> false);

        assertTrue(evicted1.isEmpty());
        assertTrue(evicted2.isEmpty());
        assertEquals(2, tracker.count(player1, uuid -> false));
    }

    @Test
    @DisplayName("exceeding limit evicts the oldest entity in FIFO order")
    void exceedingLimitEvictsOldest() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID e3 = UUID.randomUUID();
        UUID e4 = UUID.randomUUID();
        UUID e5 = UUID.randomUUID();

        tracker.track(player1, e1, 3, uuid -> false);
        tracker.track(player1, e2, 3, uuid -> false);
        tracker.track(player1, e3, 3, uuid -> false);

        List<UUID> evicted4 = tracker.track(player1, e4, 3, uuid -> false);
        assertEquals(List.of(e1), evicted4);
        assertEquals(3, tracker.count(player1, uuid -> false));
        assertEquals(Set.of(e2, e3, e4), tracker.getTracked(player1));

        List<UUID> evicted5 = tracker.track(player1, e5, 3, uuid -> false);
        assertEquals(List.of(e2), evicted5);
        assertEquals(3, tracker.count(player1, uuid -> false));
        assertEquals(Set.of(e3, e4, e5), tracker.getTracked(player1));
    }

    @Test
    @DisplayName("duplicate tracking is idempotent and does not evict")
    void duplicateTrackingIsIdempotent() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID e3 = UUID.randomUUID();

        tracker.track(player1, e1, 3, uuid -> false);
        tracker.track(player1, e2, 3, uuid -> false);
        tracker.track(player1, e3, 3, uuid -> false);

        List<UUID> duplicate = tracker.track(player1, e2, 3, uuid -> false);
        assertTrue(duplicate.isEmpty());
        assertEquals(3, tracker.count(player1, uuid -> false));
    }

    @Test
    @DisplayName("zero or negative limit is unlimited")
    void unlimitedWhenZeroOrNegative() {
        for (int i = 0; i < 50; i++) {
            List<UUID> evicted = tracker.track(player1, UUID.randomUUID(), 0, uuid -> false);
            assertTrue(evicted.isEmpty());
        }
        assertEquals(50, tracker.count(player1, uuid -> false));
    }

    @Test
    @DisplayName("stale or dead entities are pruned before capacity check")
    void staleEntitiesArePruned() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID e3 = UUID.randomUUID();
        UUID e4 = UUID.randomUUID();

        tracker.track(player1, e1, 3, uuid -> false);
        tracker.track(player1, e2, 3, uuid -> false);
        tracker.track(player1, e3, 3, uuid -> false);

        Set<UUID> dead = new HashSet<>();
        dead.add(e2);

        List<UUID> evicted = tracker.track(player1, e4, 3, dead::contains);
        // e2 was dead so it was pruned; count went from 3 to 2, e4 fits without evicting e1 or e3!
        assertTrue(evicted.isEmpty());
        assertEquals(Set.of(e1, e3, e4), tracker.getTracked(player1));
    }

    @Test
    @DisplayName("different players have completely isolated tracking and limits")
    void playersAreIsolated() {
        UUID p1_e1 = UUID.randomUUID();
        UUID p1_e2 = UUID.randomUUID();
        UUID p2_e1 = UUID.randomUUID();

        tracker.track(player1, p1_e1, 1, uuid -> false);
        tracker.track(player2, p2_e1, 1, uuid -> false);

        assertEquals(1, tracker.count(player1, uuid -> false));
        assertEquals(1, tracker.count(player2, uuid -> false));

        List<UUID> p1_evicted = tracker.track(player1, p1_e2, 1, uuid -> false);
        assertEquals(List.of(p1_e1), p1_evicted);
        assertEquals(1, tracker.count(player2, uuid -> false));
        assertEquals(Set.of(p2_e1), tracker.getTracked(player2));
    }

    @Test
    @DisplayName("clearPlayer and clearAll remove entries properly")
    void clearingWorks() {
        tracker.track(player1, UUID.randomUUID(), 5, uuid -> false);
        tracker.track(player2, UUID.randomUUID(), 5, uuid -> false);

        tracker.clearPlayer(player1);
        assertEquals(0, tracker.count(player1, uuid -> false));
        assertEquals(1, tracker.count(player2, uuid -> false));

        tracker.clearAll();
        assertEquals(0, tracker.count(player2, uuid -> false));
    }
}
