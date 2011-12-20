package jdos.win.utils;

import jdos.win.builtin.WinAPI;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

public class Heap {
    private int startingAddress;
    private int size;
    private BitSet set;
    private Vector heaps = new Vector();

    private int BLOCK_SIZE = 256;

    public Heap(int startingAddress, int size) {
        this.startingAddress = (startingAddress + BLOCK_SIZE - 1) & ~(BLOCK_SIZE-1); // round up to the nearest block size
        this.size = size - (this.startingAddress-startingAddress);
        set = new BitSet(size/BLOCK_SIZE);
    }

    public int validateHeap(int handle, int flags, int address) {
        if (handle<=0 || handle>heaps.size()) {
            return WinAPI.FALSE;
        }
        HeapItem item = (HeapItem)heaps.get(handle-1);
        if (item == null) {
            return WinAPI.FALSE;
        }
        return WinAPI.TRUE;
    }

    public int createHeap(int initialSize, int maxSize) {
        HeapItem item = new HeapItem(initialSize, maxSize);

        for (int i=0;i<heaps.size();i++) {
            if (heaps.elementAt(i) == null) {
                heaps.setElementAt(item, i);
                return i+1;
            }
        }
        heaps.addElement(item);
        return heaps.size();
    }


    public int allocateHeap(int handle, int size) {
        if (handle-1>=heaps.size())
            return 0;
        HeapItem item = (HeapItem)heaps.elementAt(handle-1);
        return item.allocateHeap(size);
    }

    public int freeHeap(int handle, int memory) {
        HeapItem item = (HeapItem)heaps.elementAt(handle-1);
        if (item == null) {
            WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            return WinAPI.FALSE;
        }
        return item.freeHeap(memory);
    }

    private class HeapItem {
        int initialSize;
        int maxSize;
        int currentSize = 0;
        Hashtable memory = new Hashtable();

        public HeapItem(int initialSize, int maxSize) {
            this.initialSize = initialSize;
            this.maxSize = maxSize;
        }

        public int freeHeap(int add) {
            Integer blockCount = (Integer)memory.get(new Integer(add));
            if (blockCount == null) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return WinAPI.FALSE;
            }
            int startingBlock = (add-startingAddress)/BLOCK_SIZE;
            for (int i=0;i<blockCount.intValue();i++) {
                if (!set.get(startingBlock+i)) {
                    WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                    return WinAPI.FALSE;
                }
            }
            for (int i=0;i<blockCount.intValue();i++) {
                set.clear(startingBlock+i);
            }
            // :TODO: currentSize-=size
            return WinAPI.TRUE;
        }

        public int allocateHeap(int size) {
            // :TODO:
            //if (maxSize!=0 && (currentSize+size)>maxSize)
            //    return 0;
            int blockCount = (size+BLOCK_SIZE-1) / BLOCK_SIZE;
            // Yes I know this is slow, please someone else write a better one :)
            for (int i=0;i<set.size();i++) {
                if (!set.get(i)) {
                    boolean found = true;
                    for (int j=1;j<blockCount;j++) {
                        if (set.get(i+j)) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        int result = startingAddress+i*BLOCK_SIZE;
                        memory.put(new Integer(result), new Integer(blockCount));
                        for (int j=0;j<blockCount;j++)
                            set.set(j+i);
                        currentSize+=size;
                        return result;
                    }
                }
            }
            return 0;
        }
    }
}
