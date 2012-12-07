package jdos.hardware;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Paging;
import jdos.gui.Render;
import jdos.ints.Int10_vesa;
import jdos.misc.Log;

public class VBE {
    static public boolean initialized = false;

    private static final int VBE_DISPI_INDEX_ID = 0x0;
    private static final int VBE_DISPI_INDEX_XRES = 0x1;
    private static final int VBE_DISPI_INDEX_YRES = 0x2;
    private static final int VBE_DISPI_INDEX_BPP = 0x3;
    private static final int VBE_DISPI_INDEX_ENABLE = 0x4;
    private static final int VBE_DISPI_INDEX_BANK = 0x5;
    private static final int VBE_DISPI_INDEX_VIRT_WIDTH = 0x6;
    private static final int VBE_DISPI_INDEX_VIRT_HEIGHT = 0x7;
    private static final int VBE_DISPI_INDEX_X_OFFSET = 0x8;
    private static final int VBE_DISPI_INDEX_Y_OFFSET = 0x9;
    private static final int VBE_DISPI_INDEX_NB = 0xa;
    private static final int VBE_DISPI_INDEX_VIDEO_MEMORY_64K = 0xa; /* read-only, not in vbe_regs */

    private static final int VBE_DISPI_DISABLED =            0x00;
    private static final int VBE_DISPI_ENABLED =             0x01;
    private static final int VBE_DISPI_GETCAPS =             0x02;
    private static final int VBE_DISPI_8BIT_DAC =            0x20;
    private static final int VBE_DISPI_LFB_ENABLED =         0x40;
    private static final int VBE_DISPI_NOCLEARMEM =          0x80;

    private static final int VBE_DISPI_ID0 = 0xB0C0;
    private static final int VBE_DISPI_ID1 = 0xB0C1;
    private static final int VBE_DISPI_ID2 = 0xB0C2;
    private static final int VBE_DISPI_ID3 = 0xB0C3;
    private static final int VBE_DISPI_ID4 = 0xB0C4;

    private static final int VBE_DISPI_MAX_XRES =           1600;
    private static final int VBE_DISPI_MAX_YRES =           1200;
    private static final int VBE_DISPI_MAX_BPP =            32;

    static private int vbeIndex;
    static private final int[] vbeRegs = new int[VBE_DISPI_INDEX_NB];

    static {
        vbeRegs[VBE_DISPI_INDEX_ID] = VBE_DISPI_ID0;
        vbeRegs[VBE_DISPI_INDEX_XRES] = VBE_DISPI_MAX_XRES;
        vbeRegs[VBE_DISPI_INDEX_YRES] = VBE_DISPI_MAX_YRES;
        vbeRegs[VBE_DISPI_INDEX_BPP] = VBE_DISPI_MAX_BPP;
    }

    static final private IoHandler.IO_ReadHandler vbe_read_index = new IoHandler.IO_ReadHandler() {
        public int call(int port, int iolen) {
            return vbeIndex;
        }
    };
    static final private IoHandler.IO_ReadHandler vbe_read_data = new IoHandler.IO_ReadHandler() {
        public int call(int port, int iolen) {
            int val = 0;
            if (vbeIndex<VBE_DISPI_INDEX_NB) {
                if ((vbeRegs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_GETCAPS)!=0) {
                    switch (vbeIndex) {
                        case VBE_DISPI_INDEX_XRES:
                            val = VBE_DISPI_MAX_XRES;
                            break;
                        case VBE_DISPI_INDEX_YRES:
                            val = VBE_DISPI_MAX_YRES;
                            break;
                        case VBE_DISPI_INDEX_BPP:
                            val = VBE_DISPI_MAX_BPP;
                            break;
                        default:
                            System.out.println("Ouch");
                    }
                } else {
                    switch (vbeIndex) {
                        case VBE_DISPI_INDEX_ID:
                        case VBE_DISPI_INDEX_ENABLE:
                            val = vbeRegs[vbeIndex] | VBE_DISPI_LFB_ENABLED;
                            break;
                        case VBE_DISPI_INDEX_XRES:
                            val = Render.render.src.width;
                            break;
                        case VBE_DISPI_INDEX_YRES:
                            val = Render.render.src.height;
                            break;
                        case VBE_DISPI_INDEX_BPP:
                            val = Render.render.src.bpp;
                            break;
                        default:
                            val = vbeRegs[vbeIndex];
                            System.out.println("Ouch");
                    }
                }
            } else if (vbeIndex == VBE_DISPI_INDEX_VIDEO_MEMORY_64K) {
                val = VGA.vga.vmemsize >>> 16;
            }
            System.out.println("VBE Read "+vbeIndex+" = "+val+" eip=0x"+Integer.toHexString(CPU_Regs.reg_eip));
            return val;
        }
    };

    static final private IoHandler.IO_WriteHandler vbe_write_index  = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            vbeIndex = val;
        }
    };

    static final private IoHandler.IO_WriteHandler vbe_write_data  = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int data, /*Bitu*/int iolen) {
            System.out.println("VBE Write "+vbeIndex+"="+data+" eip=0x"+Integer.toHexString(CPU_Regs.reg_eip));
            if (vbeIndex <= VBE_DISPI_INDEX_NB) {
                switch(vbeIndex) {
                    case VBE_DISPI_INDEX_ID:
                        if (data == VBE_DISPI_ID0 || data == VBE_DISPI_ID1 || data == VBE_DISPI_ID2 || data == VBE_DISPI_ID3 || data == VBE_DISPI_ID4)
                            vbeRegs[vbeIndex] = data;
                        break;
                    case VBE_DISPI_INDEX_XRES:
                        if ((data <= VBE_DISPI_MAX_XRES) && ((data & 7) == 0))
                            vbeRegs[vbeIndex] = data;
                        break;
                    case VBE_DISPI_INDEX_YRES:
                        if (data <= VBE_DISPI_MAX_YRES)
                            vbeRegs[vbeIndex] = data;
                        break;
                    case VBE_DISPI_INDEX_BANK:
                        data &= (VGA.vga.vmemsize >>> 16)-1;
                        vbeRegs[vbeIndex] = data;
                        // :TODO:
                        if (data != 0) {
                            Log.exit("Non 0 VBE_DISPI_INDEX_BANK not implemented yet");
                        }
                        // bankOffset = data << 16;
                        break;
                    case VBE_DISPI_INDEX_BPP:
                        if (data == 0)
                            data = 8;
                        if (data == 4 || data == 8 || data == 15 ||
                            data == 16 || data == 24 || data == 32) {
                            vbeRegs[vbeIndex] = data;
                        }
                        break;
                    case VBE_DISPI_INDEX_ENABLE:
                        if ((data & VBE_DISPI_ENABLED) != 0) {

                            vbeRegs[VBE_DISPI_INDEX_VIRT_WIDTH] = vbeRegs[VBE_DISPI_INDEX_XRES];
                            vbeRegs[VBE_DISPI_INDEX_VIRT_HEIGHT] = vbeRegs[VBE_DISPI_INDEX_YRES];
                            vbeRegs[VBE_DISPI_INDEX_X_OFFSET] = 0;
                            vbeRegs[VBE_DISPI_INDEX_Y_OFFSET] = 0;

                            int vbeLineOffset;
                            if (vbeRegs[VBE_DISPI_INDEX_BPP] == 4)
                                vbeLineOffset = vbeRegs[VBE_DISPI_INDEX_XRES] >>> 1;
                            else
                                vbeLineOffset = vbeRegs[VBE_DISPI_INDEX_XRES] * ((vbeRegs[VBE_DISPI_INDEX_BPP] + 7) >>> 3);

                            // :TODO:
                            // vbeStartAddress = 0;

                            /* clear the screen (should be done in BIOS) */
                            if ((vbeRegs[VBE_DISPI_INDEX_ENABLE] & VBE_DISPI_NOCLEARMEM) == 0)
                            {
                                int limit = vbeRegs[VBE_DISPI_INDEX_YRES] * vbeLineOffset;
                                Memory.host_zeroset(VGA.vga.mem.linear_orgptr, limit);
                            }
                            int y = vbeRegs[VBE_DISPI_INDEX_YRES];
                            int x = vbeRegs[VBE_DISPI_INDEX_XRES];
                            int mode = -1;
                            switch(vbeRegs[VBE_DISPI_INDEX_BPP]) {
                                case 15:
                                         if (x == 320 && y == 200) mode = 0x10D;
                                    else if (x == 320 && y == 240) mode = 0x160;
                                    else if (x == 320 && y == 400) mode = 0x161;
                                    else if (x == 320 && y == 480) mode = 0x162;
                                    else if (x == 640 && y == 400) mode = 0x165;
                                    else if (x == 640 && y == 480) mode = 0x110;
                                    else if (x == 800 && y == 600) mode = 0x113;
                                    else if (x ==1024 && y == 768) mode = 0x116;
                                    else if (x ==1152 && y == 864) mode = 0x209;
                            }
                            if (mode == -1)
                                return;
                            VGA.vga.crtc.read_only = false;
                            Int10_vesa.VESA_SetSVGAMode(mode);
                            VGA.vga.config.compatible_chain4 = false;

//                            /* we initialise the VGA graphic mode */
//                            /* (should be done in BIOS) */
//                            /* graphic mode + memory map 1 */
//                            //VGA.vga.gfx.miscellaneous = (byte)((VGA.vga.gfx.miscellaneous & ~0x0c) | 0x05);
//                            IoHandler.IO_Write(0x3ce, 0x06);
//                            IoHandler.IO_Write(0x3cf, ((VGA.vga.gfx.miscellaneous & ~0x0c) | 0x05) & 0xFF);
//
//                            /* no CGA modes */
//                            //VGA.vga.crtc.mode_control|=3;
//                            IoHandler.IO_Write(0x3d4, 0x17);
//                            IoHandler.IO_Write(0x3d5, VGA.vga.crtc.mode_control|=3);
//
//                            // VGA.vga.crtc.offset = (byte)(vbeLineOffset >>> 3);
//                            IoHandler.IO_Write(0x3d4, 0x13);
//                            IoHandler.IO_Write(0x3d5, vbeLineOffset >>> 3);
//
//                            /* width */
//                            //VGA.vga.crtc.horizontal_display_end = (byte)((vbeRegs[VBE_DISPI_INDEX_XRES] >>> 3) - 1);
//                            IoHandler.IO_Write(0x3d4, 0x01);
//                            IoHandler.IO_Write(0x3d5, (vbeRegs[VBE_DISPI_INDEX_XRES] >>> 3) - 1);
//
//                            /* height */
//                            int h = vbeRegs[VBE_DISPI_INDEX_YRES] - 1;
//                            // VGA.vga.crtc.vertical_display_end = (short)h;
//                            IoHandler.IO_Write(0x3d4, 0x12);
//                            IoHandler.IO_Write(0x3d5, h);
//
//                            // VGA.vga.crtc.overflow = (byte)((VGA.vga.crtc.overflow & ~ 0x42) | ((h >>> 7) & 0x02) | ((h >>> 3) & 40));
//                            IoHandler.IO_Write(0x3d4, 0x07);
//                            IoHandler.IO_Write(0x3d5, ((VGA.vga.crtc.overflow & ~ 0x42) | ((h >>> 7) & 0x02) | ((h >>> 3) & 40)) & 0xFF);
//
//                            /* line compare to 1023 */
//                            // VGA.vga.crtc.line_compare = 0xff;
//                            IoHandler.IO_Write(0x3d4, 0x18);
//                            IoHandler.IO_Write(0x3d5, 0xff);
//
//                            // VGA.vga.crtc.overflow |= 0x10;
//                            IoHandler.IO_Write(0x3d4, 0x07);
//                            IoHandler.IO_Write(0x3d5, VGA.vga.crtc.overflow | 0x10);
//
//                            // VGA.vga.crtc.maximum_scan_line |= 0x40;
//                            IoHandler.IO_Write(0x3d4, 0x09);
//                            IoHandler.IO_Write(0x3d5, VGA.vga.crtc.maximum_scan_line | 0x40);
//
//                            int shiftControl;
//                            if (vbeRegs[VBE_DISPI_INDEX_BPP] == 4) {
//                                shiftControl = 0;
//                                // VGA.vga.seq.clocking_mode &= 0x8;
//                                /* no double line */
//                                IoHandler.IO_Write(0x3c4, 0x01);
//                                IoHandler.IO_Write(0x3c5, VGA.vga.seq.clocking_mode & 0x8);
//                            } else {
//                                shiftControl = 2;
//                                // VGA.vga.seq.memory_mode |= 0x08;
//                                /* set chain 4 mode */
//                                IoHandler.IO_Write(0x3c4, 0x04);
//                                IoHandler.IO_Write(0x3c5, VGA.vga.seq.memory_mode | 0x08);
//
//                                // VGA.vga.seq.map_mask |= 0x0f;
//                                /* activate all planes */
//                                IoHandler.IO_Write(0x3c4, 0x02);
//                                IoHandler.IO_Write(0x3c5, VGA.vga.seq.map_mask | 0x0f);
//                            }
//                            // VGA.vga.gfx.mode = (byte)((VGA.vga.gfx.mode & ~0x60) | (shiftControl << 5));
//                            IoHandler.IO_Write(0x3ce, 0x05);
//                            IoHandler.IO_Write(0x3cf, ((VGA.vga.gfx.mode & ~0x60) | (shiftControl << 5)) & 0xFF);
//
//                            // VGA.vga.crtc.maximum_scan_line &= ~0x9f;
//                            /* no double scan */
//                            IoHandler.IO_Write(0x3d4, 0x09);
//                            IoHandler.IO_Write(0x3d5, (VGA.vga.crtc.maximum_scan_line & ~0x9f) & 0xFF);
                            if (VGA.vga.draw.resizing) {
                                VGA.vga.draw.resizing = false;
                                Pic.PIC_RemoveEvents(VGA_draw.VGA_SetupDrawing);
                                VGA_draw.VGA_SetupDrawing.call(0);
                            }
                            //DecodeBlock.start = 1;
                            VGA.vga.crtc.read_only = true;
                        } else {
                            /* XXX: the bios should do that */
                            // :TODO:
                            // bankOffset = 0;
                        }
                        vbeRegs[vbeIndex] = data;
                        break;
                    case VBE_DISPI_INDEX_VIRT_WIDTH:
                    {
                        if (data < vbeRegs[VBE_DISPI_INDEX_XRES])
                            return;
                        int w = data;
                        int lineOffset;
                        if (vbeRegs[VBE_DISPI_INDEX_BPP] == 4) {
                            lineOffset = data >>> 1;
                        } else {
                            lineOffset = data * ((vbeRegs[VBE_DISPI_INDEX_BPP] + 7) >>> 3);
                        }
                        int h = VGA.vga.vmemsize / lineOffset;
                        /* XXX: support wierd bochs semantics ? */
                        if (h < vbeRegs[VBE_DISPI_INDEX_YRES])
                            return;
                        vbeRegs[VBE_DISPI_INDEX_VIRT_WIDTH] = w;
                        vbeRegs[VBE_DISPI_INDEX_VIRT_HEIGHT] = h;
                        // :TODO:
                        // vbeLineOffset = lineOffset;
                    }
                    break;
                    case VBE_DISPI_INDEX_X_OFFSET:
                    case VBE_DISPI_INDEX_Y_OFFSET:
                    {
                        vbeRegs[vbeIndex] = data;
                        // :TODO:
                        /*
                        vbeStartAddress = vbeLineOffset * vbeRegs[VBE_DISPI_INDEX_Y_OFFSET];
                        int x = vbeRegs[VBE_DISPI_INDEX_X_OFFSET];
                        if (vbeRegs[VBE_DISPI_INDEX_BPP] == 4) {
                            vbeStartAddress += x >>> 1;
                        } else {
                            vbeStartAddress += x * ((vbeRegs[VBE_DISPI_INDEX_BPP] + 7) >>> 3);
                        }
                        vbeStartAddress >>>= 2;
                        */
                    }
                    break;
                    default:
                        vbeRegs[vbeIndex] = data;
                }
            }
        }
    };
    static private class Bochs_LFB_Handler extends Paging.PageHandler {
        public Bochs_LFB_Handler() {
            flags=Paging.PFLAG_READABLE|Paging.PFLAG_WRITEABLE|Paging.PFLAG_NOCODE;
        }
        public /*HostPt*/int GetHostReadPt( /*Bitu*/int phys_page ) {
            phys_page -= 0xE0000;
            return VGA.vga.mem.linear+((VGA.vga.svga.bank_read_full+phys_page*4096)&(VGA.vga.vmemwrap-1));
            //return VGA.vga.mem.linear_orgptr + (phys_page << 12);
        }
        public /*HostPt*/int GetHostWritePt( /*Bitu*/int phys_page ) {
            return VGA.vga.mem.linear+((VGA.vga.svga.bank_write_full+phys_page*4096)&(VGA.vga.vmemwrap-1));
            //return GetHostReadPt( phys_page );
        }
    }
    static public Paging.PageHandler handler = new Bochs_LFB_Handler();

    static public void registerIoPorts() {
        new IoHandler.IO_WriteHandleObject().Install(0x1cf, vbe_write_data, IoHandler.IO_MA);
        new IoHandler.IO_WriteHandleObject().Install(0xff81, vbe_write_data, IoHandler.IO_MA);
        new IoHandler.IO_WriteHandleObject().Install(0x1ce, vbe_write_index, IoHandler.IO_MA);
        new IoHandler.IO_WriteHandleObject().Install(0xff80, vbe_write_index, IoHandler.IO_MA);
        new IoHandler.IO_ReadHandleObject().Install(0x1cf, vbe_read_data, IoHandler.IO_MA);
        new IoHandler.IO_ReadHandleObject().Install(0xff81, vbe_read_data, IoHandler.IO_MA);
        new IoHandler.IO_ReadHandleObject().Install(0x1ce, vbe_read_index, IoHandler.IO_MA);
        new IoHandler.IO_ReadHandleObject().Install(0xff80, vbe_read_index, IoHandler.IO_MA);
        initialized = true;
    }
}
