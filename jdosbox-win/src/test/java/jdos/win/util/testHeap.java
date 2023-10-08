package jdos.win.util;

import jdos.win.utils.Heap;
import junit.framework.TestCase;

public class testHeap extends TestCase {
    public void testAlloc() {
        Heap heap = new Heap(0x01000000L, 0xFFFF0000L);
        long a = heap.alloc(0x100, false);
        assertEquals(0x01000000, a);
        long b = heap.alloc(0x100, false);
        assertEquals(0x01000100, b);
        long c = heap.alloc(0x100, true);
        assertEquals(0x01001000, c);
    }

    public void testAllocAt() {
        Heap heap = new Heap(0x01000000L, 0xFFFF0000L);
        long a = heap.alloc(0x40000000, 0x01000000);
        assertEquals(0x40000000, a);
        long b = heap.alloc(0xE0000000L, 0x01000000);
        assertEquals(0xE0000000L, b);
        long c = heap.alloc(0xE0001000L, 0x01000000);
        assertEquals(0, c);
        long d = heap.alloc(0x10000000L, 0x01000000);
        assertEquals(0x10000000L, d);
    }

    public void testGetNext() {
        Heap heap = new Heap(0x01000000L, 0xFFFF0000L);
        long a = heap.alloc(0x40000000, 0x01000000);
        assertEquals(0x40000000, a);
        long b = heap.alloc(0x42000000, 0x01000000);
        assertEquals(0x42000000, b);
        assertEquals(0x41000000, heap.getNextAddress(0x40000000, 0x00500000, false));
        assertEquals(0x41000000, heap.getNextAddress(0x40000000, 0x01000000, false));
        assertEquals(0x43000000, heap.getNextAddress(0x40000000, 0x01000001, false));
    }
}
