package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.Paging;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;
import jdos.util.Ptr;

public class VGA_memory {
    static public boolean VGA_KEEP_CHANGES = false;

//    static private long CHECKED(long addr) {
//        if (Config.C_VGARAM_CHECKED)
//            return addr & (VGA.vga.vmemwrap-1);
//        return addr;
//    }
//
//    static private long CHECKED2(long addr) {
//        if (Config.C_VGARAM_CHECKED)
//            return addr & ((VGA.vga.vmemwrap >> 2) - 1);
//        return addr;
//    }
//
    static private int CHECKED3(int addr) {
        return addr & (VGA.vga.vmemwrap-1);
    }
//
//    static private long CHECKED4(long addr) {
//        return addr & ((VGA.vga.vmemwrap >> 2) - 1);
//    }
//
//    static private void MEM_CHANGED(long _MEM) {
//        if (VGA_KEEP_CHANGES)
//            VGA.vga.changes.map.or(((int)_MEM) >> VGA.VGA_CHANGE_SHIFT, VGA.vga.changes.writeMask);
//    }

    //Nice one from DosEmu
    static private /*Bit32u*/int RasterOp(/*Bit32u*/int input,/*Bit32u*/int mask) {
        switch (VGA.vga.config.raster_op) {
        case 0x00:	/* None */
            return (input & mask) | (VGA.vga.latch.d & ~mask);
        case 0x01:	/* AND */
            return (input | ~mask) & VGA.vga.latch.d;
        case 0x02:	/* OR */
            return (input & mask) | VGA.vga.latch.d;
        case 0x03:	/* XOR */
            return (input & mask) ^ VGA.vga.latch.d;
        }
        return 0;
    }

    static private /*Bit32u*/int ModeOperation(/*Bit8u*/int val) {
        /*Bit32u*/int full;
        switch (VGA.vga.config.write_mode) {
        case 0x00:
            // Write Mode 0: In this mode, the host data is first rotated as per the Rotate Count field, then the Enable Set/Reset mechanism selects data from this or the Set/Reset field. Then the selected Logical Operation is performed on the resulting data and the data in the latch register. Then the Bit Mask field is used to select which bits come from the resulting data and which come from the latch register. Finally, only the bit planes enabled by the Memory Plane Write Enable field are written to memory.
            val=((val >> VGA.vga.config.data_rotate) | (val << (8-VGA.vga.config.data_rotate))) & 0xFF;
            full=VGA.ExpandTable[val];
            full=(full & VGA.vga.config.full_not_enable_set_reset) | VGA.vga.config.full_enable_and_set_reset;
            full=RasterOp(full,VGA.vga.config.full_bit_mask);
            break;
        case 0x01:
            // Write Mode 1: In this mode, data is transferred directly from the 32 bit latch register to display memory, affected only by the Memory Plane Write Enable field. The host data is not used in this mode.
            full=VGA.vga.latch.d;
            break;
        case 0x02:
            //Write Mode 2: In this mode, the bits 3-0 of the host data are replicated across all 8 bits of their respective planes. Then the selected Logical Operation is performed on the resulting data and the data in the latch register. Then the Bit Mask field is used to select which bits come from the resulting data and which come from the latch register. Finally, only the bit planes enabled by the Memory Plane Write Enable field are written to memory.
            full=RasterOp(VGA.FillTable[val&0xF],VGA.vga.config.full_bit_mask);
            break;
        case 0x03:
            // Write Mode 3: In this mode, the data in the Set/Reset field is used as if the Enable Set/Reset field were set to 1111b. Then the host data is first rotated as per the Rotate Count field, then logical ANDed with the value of the Bit Mask field. The resulting value is used on the data obtained from the Set/Reset field in the same way that the Bit Mask field would ordinarily be used. to select which bits come from the expansion of the Set/Reset field and which come from the latch register. Finally, only the bit planes enabled by the Memory Plane Write Enable field are written to memory.
            val=((val >> VGA.vga.config.data_rotate) | (val << (8-VGA.vga.config.data_rotate))) & 0xFF;
            full=RasterOp(VGA.vga.config.full_set_reset,VGA.ExpandTable[val] & VGA.vga.config.full_bit_mask);
            break;
        default:
            if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:Unsupported write mode "+VGA.vga.config.write_mode);
            full=0;
            break;
        }
        return full;
    }

    /* Gonna assume that whoever maps vga memory, maps it on 32/64kb boundary */

    static private final int VGA_PAGES = (128/4);
    static private final int VGA_PAGE_A0 = (0xA0000/4096);
    static private final int VGA_PAGE_B0 = (0xB0000/4096);
    static private final int VGA_PAGE_B8 = (0xB8000/4096);

    private static class VGAPages {
        /*Bitu*/int base, mask;
    }
    private static VGAPages vgapages = new VGAPages();

    private static class VGA_UnchainedRead_Handler extends Paging.PageHandler {
        public /*Bitu*/int readHandler(/*PhysPt*/int start) {
            VGA.vga.latch.d=RAM.readd(VGA.vga.mem.linear+start*4);
            switch (VGA.vga.config.read_mode) {
            case 0:
                return (VGA.vga.latch.b(VGA.vga.config.read_map_select));
            case 1:
                VGA.VGA_Latch templatch=new VGA.VGA_Latch();
                templatch.d=(VGA.vga.latch.d &	VGA.FillTable[VGA.vga.config.color_dont_care]) ^ VGA.FillTable[VGA.vga.config.color_compare & VGA.vga.config.color_dont_care];
                return (~(templatch.b(0) | templatch.b(1) | templatch.b(2) | templatch.b(3))) & 0xFF;
            }
            return 0;
        }
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED2(addr);
            return readHandler(addr);
        }
        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED2(addr);
            int result = readHandler(addr);
            result|= (readHandler(addr + 1) << 8);
            return result;
        }
        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED2(addr);
            int result = readHandler(addr);
            result |= (readHandler(addr+1) << 8);
            result |= (readHandler(addr+2) << 16);
            result |= (readHandler(addr+3) << 24);
            return result;
        }
    }

    private static class VGA_ChainedEGA_Handler extends Paging.PageHandler {
        public /*Bitu*/int readHandler(/*PhysPt*/int addr) {
            return RAM.readb(VGA.vga.mem.linear+addr);
        }
        public void writeHandler(/*PhysPt*/int s, /*Bit8u*/int val) {
            int start = (int)s;
            ModeOperation(val);
            /* Update video memory and the pixel buffer */
            VGA.VGA_Latch pixels = new VGA.VGA_Latch();
            RAM.writeb(VGA.vga.mem.linear+start, (short)val);
            start >>= 2;
            pixels.d=RAM.readd(VGA.vga.mem.linear+start*4);

            /*Bit32u*/int colors0_3, colors4_7;
            VGA.VGA_Latch temp=new VGA.VGA_Latch();temp.d=(pixels.d>>4) & 0x0f0f0f0f;
            colors0_3 =
                VGA.Expand16Table[0][temp.b(0)] |
                VGA.Expand16Table[1][temp.b(1)] |
                VGA.Expand16Table[2][temp.b(2)] |
                VGA.Expand16Table[3][temp.b(3)];
            RAM.writed(VGA.vga.fastmem+start<<3, colors0_3);
            temp.d=pixels.d & 0x0f0f0f0f;
            colors4_7 =
                VGA.Expand16Table[0][temp.b(0)] |
                VGA.Expand16Table[1][temp.b(1)] |
                VGA.Expand16Table[2][temp.b(2)] |
                VGA.Expand16Table[3][temp.b(3)];
            RAM.writed(VGA.vga.fastmem+(start<<3)+4, colors4_7);
        }

        public VGA_ChainedEGA_Handler()  {
            flags=Paging.PFLAG_NOCODE;
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED(addr);
//            MEM_CHANGED( addr << 3);
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED(addr);
//            MEM_CHANGED( addr << 3);
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
            writeHandler(addr+1,(/*Bit8u*/short)(val >> 8));
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED(addr);
//            MEM_CHANGED( addr << 3);
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
            writeHandler(addr+1,(/*Bit8u*/short)(val >> 8));
            writeHandler(addr+2,(/*Bit8u*/short)(val >> 16));
            writeHandler(addr+3,(/*Bit8u*/short)(val >> 24));
        }
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            return readHandler(addr);
        }
        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            int result = readHandler(addr);
            result |= (readHandler(addr+1) << 8);
            return result;
        }
        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            int result = readHandler(addr);
            result |= (readHandler(addr+1) << 8);
            result |= (readHandler(addr+2) << 16);
            result |= (readHandler(addr+3) << 24);
            return result;
        }
    }

    private static class VGA_UnchainedEGA_Handler extends VGA_UnchainedRead_Handler {
        //template< bool wrapping>
        public void writeHandler(/*PhysPt*/int start, /*Bit8u*/short val) {
            /*Bit32u*/int data=ModeOperation(val);
            /* Update video memory and the pixel buffer */
            VGA.VGA_Latch pixels = new VGA.VGA_Latch();
            pixels.d=RAM.readd(VGA.vga.mem.linear+start*4);
            pixels.d&=VGA.vga.config.full_not_map_mask;
            pixels.d|=(data & VGA.vga.config.full_map_mask);
            RAM.writed(VGA.vga.mem.linear+start*4, pixels.d);

            /*Bit32u*/int colors0_3, colors4_7;
            VGA.VGA_Latch temp=new VGA.VGA_Latch();temp.d=(pixels.d>>4) & 0x0f0f0f0f;
                colors0_3 =
                VGA.Expand16Table[0][temp.b(0)] |
                VGA.Expand16Table[1][temp.b(1)] |
                VGA.Expand16Table[2][temp.b(2)] |
                VGA.Expand16Table[3][temp.b(3)];
            RAM.writed(VGA.vga.fastmem+(start<<3), colors0_3);
            temp.d=pixels.d & 0x0f0f0f0f;
            colors4_7 =
                VGA.Expand16Table[0][temp.b(0)] |
                VGA.Expand16Table[1][temp.b(1)] |
                VGA.Expand16Table[2][temp.b(2)] |
                VGA.Expand16Table[3][temp.b(3)];
            RAM.writed(VGA.vga.fastmem+(start<<3)+4, colors4_7);
        }
        public VGA_UnchainedEGA_Handler()  {
            flags=Paging.PFLAG_NOCODE;
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED2(addr);
//            MEM_CHANGED( addr << 3);
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED2(addr);
//            MEM_CHANGED( addr << 3);
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
            writeHandler(addr+1,(/*Bit8u*/short)(val >> 8));
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED2(addr);
//            MEM_CHANGED( addr << 3);
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
            writeHandler(addr+1,(/*Bit8u*/short)(val >> 8));
            writeHandler(addr+2,(/*Bit8u*/short)(val >> 16));
            writeHandler(addr+3,(/*Bit8u*/short)(val >> 24));
        }
    }

    //Slighly unusual version, will directly write 8,16,32 bits values
    static private class VGA_ChainedVGA_Handler extends Paging.PageHandler {
        VGA_ChainedVGA_Handler()  {
            flags=Paging.PFLAG_NOCODE;
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr ) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            return RAM.readb(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
        }
        public /*Bitu*/int readw(/*PhysPt*/int addr ) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            if ((addr & 1)!=0) {
                int a = RAM.readb(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
                int b = RAM.readb(VGA.vga.mem.linear + (((addr+1) & ~3) << 2) + ((addr+1) & 3));
                return a | (b << 8);
            }
            return RAM.readw(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
        }
        public /*Bitu*/int readd(/*PhysPt*/int addr ) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            if ((addr & 3)!=0) {
                int a = RAM.readb(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
                int b = RAM.readb(VGA.vga.mem.linear + (((addr+1) & ~3) << 2) + ((addr+1) & 3));
                int c = RAM.readb(VGA.vga.mem.linear + (((addr+2) & ~3) << 2) + ((addr+2) & 3));
                int d = RAM.readb(VGA.vga.mem.linear + (((addr+3) & ~3) << 2) + ((addr+3) & 3));
                return a | (b << 8) | (c << 16) | (d << 24);
            }
            return RAM.readd(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
        }
        public void writeb(/*PhysPt*/int addr, /*Bitu*/int val ) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;

            RAM.writeb(VGA.vga.mem.linear+((addr & ~3) << 2) + (addr & 3), (short)val);

            // Cache
            RAM.writeb(VGA.vga.fastmem+addr, (short)val);
            if (addr < 320) // And replicate the first line
                RAM.writeb(VGA.vga.fastmem+addr+64*1024, (short)val);
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED(addr);
//            MEM_CHANGED( addr );
    //		MEM_CHANGED( addr + 1);
            if ((addr & 1)!=0) {
                RAM.writebs(VGA.vga.mem.linear+((addr & ~3) << 2) + (addr & 3), (byte)val);
                RAM.writebs(VGA.vga.mem.linear+(((addr+1) & ~3) << 2) + ((addr+1) & 3), (byte)(val>>8));
            } else {
                RAM.writew(VGA.vga.mem.linear+((addr & ~3) << 2) + (addr & 3), val);
            }

            // Cache
            RAM.writew(VGA.vga.fastmem+addr, val);
            if (addr < 320) // And replicate the first line
                RAM.writew(VGA.vga.fastmem+addr+64*1024, val);
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED(addr);
//            MEM_CHANGED( addr );
    //		MEM_CHANGED( addr + 3);

            if ((addr & 3)!=0) {
                RAM.writebs(VGA.vga.mem.linear+((addr & ~3) << 2) + (addr & 3), (byte)val);
                RAM.writebs(VGA.vga.mem.linear+(((addr+1) & ~3) << 2) + ((addr+1) & 3), (byte)(val>>8));
                RAM.writebs(VGA.vga.mem.linear+(((addr+2) & ~3) << 2) + ((addr+2) & 3), (byte)(val>>16));
                RAM.writebs(VGA.vga.mem.linear+(((addr+3) & ~3) << 2) + ((addr+3) & 3), (byte)(val>>24));
            } else {
                RAM.writed(VGA.vga.mem.linear+((addr & ~3) << 2) + (addr & 3), val);
            }

            // Cache
            RAM.writed(VGA.vga.fastmem+addr, val);
            if (addr < 320) // And replicate the first line
                RAM.writed(VGA.vga.fastmem+addr+64*1024, val);
        }
    }

     static private class VGA_UnchainedVGA_Handler extends VGA_UnchainedRead_Handler {
        public void writeHandler( /*PhysPt*/int addr, /*Bit8u*/int val ) {
            addr <<= 2;
            /*Bit32u*/int data=ModeOperation(val);
            int d=RAM.readd(VGA.vga.mem.linear+addr);
            d&=VGA.vga.config.full_not_map_mask;
            d|=(data & VGA.vga.config.full_map_mask);
            RAM.writed(VGA.vga.mem.linear+addr, d);
    //		if(VGA.vga.config.compatible_chain4)
    //			((/*Bit32u*/long*)VGA.vga.mem.linear)[CHECKED2(addr+64*1024)]=pixels.d;
        }

        public VGA_UnchainedVGA_Handler()  {
            flags=Paging.PFLAG_NOCODE;
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            int a = (int)(addr & vgapages.mask);
            a += VGA.vga.svga.bank_write_full;
            //addr = CHECKED2(addr);
            //MEM_CHANGED( addr << 2 );
            writeHandler(a,val);
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            int a = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            //addr = CHECKED2(addr);
            //MEM_CHANGED( addr << 2);
            writeHandler(a,val);
            writeHandler(a+1,val >> 8);
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            int a = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            //addr = CHECKED2(addr);
            //MEM_CHANGED( addr << 2);
            writeHandler(a,val);
            writeHandler(a+1,val >> 8);
            writeHandler(a+2,val >> 16);
            writeHandler(a+3,val >>> 24);
        }
    }

    static private class VGA_TEXT_PageHandler extends Paging.PageHandler {
        public VGA_TEXT_PageHandler() {
            flags=Paging.PFLAG_NOCODE;
        }
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            switch(VGA.vga.gfx.read_map_select) {
            case 0: // character index
                return RAM.readb(VGA.vga.mem.linear+CHECKED3(VGA.vga.svga.bank_read_full+addr));
            case 1: // character attribute
                return RAM.readb(VGA.vga.mem.linear+CHECKED3(VGA.vga.svga.bank_read_full+addr+1));
            case 2: // font map
                return VGA.vga.draw.font[addr];
            default: // 3=unused, but still RAM that could save values
                return 0;
            }
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val){
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            if (VGA.vga.seq.map_mask == 4) {
                VGA.vga.draw.font[addr]=(/*Bit8u*/byte)val;
            } else {
                if ((VGA.vga.seq.map_mask & 0x4)!=0)
                    VGA.vga.draw.font[addr]=(/*Bit8u*/byte)val;
                if ((VGA.vga.seq.map_mask & 0x2)!=0) // character attribute
                    RAM.writeb(VGA.vga.mem.linear+CHECKED3(VGA.vga.svga.bank_read_full+addr+1), (short)val);
                if ((VGA.vga.seq.map_mask & 0x1)!=0) // character index
                    RAM.writeb(VGA.vga.mem.linear+CHECKED3(VGA.vga.svga.bank_read_full+addr), (short)val);
            }
        }
    }

    static private class VGA_Map_Handler extends Paging.PageHandler {
        public VGA_Map_Handler() {
            flags=Paging.PFLAG_READABLE|Paging.PFLAG_WRITEABLE|Paging.PFLAG_NOCODE;
        }
        public /*HostPt*/int GetHostReadPt(/*Bitu*/int phys_page) {
            phys_page-=vgapages.base;
            return VGA.vga.mem.linear+((VGA.vga.svga.bank_read_full+phys_page*4096)&(VGA.vga.vmemwrap-1));
        }
        public /*HostPt*/int GetHostWritePt(/*Bitu*/int phys_page) {
            phys_page-=vgapages.base;
            return VGA.vga.mem.linear+((VGA.vga.svga.bank_write_full+phys_page*4096)&(VGA.vga.vmemwrap-1));
        }
    }

    static private class VGA_Changes_Handler extends Paging.PageHandler {
        public VGA_Changes_Handler() {
            flags=Paging.PFLAG_NOCODE;
        }
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            return RAM.readb(VGA.vga.mem.linear+addr);
        }
        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            return RAM.readw(VGA.vga.mem.linear+addr);
        }
        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
//            addr = CHECKED(addr);
            return RAM.readd(VGA.vga.mem.linear+addr);
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED(addr);
//            MEM_CHANGED( addr );
            RAM.writeb(VGA.vga.mem.linear+addr, (short)val);
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED(addr);
//            MEM_CHANGED( addr );
            RAM.writew(VGA.vga.mem.linear+addr, val);
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
//            addr = CHECKED(addr);
//            MEM_CHANGED( addr );
            RAM.writed(VGA.vga.mem.linear+addr, val);
        }
    }

    static private class VGA_LIN4_Handler extends VGA_UnchainedEGA_Handler {
        public VGA_LIN4_Handler() {
            flags=Paging.PFLAG_NOCODE;
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = VGA.vga.svga.bank_write_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr&=(VGA.vga.vmemwrap>>2)-1;
//            addr = CHECKED4(addr);
//            MEM_CHANGED( addr << 3 );
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = VGA.vga.svga.bank_write_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr&=(VGA.vga.vmemwrap>>2)-1;
//            addr = CHECKED4(addr);
//            MEM_CHANGED( addr << 3 );
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
            writeHandler(addr+1,(/*Bit8u*/short)(val >> 8));
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = VGA.vga.svga.bank_write_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr&=(VGA.vga.vmemwrap>>2)-1;
//            addr = CHECKED4(addr);
//            MEM_CHANGED( addr << 3 );
            writeHandler(addr+0,(/*Bit8u*/short)(val >> 0));
            writeHandler(addr+1,(/*Bit8u*/short)(val >> 8));
            writeHandler(addr+2,(/*Bit8u*/short)(val >> 16));
            writeHandler(addr+3,(/*Bit8u*/short)(val >> 24));
        }
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            addr = VGA.vga.svga.bank_read_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr&=(VGA.vga.vmemwrap>>2)-1;
//            addr = CHECKED4(addr);
            return readHandler(addr);
        }
        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            addr = VGA.vga.svga.bank_read_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr&=(VGA.vga.vmemwrap>>2)-1;
//            addr = CHECKED4(addr);
            int result = readHandler(addr);
            result |= (readHandler(addr+1) << 8);
            return result;
        }
        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            addr = VGA.vga.svga.bank_read_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr&=(VGA.vga.vmemwrap>>2)-1;
//            addr = CHECKED4(addr);
            int result = readHandler(addr);
            result |= (readHandler(addr+1) << 8);
            result |= (readHandler(addr+2) << 16);
            result |= (readHandler(addr+3) << 24);
            return result;
        }
    }

    static private class VGA_LFBChanges_Handler extends Paging.PageHandler {
        public VGA_LFBChanges_Handler() {
            flags=Paging.PFLAG_NOCODE;
        }
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
//            addr = CHECKED(addr);
            return RAM.readb(VGA.vga.mem.linear+addr);
        }
        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
//            addr = CHECKED(addr);
            return RAM.readw(VGA.vga.mem.linear+addr);
        }
        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
//            addr = CHECKED(addr);
            return RAM.readd(VGA.vga.mem.linear+addr);
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
//            addr = CHECKED(addr);
            RAM.writeb(VGA.vga.mem.linear+addr, (short)val);
//            MEM_CHANGED( addr );
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
//            addr = CHECKED(addr);
            RAM.writew(VGA.vga.mem.linear+addr, val);
//            MEM_CHANGED( addr );
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
//            addr = CHECKED(addr);
            RAM.writed(VGA.vga.mem.linear+addr, val);
//            MEM_CHANGED( addr );
        }
    }

    static private class VGA_LFB_Handler extends Paging.PageHandler {
        public VGA_LFB_Handler() {
            flags=Paging.PFLAG_READABLE|Paging.PFLAG_WRITEABLE|Paging.PFLAG_NOCODE;
        }
        public /*HostPt*/int GetHostReadPt( /*Bitu*/int phys_page ) {
            phys_page -= VGA.vga.lfb.page;
            return VGA.vga.mem.linear + ((phys_page * 4096)&(VGA.vga.vmemwrap-1));
        }
        public /*HostPt*/int GetHostWritePt( /*Bitu*/int phys_page ) {
            return GetHostReadPt( phys_page );
        }
    }

    private static class VGA_MMIO_Handler extends Paging.PageHandler {
        public VGA_MMIO_Handler() {
            flags=Paging.PFLAG_NOCODE;
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
            /*Bitu*/int port = (int)Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            VGA_xga.XGA_Write.call(port, val, 1);
        }
        public void writew(/*PhysPt*/int addr,/*Bitu*/int val) {
            /*Bitu*/int port = (int)Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            VGA_xga.XGA_Write.call(port, val, 2);
        }
        public void writed(/*PhysPt*/int addr,/*Bitu*/int val) {
            /*Bitu*/int port = (int)Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            VGA_xga.XGA_Write.call(port, val, 4);
        }

        public /*Bitu*/int readb(/*PhysPt*/int addr) {
            /*Bitu*/int port = (int)Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            return VGA_xga.XGA_Read.call(port, 1);
        }
        public /*Bitu*/int readw(/*PhysPt*/int addr) {
            /*Bitu*/int port = (int)Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            return VGA_xga.XGA_Read.call(port, 2);
        }
        public /*Bitu*/int readd(/*PhysPt*/int addr) {
            /*Bitu*/int port = (int)Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            return VGA_xga.XGA_Read.call(port, 4);
        }
    }

    static private class VGA_TANDY_PageHandler extends Paging.PageHandler {
        public VGA_TANDY_PageHandler() {
            flags=Paging.PFLAG_READABLE|Paging.PFLAG_WRITEABLE;
    //			|Paging.PFLAG_NOCODE;
        }
        public /*HostPt*/int GetHostReadPt(/*Bitu*/int phys_page) {
            // Odd banks are limited to 16kB and repeated
            if ((VGA.vga.tandy.mem_bank & 1)!=0)
                phys_page&=0x03;
            else
                phys_page&=0x07;
            return VGA.vga.tandy.mem_base + (phys_page * 4096);
        }
        public /*HostPt*/int GetHostWritePt(/*Bitu*/int phys_page) {
            return GetHostReadPt( phys_page );
        }
    }


    static private class VGA_PCJR_Handler extends Paging.PageHandler {
        public VGA_PCJR_Handler() {
            flags=Paging.PFLAG_READABLE|Paging.PFLAG_WRITEABLE;
        }
        public /*HostPt*/int GetHostReadPt(/*Bitu*/int phys_page) {
            phys_page-=0xb8;
            // The 16kB map area is repeated in the 32kB range
		    // On CGA CPU A14 is not decoded so it repeats there too
		    phys_page&=0x03;
            return VGA.vga.tandy.mem_base + (phys_page * 4096);
        }
        public /*HostPt*/int GetHostWritePt(/*Bitu*/int phys_page) {
            return GetHostReadPt( phys_page );
        }
    }

    private static class VGA_Empty_Handler extends Paging.PageHandler {
        public VGA_Empty_Handler() {
            flags=Paging.PFLAG_NOCODE;
        }
        public /*Bitu*/int readb(/*PhysPt*/int addr) {
    //		LOG(LOG_VGA, LOG_NORMAL ) ( "Read from empty memory space at %x", addr );
            return 0xff;
        }
        public void writeb(/*PhysPt*/int addr,/*Bitu*/int val) {
    //		LOG(LOG_VGA, LOG_NORMAL ) ( "Write %x to empty memory space at %x", val, addr );
        }
    }

    private static class vg {
        public VGA_Map_Handler				map = new VGA_Map_Handler();
        public VGA_Changes_Handler			changes = new VGA_Changes_Handler();
        public VGA_TEXT_PageHandler		    text = new VGA_TEXT_PageHandler();
        public VGA_TANDY_PageHandler		tandy = new VGA_TANDY_PageHandler();
        public VGA_ChainedEGA_Handler		cega = new VGA_ChainedEGA_Handler();
        public VGA_ChainedVGA_Handler		cvga = new VGA_ChainedVGA_Handler();
        public VGA_UnchainedEGA_Handler	    uega = new VGA_UnchainedEGA_Handler();
        public VGA_UnchainedVGA_Handler	    uvga = new VGA_UnchainedVGA_Handler();
        public VGA_PCJR_Handler			    pcjr = new VGA_PCJR_Handler();
        public VGA_LIN4_Handler			    lin4 = new VGA_LIN4_Handler();
        public VGA_LFB_Handler				lfb =   new VGA_LFB_Handler();
        public VGA_LFBChanges_Handler		lfbchanges = new VGA_LFBChanges_Handler();
        public VGA_MMIO_Handler			    mmio = new VGA_MMIO_Handler();
        public VGA_Empty_Handler			empty = new VGA_Empty_Handler();
    }
    static private vg vgaph = new vg();

    private static void VGA_ChangedBank() {
        if (!VGA.VGA_LFB_MAPPED) {
            //If the mode is accurate than the correct mapper must have been installed already
            if ( VGA.vga.mode >= VGA.M_LIN4 && VGA.vga.mode <= VGA.M_LIN32 ) {
                return;
            }
        }
        VGA_SetupHandlers();
    }

    private static void rangeDone() {
        Paging.PAGING_ClearTLB();
    }

    public static void VGA_SetupHandlers() {
        VGA.vga.svga.bank_read_full = VGA.vga.svga.bank_read*VGA.vga.svga.bank_size;
        VGA.vga.svga.bank_write_full = VGA.vga.svga.bank_write*VGA.vga.svga.bank_size;

        Paging.PageHandler newHandler;
        switch (Dosbox.machine) {
        case MachineType.MCH_CGA:
        case MachineType.MCH_PCJR:
            Memory.MEM_SetPageHandler(VGA_PAGE_B8, 8, vgaph.pcjr);
            rangeDone();
            return;
        case MachineType.MCH_HERC:
            vgapages.base=VGA_PAGE_B0;
            if ((VGA.vga.herc.enable_bits & 0x2)!=0) {
                vgapages.mask=0xffff;
                Memory.MEM_SetPageHandler(VGA_PAGE_B0,16,vgaph.map);
            } else {
                vgapages.mask=0x7fff;
                /* With hercules in 32kb mode it leaves a memory hole on 0xb800 */
                Memory.MEM_SetPageHandler(VGA_PAGE_B0,8,vgaph.map);
                Memory.MEM_SetPageHandler(VGA_PAGE_B8,8,vgaph.empty);
            }
            rangeDone();
            return;
        case MachineType.MCH_TANDY:
            /* Always map 0xa000 - 0xbfff, might overwrite 0xb800 */
            vgapages.base=VGA_PAGE_A0;
            vgapages.mask=0x1ffff;
            Memory.MEM_SetPageHandler(VGA_PAGE_A0, 32, vgaph.map );
            if ( (VGA.vga.tandy.extended_ram & 1 )!=0) {
                //You seem to be able to also map different 64kb banks, but have to figure that out
                //This seems to work so far though
                VGA.vga.tandy.draw_base = VGA.vga.mem.linear;
                VGA.vga.tandy.mem_base = VGA.vga.mem.linear;
            } else {
                VGA.vga.tandy.draw_base = 0x80000+VGA.vga.tandy.draw_bank * 16 * 1024;
                VGA.vga.tandy.mem_base = 0x80000+VGA.vga.tandy.mem_bank * 16 * 1024;
                Memory.MEM_SetPageHandler( 0xb8, 8, vgaph.tandy );
            }
            rangeDone();
            return;
    //		Memory.MEM_SetPageHandler(VGA.vga.tandy.mem_bank<<2,VGA.vga.tandy.is_32k_mode ? 0x08 : 0x04,range_handler);
        // EGAVGA_ARCH_CASE
        case MachineType.MCH_EGA:
        case MachineType.MCH_VGA:
            break;
        default:
            Log.log_msg("Illegal machine type "+Dosbox.machine);
            return;
        }

        /* This should be vga only */
        switch (VGA.vga.mode) {
        case VGA.M_ERROR:
        default:
            return;
        case VGA.M_LIN4:
            newHandler = vgaph.lin4;
            break;
        case VGA.M_LIN15:
        case VGA.M_LIN16:
        case VGA.M_LIN32:
            if (VGA.VGA_LFB_MAPPED)
                newHandler = vgaph.map;
            else
                newHandler = vgaph.changes;
            break;
        case VGA.M_LIN8:
        case VGA.M_VGA:
            if (VGA.vga.config.chained) {
                if(VGA.vga.config.compatible_chain4)
                    newHandler = vgaph.cvga;
                else
                    if (VGA.VGA_LFB_MAPPED)
                        newHandler = vgaph.map;
                    else
                        newHandler = vgaph.changes;
            } else {
                newHandler = vgaph.uvga;
            }
            break;
        case VGA.M_EGA:
            if (VGA.vga.config.chained)
                newHandler = vgaph.cega;
            else
                newHandler = vgaph.uega;
            break;
        case VGA.M_TEXT:
            /* Check if we're not in odd/even mode */
            if ((VGA.vga.gfx.miscellaneous & 0x2)!=0) newHandler = vgaph.map;
            else newHandler = vgaph.text;
            break;
        case VGA.M_CGA4:
        case VGA.M_CGA2:
            newHandler = vgaph.map;
            break;
        }
        switch ((VGA.vga.gfx.miscellaneous >>> 2) & 3) {
        case 0:
            vgapages.base = VGA_PAGE_A0;
            switch (Dosbox.svgaCard) {
            case SVGACards.SVGA_TsengET3K:
            case SVGACards.SVGA_TsengET4K:
                vgapages.mask = 0xffff;
                break;
            case SVGACards.SVGA_S3Trio:
            default:
                vgapages.mask = 0x1ffff;
                break;
            }
            Memory.MEM_SetPageHandler(VGA_PAGE_A0, 32, newHandler );
            break;
        case 1:
            vgapages.base = VGA_PAGE_A0;
            vgapages.mask = 0xffff;
            Memory.MEM_SetPageHandler( VGA_PAGE_A0, 16, newHandler );
            Memory.MEM_ResetPageHandler( VGA_PAGE_B0, 16);
            break;
        case 2:
            vgapages.base = VGA_PAGE_B0;
            vgapages.mask = 0x7fff;
            Memory.MEM_SetPageHandler( VGA_PAGE_B0, 8, newHandler );
            Memory.MEM_ResetPageHandler( VGA_PAGE_A0, 16 );
            Memory.MEM_ResetPageHandler( VGA_PAGE_B8, 8 );
            break;
        case 3:
            vgapages.base = VGA_PAGE_B8;
            vgapages.mask = 0x7fff;
            Memory.MEM_SetPageHandler( VGA_PAGE_B8, 8, newHandler );
            Memory.MEM_ResetPageHandler( VGA_PAGE_A0, 16 );
            Memory.MEM_ResetPageHandler( VGA_PAGE_B0, 8 );
            break;
        }
        if(Dosbox.svgaCard == SVGACards.SVGA_S3Trio && (VGA.vga.s3.ext_mem_ctrl & 0x10)!=0)
            Memory.MEM_SetPageHandler(VGA_PAGE_A0, 16, vgaph.mmio);
        rangeDone();
    }

    public static void VGA_StartUpdateLFB() {
        VGA.vga.lfb.page = VGA.vga.s3.la_window << 4;
        VGA.vga.lfb.addr = VGA.vga.s3.la_window << 16;
        if (VGA.VGA_LFB_MAPPED)
            VGA.vga.lfb.handler = vgaph.lfb;
        else
            VGA.vga.lfb.handler = vgaph.lfbchanges;
        Memory.MEM_SetLFB(VGA.vga.s3.la_window << 4 ,(int)(VGA.vga.vmemsize/4096), VGA.vga.lfb.handler, vgaph.mmio);
    }

    public static Section.SectionFunction VGA_Memory_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            VGA.vga.mem.linear_orgptr = 0;
            VGA.vga.mem.linear = 0;
            VGA.vga.fastmem_orgptr = 0;
            VGA.vga.fastmem = 0;
        }
    };

    public static Section.SectionFunction VGA_SetupMemory = new Section.SectionFunction() {
        public void call(Section sec) {
            VGA.vga.svga.bank_read = VGA.vga.svga.bank_write = 0;
            VGA.vga.svga.bank_read_full = VGA.vga.svga.bank_write_full = 0;

            /*Bit32u*/int vga_allocsize=VGA.vga.vmemsize;
            // Keep lower limit at 512k
            if (vga_allocsize<512*1024) vga_allocsize=512*1024;
            // We reserve extra 2K for one scan line
            vga_allocsize+=2048;
            VGA.vga.mem.linear_orgptr = Memory.allocate(vga_allocsize);
            VGA.vga.mem.linear=VGA.vga.mem.linear_orgptr;

            VGA.vga.fastmem_orgptr = Memory.allocate(Memory.videoCacheSize+4096);
            VGA.vga.fastmem = VGA.vga.fastmem_orgptr;

            VGA_draw.TempLine = Memory.allocate(VGA_draw.TEMPLINE_SIZE);

            // In most cases these values stay the same. Assumptions: vmemwrap is power of 2,
            // vmemwrap <= vmemsize, fastmem implicitly has mem wrap twice as big
            VGA.vga.vmemwrap = VGA.vga.vmemsize;

            if (VGA_KEEP_CHANGES) {
                VGA.vga.changes = new VGA.VGA_Changes();
                int changesMapSize = (VGA.vga.vmemsize >> VGA.VGA_CHANGE_SHIFT) + 32;
                VGA.vga.changes.map = new /*Bit8u*/Ptr(changesMapSize);
            }
            VGA.vga.svga.bank_read = VGA.vga.svga.bank_write = 0;
            VGA.vga.svga.bank_read_full = VGA.vga.svga.bank_write_full = 0;
            VGA.vga.svga.bank_size = 0x10000; /* most common bank size is 64K */

            sec.AddDestroyFunction(VGA_Memory_ShutDown);

            if (Dosbox.machine== MachineType.MCH_PCJR) {
                /* PCJr does not have dedicated graphics memory but uses
                   conventional memory below 128k */
                //TODO map?
            }
        }
    };
}
