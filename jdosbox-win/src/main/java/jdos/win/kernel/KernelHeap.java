package jdos.win.kernel;

import jdos.hardware.Memory;
import jdos.win.Win;

import java.util.ArrayList;
import java.util.Hashtable;

// Based heavily on James Molloy's work at http://www.jamesmolloy.co.uk/tutorial_html/7.-The%20Heap.html

public class KernelHeap {
    static private final int SMALLEST_SIZE_FOR_SPLIT = 4;

    private boolean kernel;
    private boolean readonly;
    private long start;
    private long end;
    private long max;
    private int directory;
    private ArrayList itemsBySize = new ArrayList();
    private ArrayList itemsByAddress = new ArrayList();
    private KernelMemory memory;
    private Hashtable usedMemory = new Hashtable();

    private static class HeapItem implements Comparable {
        public int compareTo(Object o) {
            return ((HeapItem)o).size - size;
        }

        public HeapItem(long address, int size) {
            this.address = address;
            this.size = size;
        }
        public long address;
        public int size;
    }

    public KernelHeap(KernelMemory memory, int directory, long start, long end, long max, boolean kernel, boolean readonly) {
        this.memory = memory;
        this.directory = directory;
        this.kernel = kernel;
        this.readonly = readonly;
        this.start = start;
        this.end = start;
        this.max = max;
        if ((start & 0xFFF)!=0 || (end & 0xFFF)!=0) {
            System.out.println("Heap requires addresses to be 4k aligned");
            System.exit(0);
        }
        expand((int)(end - start), true);
    }

    public void deallocate() {
        for (long i=start;i<end;i+=4096) {
            memory.free_frame(memory.get_page((int)i, false, directory));
        }
    }

    int getFreeItemCount() {
        return itemsBySize.size();
    }

    private int findIndexBySize(int key) {
        int lo = 0;
        int hi = itemsBySize.size() - 1;
        while (lo <= hi) {
            // Key is in a[lo..hi] or not present.
            int mid = lo + (hi - lo) / 2;
            if      (key < ((HeapItem)itemsBySize.get(mid)).size) hi = mid - 1;
            else if (key > ((HeapItem)itemsBySize.get(mid)).size) lo = mid + 1;
            else return mid;
        }
        HeapItem item = getLargestItem();
        if (item==null || key>item.size)
            return -1;
        return lo;
    }

    private int findIndexByAddress(long key) {
        int lo = 0;
        int hi = itemsByAddress.size() - 1;
        while (lo <= hi) {
            // Key is in a[lo..hi] or not present.
            int mid = lo + (hi - lo) / 2;
            if      (key < ((HeapItem)itemsByAddress.get(mid)).address) hi = mid - 1;
            else if (key > ((HeapItem)itemsByAddress.get(mid)).address) lo = mid + 1;
            else return mid;
        }
        HeapItem item = getLastItem();
        if (item==null || key>item.address)
            return -1;
        return lo;
    }

    private void insertItem(HeapItem item) {
        int index = findIndexBySize(item.size);
        if (index<0)
            itemsBySize.add(item);
        else
            itemsBySize.add(index, item);
        index = findIndexByAddress(item.address);
        if (index<0)
            itemsByAddress.add(item);
        else
            itemsByAddress.add(index, item);
    }

    private HeapItem getLastItem() {
        if (itemsByAddress.size()==0)
            return null;
        return (HeapItem)itemsByAddress.get(itemsByAddress.size()-1);
    }

    private HeapItem getLargestItem() {
        if (itemsBySize.size() == 0)
            return null;
        return (HeapItem)itemsBySize.get(itemsBySize.size()-1);
    }

    private void removeItem(HeapItem item) {
        itemsBySize.remove(item);
        itemsByAddress.remove(item);
    }

    private boolean expand(int size, boolean pageAlign) {
        HeapItem last = getLastItem();
        boolean combineWithLast = false;
        if (!pageAlign && last!=null && last.address+last.size==end) {
            combineWithLast = true;
            size-=last.size;
        }
        if (end+(size+0xfff & 0xFFFFF000)>max) {
            return false;
        }
        long old_end = end;
        long address = end & 0xFFFFFFFFl;
        long new_end = address+size;
        int new_size = 0;
        while (address<new_end) {
            memory.alloc_frame(memory.get_page((int)address, true, directory), kernel, !readonly);
            address+=0x1000;
            new_size+=0x1000;
        }
        end +=new_size;

        if (combineWithLast) {
            removeItem(last);
            last.size+=new_size;
            insertItem(last);
        } else {
            insertItem(new HeapItem(old_end & 0xFFFFFFFFl, new_size));
        }
        return true;
    }

    private void dump() {
        for (int i=0;i<itemsBySize.size();i++) {
            HeapItem item = (HeapItem)itemsBySize.get(i);
            System.out.println(item.size+"@"+Long.toString(item.address, 16));
        }
    }
    private boolean inExpand = false;

    private int expandAndAlloc(int size, boolean pageAlign) {
        int result = 0;
        if (inExpand) { // prevent recursion
            dump();
            Win.panic("Error in Kernel Heap class");
        }
        inExpand = true;
        if (expand(size, pageAlign)) {
            result = alloc(size, pageAlign);
        }
        inExpand = false;
        return result;
    }

    public int alloc(int size, boolean pageAlign) {
        size = (size + 3) & ~3;
        int index = findIndexBySize(size);
        if (index<0) {
            return expandAndAlloc(size, pageAlign);
        }
        HeapItem item = null;
        if (pageAlign) {
            boolean found = false;
            for (int i=index;i<itemsBySize.size();i++) {
                item = (HeapItem)itemsBySize.get(i);
                long address = item.address;
                if ((address & 0xFFF)!=0) {
                    address+=0xFFF;
                    address&=~0xFFF;
                }
                if (item.address+item.size>=address+size) {
                    removeItem(item);
                    int newSize = (int)(address-item.address);
                    if (newSize>0) {
                        HeapItem newItem = new HeapItem(item.address, newSize);
                        insertItem(newItem);
                        item.address+=newSize;
                        item.size-=newSize;
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                return expandAndAlloc(size, pageAlign);
            }
        } else {
            item = (HeapItem)itemsBySize.get(index);
            removeItem(item);
        }

        if (item.size-size>=SMALLEST_SIZE_FOR_SPLIT) {
            int newSize = item.size-size;
            HeapItem newItem = new HeapItem(item.address+size, newSize);
            insertItem(newItem);
            item.size-=newSize;
        }
        usedMemory.put(new Long(item.address), item);
        return (int)item.address;
    }

    public void free(int p1) {
        long p = p1 & 0xFFFFFFFFl;
        if (p == 0)
            return;
        HeapItem item = (HeapItem)usedMemory.remove(new Long(p));
        if (item == null) {
            System.out.println("Heap is corrupt, tried to free 0x"+Long.toString(p, 16));
            System.exit(0);
        }
        int index = findIndexByAddress(p);
        if (index>=0) {
            boolean found = false;
            if (index>0) {
                HeapItem before = (HeapItem)itemsByAddress.get(index-1);
                if (before.address+before.size==item.address) {
                    removeItem(before);
                    before.size+=item.size;
                    item = before;
                    found = true;
                    insertItem(before);
                }
            }
            if (index<itemsByAddress.size()) {
                HeapItem after = (HeapItem)itemsByAddress.get(index);
                if (item.address+item.size==after.address) {
                    removeItem(after);
                    after.address-=item.size;
                    after.size+=item.size;
                    if (found)
                        removeItem(item); // if we merge with the before and after items
                    else
                        found = true;
                    insertItem(after);
                }
            }
            if (!found) {
                insertItem(item);
            }
        } else {
            insertItem(item);
        }
    }

    public int size(int address) {
        HeapItem item = (HeapItem)usedMemory.get(new Long(address));
        if (item != null)
            return item.size;
        return 0;
    }

    public int realloc(int address, int size, boolean zeroNewMemory) {
        int result = alloc(size, false);
        int oldSize = size(address);
        if (size>oldSize) {
            Memory.mem_memcpy(result, address, oldSize);
            if (zeroNewMemory)
                Memory.mem_zero(result+oldSize, size-oldSize);
        } else {
            Memory.mem_memcpy(result, address, size);
        }
        free(address);
        return result;
    }
}
