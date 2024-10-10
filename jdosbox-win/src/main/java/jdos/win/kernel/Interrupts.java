package jdos.win.kernel;

import jdos.cpu.CPU;
import jdos.cpu.Callback;
import jdos.cpu.Paging;
import jdos.hardware.IO;
import jdos.hardware.Memory;
import jdos.win.Win;

public class Interrupts {

    static final public int IRQ0 = 32;
    static final public int IRQ1 = 33;
    static final public int IRQ2 = 34;
    static final public int IRQ3 = 35;
    static final public int IRQ4 = 36;
    static final public int IRQ5 = 37;
    static final public int IRQ6 = 38;
    static final public int IRQ7 = 39;
    static final public int IRQ8 = 40;
    static final public int IRQ9 = 41;
    static final public int IRQ10 = 42;
    static final public int IRQ11 = 43;
    static final public int IRQ12 = 44;
    static final public int IRQ13 = 45;
    static final public int IRQ14 = 46;
    static final public int IRQ15 = 47;

    public Callback.Handler[] interrupt_handlers;

    public int isr0;
    public int isr1;
    public int isr2;
    public int isr3;
    public int isr4;
    public int isr5;
    public int isr6;
    public int isr7;
    public int isr8;
    public int isr9;
    public int isr10;
    public int isr11;
    public int isr12;
    public int isr13;
    public int isr14;
    public int isr15;
    public int isr16;
    public int isr17;
    public int isr18;
    public int isr19;
    public int isr20;
    public int isr21;
    public int isr22;
    public int isr23;
    public int isr24;
    public int isr25;
    public int isr26;
    public int isr27;
    public int isr28;
    public int isr29;
    public int isr30;
    public int isr31;
    public int isr128;

    public int irq0;
    public int irq1;
    public int irq2;
    public int irq3;
    public int irq4;
    public int irq5;
    public int irq6;
    public int irq7;
    public int irq8;
    public int irq9;
    public int irq10;
    public int irq11;
    public int irq12;
    public int irq13;
    public int irq14;
    public int irq15;

    Callback.Handler isrHandler = new Callback.Handler() {
        public int call() {
            int index = CPU.CPU_Peek32(0);
            if (index>=IRQ0 && index<IRQ15) {
                // Send an EOI (end of interrupt) signal to the PICs.
                if (index>=IRQ8)
                    IO.IO_WriteB(0xA0, 0x20);
                IO.IO_WriteB(0x20, 0x20);
            }
            if (interrupt_handlers[index]!=null)
                return interrupt_handlers[index].call();
            else {
                if (index == 14) {
                    System.out.println("Page Fault at "+Integer.toHexString(Paging.cr2));
                    Win.exit();
                }
            }
            return 0;
        }
        public String getName() {
            return "isr";
        }
    };

    public Interrupts(KernelMemory memory) {
        interrupt_handlers = new Callback.Handler[256];

        isr0 = install(memory, 0, false, isrHandler);
        isr1 = install(memory, 1, false, isrHandler);
        isr2 = install(memory, 2, false, isrHandler);
        isr3 = install(memory, 3, false, isrHandler);
        isr4 = install(memory, 4, false, isrHandler);
        isr5 = install(memory, 5, false, isrHandler);
        isr6 = install(memory, 6, false, isrHandler);
        isr7 = install(memory, 7, false, isrHandler);
        isr8 = install(memory, 8, true, isrHandler);
        isr9 = install(memory, 9, false, isrHandler);
        isr10 = install(memory, 10, true, isrHandler);
        isr11 = install(memory, 11, true, isrHandler);
        isr12 = install(memory, 12, true, isrHandler);
        isr13 = install(memory, 13, true, isrHandler);
        isr14 = install(memory, 14, true, isrHandler);
        isr15 = install(memory, 15, false, isrHandler);
        isr16 = install(memory, 16, false, isrHandler);
        isr17 = install(memory, 17, false, isrHandler);
        isr18 = install(memory, 18, false, isrHandler);
        isr19 = install(memory, 19, false, isrHandler);
        isr20 = install(memory, 20, false, isrHandler);
        isr21 = install(memory, 21, false, isrHandler);
        isr22 = install(memory, 22, false, isrHandler);
        isr23 = install(memory, 23, false, isrHandler);
        isr24 = install(memory, 24, false, isrHandler);
        isr25 = install(memory, 25, false, isrHandler);
        isr26 = install(memory, 26, false, isrHandler);
        isr27 = install(memory, 27, false, isrHandler);
        isr28 = install(memory, 28, false, isrHandler);
        isr29 = install(memory, 29, false, isrHandler);
        isr30 = install(memory, 30, false, isrHandler);
        isr31 = install(memory, 31, false, isrHandler);
        isr128 = install(memory, 128, false, isrHandler);

        irq0 = install(memory, IRQ0, false, isrHandler);
        irq1 = install(memory, IRQ1, false, isrHandler);
        irq2 = install(memory, IRQ2, false, isrHandler);
        irq3 = install(memory, IRQ3, false, isrHandler);
        irq4 = install(memory, IRQ4, false, isrHandler);
        irq5 = install(memory, IRQ5, false, isrHandler);
        irq6 = install(memory, IRQ6, false, isrHandler);
        irq7 = install(memory, IRQ7, false, isrHandler);
        irq8 = install(memory, IRQ8, false, isrHandler);
        irq9 = install(memory, IRQ9, false, isrHandler);
        irq10 = install(memory, IRQ10, false, isrHandler);
        irq11 = install(memory, IRQ11, false, isrHandler);
        irq12 = install(memory, IRQ12, false, isrHandler);
        irq13 = install(memory, IRQ13, false, isrHandler);
        irq14 = install(memory, IRQ14, false, isrHandler);
        irq15 = install(memory, IRQ15, false, isrHandler);
    }

    public void registerHandler(int irq, Callback.Handler handler) {
        interrupt_handlers[irq] = handler;
    }

    public int install(KernelMemory memory, int index, boolean errorCodePresent, Callback.Handler handler) {
        int callback = WinCallback.addCallback(handler);

        int physAddress = memory.kmalloc(errorCodePresent?13:15);
        int result = physAddress;

        if (!errorCodePresent) {
            Memory.mem_writeb(physAddress++, 0x6a); // push ib
            Memory.mem_writeb(physAddress++, 0x00);
        }
        // push entry index
        Memory.mem_writeb(physAddress++, 0x6a); // push ib
        Memory.mem_writeb(physAddress++, index);

        Memory.mem_writeb(physAddress++, 0xFE);	    //GRP 4
        Memory.mem_writeb(physAddress++,0x38);	    //Extra Callback instruction
        Memory.mem_writew(physAddress,callback);
        physAddress+=2;

        Memory.mem_writeb(physAddress,0x81);  // Grpl Ed,Id
        Memory.mem_writeb(physAddress+0x01,0xC4);  // ADD ESP
        Memory.mem_writed(physAddress+0x02,0x00000008); // 8 (pop 2 32-bit words off)
        physAddress+=6;

        Memory.mem_writeb(physAddress,0xCF); //IRET
        return result;
    }
}
