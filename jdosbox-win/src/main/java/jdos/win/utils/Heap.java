package jdos.win.utils;

import java.util.ArrayList;
import java.util.Hashtable;

public class Heap {
    static private final int SMALLEST_SIZE_FOR_SPLIT = 4;

    private long start;
    private long end;
    private ArrayList itemsBySize = new ArrayList();
    private ArrayList itemsByAddress = new ArrayList();
    private Hashtable usedMemory = new Hashtable();

    private static class HeapItem implements Comparable {
        public int compareTo(Object o) {
            if (((HeapItem)o).size == size)
                return 0;
            if (((HeapItem)o).size < size)
                return -1;
            return 1;
        }

        public HeapItem(long address, long size) {
            this.address = address;
            this.size = size;
        }
        public long address;
        public long size;
    }

    public Heap(long start, long end) {
        this.start = start;
        this.end = end;
        insertItem(new HeapItem(start & 0xFFFFFFFFl, end-start));
    }

    private int findIndexBySize(long key) {
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

    public long getNextAddress(long address, long size, boolean pageAlign) {
        int index = findIndexByAddress(address);
        if (pageAlign) {
            address = (address + 0xFFF) & ~0xFFF;
        }
        if (index<0) {
            HeapItem last = getLastItem();
            if (last.address+last.size>=address+size)
                return address;
            return 0;
        }
        int first = index;
        while (index<itemsByAddress.size()) {
            HeapItem next = (HeapItem)itemsByAddress.get(index++);
            long a = next.address;
            if (pageAlign) {
                a = (a + 0xFFF) & ~0xFFF;
            }
            if (a>=address && next.size-(a-next.address)>=size) {
                return a;
            }
        }
        index = first;
        while (index<itemsByAddress.size()) {
            HeapItem next = (HeapItem)itemsByAddress.get(index++);
            long a = next.address;
            if (pageAlign) {
                a = (a + 0xFFF) & ~0xFFF;
            }
            if (a-next.address+next.size>=size) {
                return a;
            }
        }
        return 0;
    }

    public long alloc(long address, long size) {
        address&=0xFFFFFFFFl;
        int index = findIndexByAddress(address);
        if (index<0) {
            HeapItem last = getLastItem();
            if (address<last.address)
                return 0;
            removeItem(last);
            last.size = address - last.address;
            if (last.size != 0)
                insertItem(last);
            usedMemory.put(new Long(address), new HeapItem(address, size));
            if (address+size<end)
                insertItem(new HeapItem(address+size, end-(address+size)));
        } else {
            HeapItem free = (HeapItem)itemsByAddress.get(index);
            if (free.address > address && index>0)
                free = (HeapItem)itemsByAddress.get(index-1); // getNextAddress aligned it into this slot
            if (address<free.address || address+size>free.address+free.size) {
                return 0;
            }
            removeItem(free);
            long newAddress = address+size;
            if (free.size==newAddress-free.address)
                removeItem(free);
            free.size-=newAddress-free.address;
            long oldAddress = free.address;
            free.address = newAddress;
            usedMemory.put(new Long(address), new HeapItem(address, size));
            if (oldAddress<address && oldAddress>=start) {
                insertItem(new HeapItem(oldAddress, address-oldAddress));
            }
            insertItem(free);
        }
        return address;
    }

    public long alloc(long size, boolean pageAlign) {
        int index = findIndexBySize(size);
        if (index<0) {
            return 0;
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
                return 0;
            }
        } else {
            item = (HeapItem)itemsBySize.get(index);
            removeItem(item);
        }

        if (item.size-size>=SMALLEST_SIZE_FOR_SPLIT) {
            long newSize = item.size-size;
            HeapItem newItem = new HeapItem(item.address+size, newSize);
            insertItem(newItem);
            item.size-=newSize;
        }
        usedMemory.put(new Long(item.address), item);
        return item.address;
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
            if (index>0) {
                HeapItem before = (HeapItem)itemsByAddress.get(index-1);
                if (before.address+before.size==item.address) {
                    removeItem(before);
                    before.size+=item.size;
                    item = before;
                }
            }
            if (index<itemsByAddress.size()) {
                HeapItem after = (HeapItem)itemsByAddress.get(index);
                if (item.address+item.size==after.address) {
                    removeItem(after);
                    after.address=item.address;
                    after.size+=item.size;
                }
            }
            insertItem(item);
        } else {
            insertItem(item);
        }
    }
}
