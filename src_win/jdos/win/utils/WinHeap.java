package jdos.win.utils;

import jdos.win.builtin.WinAPI;
import jdos.win.kernel.KernelHeap;

import java.util.Hashtable;
import java.util.Vector;

public class WinHeap {
    private Vector heaps = new Vector();
    private KernelHeap heap;

    public WinHeap(KernelHeap heap) {
        this.heap = heap;
    }

    public void deallocate() {
        heap.deallocate();
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
        return item.alloc(size);
    }

    public int freeHeap(int handle, int memory) {
        HeapItem item = (HeapItem)heaps.elementAt(handle-1);
        if (item == null) {
            WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            return WinAPI.FALSE;
        }
        return item.free(memory);
    }

    private class HeapItem {
        int initialSize;
        int maxSize;
        int currentSize = 0;
        Hashtable allocs = new Hashtable();

        public HeapItem(int initialSize, int maxSize) {
            this.initialSize = initialSize;
            this.maxSize = maxSize;
        }

        public int free(int add) {
            Integer size = (Integer)allocs.get(new Integer(add));
            if (size == null) {
                WinSystem.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                return WinAPI.FALSE;
            }
            heap.free(add);
            currentSize-=size.intValue();
            return WinAPI.TRUE;
        }

        public int alloc(int size) {
            if (maxSize!=0 && (currentSize+size)>maxSize)
                return 0;
            int result = heap.alloc(size, false);
            allocs.put(new Integer(result), new Integer(size));
            return result;
        }
    }
}
