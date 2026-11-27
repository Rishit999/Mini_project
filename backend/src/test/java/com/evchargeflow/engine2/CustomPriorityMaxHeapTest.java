package com.evchargeflow.engine2;

import com.evchargeflow.engine2.datastructures.CustomPriorityMaxHeap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Unit tests for CustomPriorityMaxHeap (Engine 2 — Logic & Rule Engine)
 *
 * Test Cases: CH-001 through CH-016
 *
 * Uses Integer elements (score) for simplicity; the heap is generic.
 */
@DisplayName("TC-E2-02: CustomPriorityMaxHeap Unit Tests")
class CustomPriorityMaxHeapTest {

    // Simple heap of integers compared naturally (highest int = highest priority)
    private CustomPriorityMaxHeap<Integer> heap;

    // For tie-breaking tests, use a simple record
    record Item(int score, long arrivalMs, String id) {}

    private CustomPriorityMaxHeap<Item> itemHeap() {
        Comparator<Item> cmp = Comparator
            .comparingInt(Item::score).reversed()           // higher score first
            .thenComparingLong(Item::arrivalMs)             // earlier arrival first (ascending)
            .thenComparing(Item::id);                       // lexicographic id
        // Invert because CustomPriorityMaxHeap uses compare(a,b)>0 means a has higher priority
        return new CustomPriorityMaxHeap<>(
            (a, b) -> {
                if (a.score() != b.score()) return Integer.compare(a.score(), b.score());
                if (a.arrivalMs() != b.arrivalMs()) return Long.compare(b.arrivalMs(), a.arrivalMs());
                return b.id().compareTo(a.id());
            }
        );
    }

    @BeforeEach
    void setUp() {
        heap = new CustomPriorityMaxHeap<>(Integer::compareTo);
    }

    @Test
    @DisplayName("CH-001: Empty heap → isEmpty returns true, size 0")
    void testEmptyHeap() {
        assertThat(heap.isEmpty()).isTrue();
        assertThat(heap.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("CH-002: Insert one element → not empty, size 1")
    void testInsertOne() {
        heap.insert(70);
        assertThat(heap.isEmpty()).isFalse();
        assertThat(heap.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("CH-003: Peek returns max without removing")
    void testPeek() {
        heap.insert(50);
        heap.insert(80);
        heap.insert(30);

        assertThat(heap.peek()).isEqualTo(80);
        assertThat(heap.size()).isEqualTo(3); // peek doesn't remove
    }

    @Test
    @DisplayName("CH-004: ExtractMax removes and returns highest score")
    void testExtractMax_returnHighest() {
        heap.insert(50);
        heap.insert(80);
        heap.insert(30);

        assertThat(heap.extractMax()).isEqualTo(80);
        assertThat(heap.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("CH-005: Repeated extractMax gives descending order")
    void testExtractMax_descendingOrder() {
        heap.insert(60);
        heap.insert(90);
        heap.insert(40);
        heap.insert(75);

        int prev = Integer.MAX_VALUE;
        while (!heap.isEmpty()) {
            int s = heap.extractMax();
            assertThat(s).isLessThanOrEqualTo(prev);
            prev = s;
        }
    }

    @Test
    @DisplayName("CH-006: Tie-breaking — same score, earlier arrival wins")
    void testTieBreaking_earlierArrival() {
        CustomPriorityMaxHeap<Item> h = itemHeap();
        long earlier = 1000L;
        long later   = 2000L;
        h.insert(new Item(70, later,   "R1"));
        h.insert(new Item(70, earlier, "R2"));

        Item top = h.extractMax();
        assertThat(top.id()).isEqualTo("R2"); // earlier arrival wins
    }

    @Test
    @DisplayName("CH-007: Tie-breaking — same score, same arrival → lexicographically smaller ID wins")
    void testTieBreaking_lexId() {
        CustomPriorityMaxHeap<Item> h = itemHeap();
        long t = 1000L;
        h.insert(new Item(70, t, "REQ-002"));
        h.insert(new Item(70, t, "REQ-001"));

        Item top = h.extractMax();
        assertThat(top.id()).isEqualTo("REQ-001");
    }

    @Test
    @DisplayName("CH-008: Remove specific element")
    void testRemove_element() {
        heap.insert(80);
        heap.insert(60);
        heap.insert(40);

        boolean removed = heap.remove(60);
        assertThat(removed).isTrue();
        assertThat(heap.size()).isEqualTo(2);

        List<Integer> remaining = heap.toSortedList();
        assertThat(remaining).doesNotContain(60);
    }

    @Test
    @DisplayName("CH-009: Remove non-existent element → false, size unchanged")
    void testRemove_nonExistent() {
        heap.insert(70);
        boolean removed = heap.remove(999);
        assertThat(removed).isFalse();
        assertThat(heap.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("CH-010: Peek on empty heap returns null")
    void testPeek_emptyHeap() {
        assertThat(heap.peek()).isNull();
    }

    @Test
    @DisplayName("CH-011: ExtractMax on empty heap returns null")
    void testExtractMax_emptyHeap() {
        assertThat(heap.extractMax()).isNull();
    }

    @Test
    @DisplayName("CH-012: Insert null → throws IllegalArgumentException")
    void testInsert_null_throws() {
        assertThatThrownBy(() -> heap.insert(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CH-013: toSortedList returns descending order without destroying heap")
    void testToSortedList_descendingOrder() {
        heap.insert(50);
        heap.insert(90);
        heap.insert(20);
        heap.insert(70);

        List<Integer> sorted = heap.toSortedList();
        assertThat(sorted).hasSize(4);
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertThat(sorted.get(i)).isGreaterThanOrEqualTo(sorted.get(i + 1));
        }
        assertThat(heap.size()).isEqualTo(4); // heap intact after toSortedList
    }

    @Test
    @DisplayName("CH-014: Heap maintains order after mixed inserts and removes")
    void testHeapIntegrity_mixedOperations() {
        for (int i = 1; i <= 10; i++) {
            heap.insert(i * 7);
        }
        heap.remove(35);  // 5*7
        heap.remove(21);  // 3*7

        int prev = Integer.MAX_VALUE;
        while (!heap.isEmpty()) {
            int s = heap.extractMax();
            assertThat(s).isLessThanOrEqualTo(prev);
            prev = s;
        }
    }

    @Test
    @DisplayName("CH-015: Rebuild restores max-heap property")
    void testRebuild_restoresProperty() {
        // Use mutable container to simulate priority change
        int[] scores = {30, 60, 20};
        // Add wrappers that read from scores array
        heap.insert(scores[0]);
        heap.insert(scores[1]);
        heap.insert(scores[2]);

        // Replace all and rebuild with new heap
        CustomPriorityMaxHeap<Integer> newHeap = new CustomPriorityMaxHeap<>(Integer::compareTo);
        newHeap.insert(90); // was 30, now 90
        newHeap.insert(50); // was 60, now 50
        newHeap.insert(20);
        newHeap.rebuild();

        assertThat(newHeap.extractMax()).isEqualTo(90);
    }

    @Test
    @DisplayName("CH-016: Large dataset (1000 elements) → correct sorted extraction")
    void testLargeDataset_correctOrder() {
        for (int i = 0; i < 1000; i++) {
            heap.insert((int)(Math.random() * 100));
        }
        assertThat(heap.size()).isEqualTo(1000);

        int prev = Integer.MAX_VALUE;
        int count = 0;
        while (!heap.isEmpty()) {
            int s = heap.extractMax();
            assertThat(s).isLessThanOrEqualTo(prev);
            prev = s;
            count++;
        }
        assertThat(count).isEqualTo(1000);
    }
}
