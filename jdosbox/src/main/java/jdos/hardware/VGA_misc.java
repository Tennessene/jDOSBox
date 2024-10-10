package jdos.hardware;

import jdos.Dosbox;
import jdos.types.MachineType;

public class VGA_misc {
    public static IoHandler.IO_ReadHandler vga_read_p3da = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            /*Bit8u*/int retval=0;
            double timeInFrame = Pic.PIC_FullIndex()-VGA.vga.draw.delay.framestart;

            VGA.vga.internal.attrindex=false;
            VGA.vga.tandy.pcjr_flipflop=false;

            // 3DAh (R):  Status Register
            // bit   0  Horizontal or Vertical blanking
            //       3  Vertical sync

            if (timeInFrame >= VGA.vga.draw.delay.vrstart &&
                timeInFrame <= VGA.vga.draw.delay.vrend)
                retval |= 8;
            if (timeInFrame >= VGA.vga.draw.delay.vdend) {
                retval |= 1;
            } else {
                double timeInLine=timeInFrame % VGA.vga.draw.delay.htotal;
                if (timeInLine >= VGA.vga.draw.delay.hblkstart &&
                    timeInLine <= VGA.vga.draw.delay.hblkend) {
                    retval |= 1;
                }
            }
            return retval;
        }
    };

    private static IoHandler.IO_WriteHandler write_p3c2 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            VGA.vga.misc_output=(short)val;
            if ((val & 0x1)!=0) {
                IoHandler.IO_RegisterWriteHandler(0x3d4,VGA_crtc.vga_write_p3d4,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3d4,VGA_crtc.vga_read_p3d4,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3da,vga_read_p3da,IoHandler.IO_MB);

                IoHandler.IO_RegisterWriteHandler(0x3d5,VGA_crtc.vga_write_p3d5,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3d5,VGA_crtc.vga_read_p3d5,IoHandler.IO_MB);

                IoHandler.IO_FreeWriteHandler(0x3b4,IoHandler.IO_MB);
                IoHandler.IO_FreeReadHandler(0x3b4,IoHandler.IO_MB);
                IoHandler.IO_FreeWriteHandler(0x3b5,IoHandler.IO_MB);
                IoHandler.IO_FreeReadHandler(0x3b5,IoHandler.IO_MB);
                IoHandler.IO_FreeReadHandler(0x3ba,IoHandler.IO_MB);
            } else {
                IoHandler.IO_RegisterWriteHandler(0x3b4,VGA_crtc.vga_write_p3d4,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3b4,VGA_crtc.vga_read_p3d4,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3ba,vga_read_p3da,IoHandler.IO_MB);
    
                IoHandler.IO_RegisterWriteHandler(0x3b5,VGA_crtc.vga_write_p3d5,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3b5,VGA_crtc.vga_read_p3d5,IoHandler.IO_MB);


                IoHandler.IO_FreeWriteHandler(0x3d4,IoHandler.IO_MB);
                IoHandler.IO_FreeReadHandler(0x3d4,IoHandler.IO_MB);
                IoHandler.IO_FreeWriteHandler(0x3d5,IoHandler.IO_MB);
                IoHandler.IO_FreeReadHandler(0x3d5,IoHandler.IO_MB);
                IoHandler.IO_FreeReadHandler(0x3da,IoHandler.IO_MB);
            }
            /*
                0	If set Color Emulation. Base Address=3Dxh else Mono Emulation. Base Address=3Bxh.
                2-3	Clock Select. 0: 25MHz, 1: 28MHz
                5	When in Odd/Even modes Select High 64k bank if set
                6	Horizontal Sync Polarity. Negative if set
                7	Vertical Sync Polarity. Negative if set
                    Bit 6-7 indicates the number of lines on the display:
                    1:  400, 2: 350, 3: 480
                    Note: Set to all zero on a hardware reset.
                    Note: This register can be read from port 3CCh.
            */
        }
    };

    private static IoHandler.IO_ReadHandler read_p3cc = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return VGA.vga.misc_output;
        }
    };

// VGA feature control register
    private static IoHandler.IO_ReadHandler read_p3ca = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return 0;
        }
    };

    private static IoHandler.IO_ReadHandler read_p3c8 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return 0x10;
        }
    };

    private static IoHandler.IO_ReadHandler read_p3c2 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            /*Bit8u*/int retval=0;

            if (Dosbox.machine== MachineType.MCH_EGA) retval = 0x0F;
            else if (Dosbox.IS_VGA_ARCH()) retval = 0x60;
            if ((Dosbox.machine==MachineType.MCH_VGA) || (((VGA.vga.misc_output>>2)&3)==0) || (((VGA.vga.misc_output>>2)&3)==3)) {
                retval |= 0x10;
            }

            if (VGA.vga.draw.vret_triggered) retval |= 0x80;
            return retval;
            /*
                0-3 0xF on EGA, 0x0 on VGA
                4	Status of the switch selected by the Miscellaneous Output
                    Register 3C2h bit 2-3. Switch high if set.
                    (apparently always 1 on VGA)
                5	(EGA) Pin 19 of the Feature Connector (FEAT0) is high if set
                6	(EGA) Pin 17 of the Feature Connector (FEAT1) is high if set
                    (default differs by card, ET4000 sets them both)
                7	If set IRQ 2 has happened due to Vertical Retrace.
                    Should be cleared by IRQ 2 interrupt routine by clearing port 3d4h
                    index 11h bit 4.
            */
        }
    };

    static public void VGA_SetupMisc() {
        if (Dosbox.IS_EGAVGA_ARCH()) {
            VGA.vga.draw.vret_triggered=false;
            IoHandler.IO_RegisterReadHandler(0x3c2,read_p3c2,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3c2,write_p3c2,IoHandler.IO_MB);
            if (Dosbox.IS_VGA_ARCH()) {
                IoHandler.IO_RegisterReadHandler(0x3ca,read_p3ca,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3cc,read_p3cc,IoHandler.IO_MB);
            } else {
                IoHandler.IO_RegisterReadHandler(0x3c8,read_p3c8,IoHandler.IO_MB);
            }
        } else if (Dosbox.machine==MachineType.MCH_CGA || Dosbox.IS_TANDY_ARCH()) {
            IoHandler.IO_RegisterReadHandler(0x3da,vga_read_p3da,IoHandler.IO_MB);
        }
    }
    
}
