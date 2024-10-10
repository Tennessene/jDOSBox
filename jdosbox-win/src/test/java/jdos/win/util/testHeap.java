package jdos.win.util;

import jdos.win.utils.Heap;
import junit.framework.TestCase;

public class testHeap extends TestCase {
    public void testAlloc() {
        Heap heap = new Heap(0x01000000l, 0xFFFF0000l);
        long a = heap.alloc(0x100, false);
        assertTrue(a==0x01000000);
        long b = heap.alloc(0x100, false);
        assertTrue(b==0x01000100);
        long c = heap.alloc(0x100, true);
        assertTrue(c==0x01001000);
    }

    public void testAllocAt() {
        Heap heap = new Heap(0x01000000l, 0xFFFF0000l);
        long a = heap.alloc(0x40000000, 0x01000000);
        assertTrue(a==0x40000000);
        long b = heap.alloc(0xE0000000l, 0x01000000);
        assertTrue(b==0xE0000000l);
        long c = heap.alloc(0xE0001000l, 0x01000000);
        assertTrue(c==0);
        long d = heap.alloc(0x10000000l, 0x01000000);
        assertTrue(d==0x10000000l);
    }

    public void testGetNext() {
        Heap heap = new Heap(0x01000000l, 0xFFFF0000l);
        long a = heap.alloc(0x40000000, 0x01000000);
        assertTrue(a==0x40000000);
        long b = heap.alloc(0x42000000, 0x01000000);
        assertTrue(b==0x42000000);
        assertTrue(heap.getNextAddress(0x40000000, 0x00500000, false)==0x41000000);
        assertTrue(heap.getNextAddress(0x40000000, 0x01000000, false)==0x41000000);
        assertTrue(heap.getNextAddress(0x40000000, 0x01000001, false)==0x43000000);
    }
}
