package jdos.win.kernel;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.hardware.IO;
import jdos.hardware.Memory;

// Based heavily on James Molloy's work at http://www.jamesmolloy.co.uk/tutorial_html/4.-The%20GDT%20and%20IDT.html

public class DescriptorTables {
    // This structure contains the value of one GDT entry.
    // We use the attribute 'packed' to tell GCC not to change
    // any of the alignment in the structure.
    private static class gdt_entry_t extends Address
    {
        //u16int limit_low;           // The lower 16 bits of the limit.
        //u16int base_low;            // The lower 16 bits of the base.
        //u8int  base_middle;         // The next 8 bits of the base.
        //u8int  access;              // Access flags, determine what ring this segment can be used in.
        //u8int  granularity;
        //u8int  base_high;           // The last 8 bits of the base.

        final static public int SIZE = 8;
        static public gdt_entry_t alloc(int count, KernelMemory memory) {
            gdt_entry_t result = new gdt_entry_t();
            result.ptr = memory.kmalloc(SIZE*count);
            return result;
        }

        public void limit_low(int index, int value) {
            writew(index*SIZE+0, value);
        }
        public void base_low(int index, int value) {
            writew(index*SIZE+2, value);
        }
        public void base_middle(int index, int value) {
            writeb(index * SIZE + 4, value);
        }
        public void access(int index, int value) {
            writeb(index * SIZE + 5, value);
        }
        public void granularity(int index, int value) {
            writeb(index * SIZE + 6, value);
        }
        public void base_high(int index, int value) {
            writeb(index * SIZE + 7, value);
        }
    }

    // This struct describes a GDT pointer. It points to the start of
    // our array of GDT entries, and is in the format required by the
    // lgdt instruction.
    private static class gdt_ptr_t extends Address
    {
        //u16int limit;               // The upper 16 bits of all selector limits.
        //u32int base;                // The address of the first gdt_entry_t struct.

        final static public int SIZE = 6;
        static public gdt_ptr_t alloc(KernelMemory memory) {
            gdt_ptr_t result = new gdt_ptr_t();
            result.ptr = memory.kmalloc(SIZE);
            return result;
        }
        public void limit(int l) {
            writew(0, l);
        }
        public void base(int b) {
            writed(2, b);
        }
    }

    // A struct describing an interrupt gate.
    private static class idt_entry_t extends Address
    {
        //u16int base_lo;             // The lower 16 bits of the address to jump to when this interrupt fires.
        //u16int sel;                 // Kernel segment selector.
        //u8int  always0;             // This must always be zero.
        //u8int  flags;               // More flags. See documentation.
        //u16int base_hi;             // The upper 16 bits of the address to jump to.
        final static public int SIZE = 8;
        static public idt_entry_t alloc(int count, KernelMemory memory) {
            idt_entry_t result = new idt_entry_t();
            result.ptr = memory.kmalloc(SIZE*count);
            result.count = count;
            return result;
        }
        private int count;
        public void clear() {
            Memory.mem_zero(ptr, count*SIZE);
        }
        public void base_lo(int index, int value) {
            writew(index*SIZE+0, value);
        }
        public void sel(int index, int value) {
            writew(index*SIZE+2, value);
        }
         public void always0(int index, int value) {
            writeb(index * SIZE + 4, value);
        }
        public void flags(int index, int value) {
            writeb(index * SIZE + 5, value);
        }
        public void base_hi(int index, int value) {
            writew(index * SIZE + 6, value);
        }
    }

    // A struct describing a pointer to an array of interrupt handlers.
    // This is in a format suitable for giving to 'lidt'.
    private static class idt_ptr_t extends Address
    {
        //u16int limit;
        //u32int base;                // The address of the first element in our idt_entry_t array.
        final static public int SIZE = 6;
        static public idt_ptr_t alloc(KernelMemory memory) {
            idt_ptr_t result = new idt_ptr_t();
            result.ptr = memory.kmalloc(SIZE);
            return result;
        }
        public void limit(int l) {
            writew(0, l);
        }
        public void base(int b) {
            writed(2, b);
        }
    }

    // A struct describing a Task State Segment.
    private static class tss_entry_t extends Address
    {
        //u32int prev_tss;   // The previous TSS - if we used hardware task switching this would form a linked list.
        //u32int esp0;       // The stack pointer to load when we change to kernel mode.
        //u32int ss0;        // The stack segment to load when we change to kernel mode.
        //u32int esp1;       // Unused...
        //u32int ss1;
        //u32int esp2;
        //u32int ss2;
        //u32int cr3;
        //u32int eip;
        //u32int eflags;
        //u32int eax;
        //u32int ecx;
        //u32int edx;
        //u32int ebx;
        //u32int esp;
        //u32int ebp;
        //u32int esi;
        //u32int edi;
        //u32int es;         // The value to load into ES when we change to kernel mode.
        //u32int cs;         // The value to load into CS when we change to kernel mode.
        //u32int ss;         // The value to load into SS when we change to kernel mode.
        //u32int ds;         // The value to load into DS when we change to kernel mode.
        //u32int fs;         // The value to load into FS when we change to kernel mode.
        //u32int gs;         // The value to load into GS when we change to kernel mode.
        //u32int ldt;        // Unused...
        //u16int trap;
        //u16int iomap_base;

        final static public int SIZE = 104;
        static public tss_entry_t alloc(KernelMemory memory) {
            tss_entry_t result = new tss_entry_t();
            result.ptr = memory.kmalloc(SIZE);
            return result;
        }
        public void clear() {
            Memory.mem_zero(ptr, SIZE);
        }

        public void prev_tss(int value) {
            writed(0, value);
        }
        public void esp0(int value) {
            writed(4, value);
        }
        public void ss0(int value) {
            writed(8, value);
        }
        public void esp1(int value) {
            writed(12, value);
        }
        public void ss1(int value) {
            writed(16, value);
        }
        public void esp2(int value) {
            writed(20, value);
        }
        public void ss2(int value) {
            writed(24, value);
        }
        public void cr3(int value) {
            writed(28, value);
        }
        public void eip(int value) {
            writed(32, value);
        }
        public void eflags(int value) {
            writed(36, value);
        }
        public void eax(int value) {
            writed(40, value);
        }
        public void ecx(int value) {
            writed(44, value);
        }
        public void edx(int value) {
            writed(48, value);
        }
        public void ebx(int value) {
            writed(52, value);
        }
        public void esp(int value) {
            writed(56, value);
        }
        public void ebp(int value) {
            writed(60, value);
        }
        public void esi(int value) {
            writed(64, value);
        }
        public void edi(int value) {
            writed(68, value);
        }
        public void es(int value) {
            writed(72, value);
        }
        public void cs(int value) {
            writed(76, value);
        }
        public void ss(int value) {
            writed(80, value);
        }
        public void ds(int value) {
            writed(84, value);
        }
        public void fs(int value) {
            writed(88, value);
        }
        public void gs(int value) {
            writed(92, value);
        }
        public void ldt(int value) {
            writed(96, value);
        }
        public void trap(int value) {
            writew(100, value);
        }
        public void iomap_base(int value) {
            writew(102, value);
        }
    }

    // Initialisation routine - zeroes all the interrupt service routines,
    // initialises the GDT and IDT.
    gdt_entry_t gdt_entries;
    gdt_ptr_t   gdt_ptr;
    idt_entry_t idt_entries;
    idt_ptr_t   idt_ptr;
    tss_entry_t tss_entry;
    Interrupts interrupts;

    public DescriptorTables(Interrupts interrupts, KernelMemory memory) {
        gdt_entries = gdt_entry_t.alloc(6, memory);
        gdt_ptr = gdt_ptr_t.alloc(memory);
        idt_entries = idt_entry_t.alloc(256, memory);
        idt_ptr = idt_ptr_t.alloc(memory);
        tss_entry = tss_entry_t.alloc(memory);
        this.interrupts = interrupts;
        init_descriptor_tables();
    }

    private void init_descriptor_tables()
    {
        // Initialise the global descriptor table.
        init_gdt();
        // Initialise the interrupt descriptor table.
        init_idt();
    }

    private void init_gdt()
    {
        gdt_ptr.limit((gdt_entry_t.SIZE * 6) - 1);
        gdt_ptr.base(gdt_entries.ptr);

        gdt_set_gate(0, 0, 0, 0, 0);                // Null segment
        gdt_set_gate(1, 0, 0xFFFFFFFF, 0x9A, 0xCF); // Code segment
        gdt_set_gate(2, 0, 0xFFFFFFFF, 0x92, 0xCF); // Data segment
        gdt_set_gate(3, 0, 0xFFFFFFFF, 0xFA, 0xCF); // User mode code segment
        gdt_set_gate(4, 0, 0xFFFFFFFF, 0xF2, 0xCF); // User mode data segment
        write_tss(5, 0x10, 0x0);

        gdt_flush(gdt_ptr.ptr);
        tss_flush();
    }

    private void gdt_flush(int value) {
        int v1 = (Memory.mem_readd(value + 2) & 0xFFFFFF);
        int v0 = Memory.mem_readw(value);
        CPU.CPU_LGDT(v0, v1);
        // 0x10 is the offset in the GDT to our data segment
        CPU.CPU_SetSegGeneralDS(0x10);
        CPU.CPU_SetSegGeneralES(0x10);
        CPU.CPU_SetSegGeneralFS(0x10);
        CPU.CPU_SetSegGeneralGS(0x10);
        CPU.CPU_SetSegGeneralSS(0x10);
        // 0x08 is the offset to our code segment
        CPU_Regs.reg_eip+=8;
    }

    private void tss_flush() {
        // mov ax, 0x2B
        // ltr ax
        //
        // Load the index of our TSS structure - The index is
        // 0x28, as it is the 5th selector and each is 8 bytes
        // long, but we set the bottom two bits (making 0x2B)
        // so that it has an RPL of 3, not zero.
        // Load 0x2B into the task state register.
        CPU.CPU_LTR(0x2B);
    }

    // Set the value of one GDT entry.
    private void gdt_set_gate(int num, int base, int limit, int access, int gran)
    {
        gdt_entries.base_low(num, (base & 0xFFFF));
        gdt_entries.base_middle(num, (base >> 16) & 0xFF);
        gdt_entries.base_high(num, (base >> 24) & 0xFF);

        gdt_entries.limit_low(num, (limit & 0xFFFF));
        gdt_entries.granularity(num, ((limit >> 16) & 0x0F) | (gran & 0xF0));
        gdt_entries.access(num, access);
    }

    // Initialise our task state segment structure.
    private void write_tss(int num, int ss0, int esp0)
    {
        // Firstly, let's compute the base and limit of our entry into the GDT.
        int base = tss_entry.ptr;
        int limit = base + tss_entry.SIZE;

        // Now, add our TSS descriptor's address to the GDT.
        gdt_set_gate(num, base, limit, 0xE9, 0x00);

        // Ensure the descriptor is initially zero.
        tss_entry.clear();

        tss_entry.ss0(ss0);  // Set the kernel stack segment.
        tss_entry.esp0(esp0); // Set the kernel stack pointer.

        // Here we set the cs, ss, ds, es, fs and gs entries in the TSS. These specify what
        // segments should be loaded when the processor switches to kernel mode. Therefore
        // they are just our normal kernel code/data segments - 0x08 and 0x10 respectively,
        // but with the last two bits set, making 0x0b and 0x13. The setting of these bits
        // sets the RPL (requested privilege level) to 3, meaning that this TSS can be used
        // to switch to kernel mode from ring 3.
        tss_entry.cs(0x0b);
        tss_entry.ss(0x13);
        tss_entry.ds(0x13);
        tss_entry.es(0x13);
        tss_entry.fs(0x13);
        tss_entry.gs(0x13);
    }

    private void set_kernel_stack(int stack)
    {
        tss_entry.esp0(stack);
    }

    private void init_idt()
    {
        idt_ptr.limit(idt_entry_t.SIZE * 256 - 1);
        idt_ptr.base(idt_entries.ptr);

        idt_entries.clear();

        // Remap the irq table.
        IO.IO_WriteB(0x20, 0x11);
        IO.IO_WriteB(0xA0, 0x11);
        IO.IO_WriteB(0x21, 0x20);
        IO.IO_WriteB(0xA1, 0x28);
        IO.IO_WriteB(0x21, 0x04);
        IO.IO_WriteB(0xA1, 0x02);
        IO.IO_WriteB(0x21, 0x01);
        IO.IO_WriteB(0xA1, 0x01);
        IO.IO_WriteB(0x21, 0x0);
        IO.IO_WriteB(0xA1, 0x0);

        idt_set_gate( 0, interrupts.isr0 , 0x08, 0x8E);
        idt_set_gate( 1, interrupts.isr1 , 0x08, 0x8E);
        idt_set_gate( 2, interrupts.isr2 , 0x08, 0x8E);
        idt_set_gate( 3, interrupts.isr3 , 0x08, 0x8E);
        idt_set_gate( 4, interrupts.isr4 , 0x08, 0x8E);
        idt_set_gate( 5, interrupts.isr5 , 0x08, 0x8E);
        idt_set_gate( 6, interrupts.isr6 , 0x08, 0x8E);
        idt_set_gate( 7, interrupts.isr7 , 0x08, 0x8E);
        idt_set_gate( 8, interrupts.isr8 , 0x08, 0x8E);
        idt_set_gate( 9, interrupts.isr9 , 0x08, 0x8E);
        idt_set_gate(10, interrupts.isr10, 0x08, 0x8E);
        idt_set_gate(11, interrupts.isr11, 0x08, 0x8E);
        idt_set_gate(12, interrupts.isr12, 0x08, 0x8E);
        idt_set_gate(13, interrupts.isr13, 0x08, 0x8E);
        idt_set_gate(14, interrupts.isr14, 0x08, 0x8E);
        idt_set_gate(15, interrupts.isr15, 0x08, 0x8E);
        idt_set_gate(16, interrupts.isr16, 0x08, 0x8E);
        idt_set_gate(17, interrupts.isr17, 0x08, 0x8E);
        idt_set_gate(18, interrupts.isr18, 0x08, 0x8E);
        idt_set_gate(19, interrupts.isr19, 0x08, 0x8E);
        idt_set_gate(20, interrupts.isr20, 0x08, 0x8E);
        idt_set_gate(21, interrupts.isr21, 0x08, 0x8E);
        idt_set_gate(22, interrupts.isr22, 0x08, 0x8E);
        idt_set_gate(23, interrupts.isr23, 0x08, 0x8E);
        idt_set_gate(24, interrupts.isr24, 0x08, 0x8E);
        idt_set_gate(25, interrupts.isr25, 0x08, 0x8E);
        idt_set_gate(26, interrupts.isr26, 0x08, 0x8E);
        idt_set_gate(27, interrupts.isr27, 0x08, 0x8E);
        idt_set_gate(28, interrupts.isr28, 0x08, 0x8E);
        idt_set_gate(29, interrupts.isr29, 0x08, 0x8E);
        idt_set_gate(30, interrupts.isr30, 0x08, 0x8E);
        idt_set_gate(31, interrupts.isr31, 0x08, 0x8E);
        idt_set_gate(32, interrupts.irq0, 0x08, 0x8E);
        idt_set_gate(33, interrupts.irq1, 0x08, 0x8E);
        idt_set_gate(34, interrupts.irq2, 0x08, 0x8E);
        idt_set_gate(35, interrupts.irq3, 0x08, 0x8E);
        idt_set_gate(36, interrupts.irq4, 0x08, 0x8E);
        idt_set_gate(37, interrupts.irq5, 0x08, 0x8E);
        idt_set_gate(38, interrupts.irq6, 0x08, 0x8E);
        idt_set_gate(39, interrupts.irq7, 0x08, 0x8E);
        idt_set_gate(40, interrupts.irq8, 0x08, 0x8E);
        idt_set_gate(41, interrupts.irq9, 0x08, 0x8E);
        idt_set_gate(42, interrupts.irq10, 0x08, 0x8E);
        idt_set_gate(43, interrupts.irq11, 0x08, 0x8E);
        idt_set_gate(44, interrupts.irq12, 0x08, 0x8E);
        idt_set_gate(45, interrupts.irq13, 0x08, 0x8E);
        idt_set_gate(46, interrupts.irq14, 0x08, 0x8E);
        idt_set_gate(47, interrupts.irq15, 0x08, 0x8E);
        idt_set_gate(128, interrupts.isr128, 0x08, 0x8E);
        idt_flush(idt_ptr.ptr);
    }

    private void idt_flush(int value) {
        int v1 = (Memory.mem_readd(value + 2) & 0xFFFFFF);
        int v0 = Memory.mem_readw(value);
        CPU.CPU_LIDT(v0, v1);
    }

    private void idt_set_gate(int num, int base, int sel, int flags)
    {
        idt_entries.base_lo(num, base & 0xFFFF);
        idt_entries.base_hi(num, (base >> 16) & 0xFFFF);

        idt_entries.sel(num, sel);
        idt_entries.always0(num, 0);
        idt_entries.flags(num, flags  | 0x60);
    }

}
