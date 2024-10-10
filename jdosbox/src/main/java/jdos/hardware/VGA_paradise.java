package jdos.hardware;

import jdos.ints.Int10_modes;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class VGA_paradise {

    static private class SVGA_PVGA1A_DATA {
        /*Bitu*/int PR0A;
        /*Bitu*/int PR0B;
        /*Bitu*/int PR1;
        /*Bitu*/int PR2;
        /*Bitu*/int PR3;
        /*Bitu*/int PR4;
        /*Bitu*/int PR5;

        boolean locked() { return (PR5&7)!=5; }

        /*Bitu*/int[] clockFreq = new int[4];
        /*Bitu*/int biosMode;
    }

    static SVGA_PVGA1A_DATA pvga1a = new SVGA_PVGA1A_DATA();


    static private void bank_setup_pvga1a() {
// Note: There is some inconsistency in available documentation. Most sources tell that PVGA1A used
//       only 7 bits of bank index (VGADOC and Ferraro agree on that) but also point that there are
//       implementations with 1M of RAM which is simply not possible with 7-bit banks. This implementation
//       assumes that the eighth bit was actually wired and could be used. This does not conflict with
//       anything and actually works in WHATVGA just fine.
        if ((pvga1a.PR1 & 0x08)!=0) {
            // TODO: Dual bank function is not supported yet
            // TODO: Requirements are not compatible with vga_memory implementation.
        } else {
            // Single bank config is straightforward
            VGA.vga.svga.bank_read = VGA.vga.svga.bank_write = (short)pvga1a.PR0A;
            VGA.vga.svga.bank_size = 4*1024;
            VGA_memory.VGA_SetupHandlers();
        }
    }

    static private VGA.tWritePort write_p3cf_pvga1a = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
            if (pvga1a.locked() && reg >= 0x09 && reg <= 0x0e)
                return;

            switch (reg) {
            case 0x09:
                // Bank A, 4K granularity, not using bit 7
                // Maps to A800h-AFFFh if PR1 bit 3 set and 64k config B000h-BFFFh if 128k config. A000h-AFFFh otherwise.
                pvga1a.PR0A = val;
                bank_setup_pvga1a();
                break;
            case 0x0a:
                // Bank B, 4K granularity, not using bit 7
                // Maps to A000h-A7FFh if PR1 bit 3 set and 64k config, A000h-AFFFh if 128k
                pvga1a.PR0B = val;
                bank_setup_pvga1a();
                break;
            case 0x0b:
                // Memory size. We only allow to mess with bit 3 here (enable bank B) - this may break some detection schemes
                pvga1a.PR1 = (pvga1a.PR1 & ~0x08) | (val & 0x08);
                bank_setup_pvga1a();
                break;
            case 0x0c:
                // Video configuration
                // TODO: Figure out if there is anything worth implementing here.
                pvga1a.PR2 = val;
                break;
            case 0x0d:
                // CRT control. Bits 3-4 contain bits 16-17 of CRT start.
                // TODO: Implement bit 2 (CRT address doubling - this mechanism is present in other chipsets as well,
                // but not implemented in DosBox core)
                pvga1a.PR3 = val;
                VGA.vga.config.display_start = (VGA.vga.config.display_start & 0xffff) | ((val & 0x18)<<13);
                VGA.vga.config.cursor_start = (VGA.vga.config.cursor_start & 0xffff) | ((val & 0x18)<<13);
                break;
            case 0x0e:
                // Video control
                // TODO: Figure out if there is anything worth implementing here.
                pvga1a.PR4 = val;
                break;
            case 0x0f:
                // Enable extended registers
                pvga1a.PR5 = val;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:GFX:PVGA1A:Write to illegal index "+Integer.toString(reg,16));
                break;
            }
        }
    };

    static private VGA.tReadPort read_p3cf_pvga1a = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            if (pvga1a.locked() && reg >= 0x09 && reg <= 0x0e)
                return 0x0;

            switch (reg) {
            case 0x09:
                return pvga1a.PR0A;
            case 0x0a:
                return pvga1a.PR0B;
            case 0x0b:
                return pvga1a.PR1;
            case 0x0c:
                return pvga1a.PR2;
            case 0x0d:
                return pvga1a.PR3;
            case 0x0e:
                return pvga1a.PR4;
            case 0x0f:
                return pvga1a.PR5;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:GFX:PVGA1A:Read from illegal index "+Integer.toString(reg,16));
                break;
            }
            return 0x0;
        }
    };

    private static VGA.tFinishSetMode FinishSetMode_PVGA1A = new VGA.tFinishSetMode() {
        public void call(/*Bitu*/int crtc_base, VGA.VGA_ModeExtraData modeData) {
            pvga1a.biosMode = modeData.modeNo;

    // Reset to single bank and set it to 0. May need to unlock first (DPaint locks on exit)
            IoHandler.IO_Write(0x3ce, 0x0f);
            /*Bitu*/int oldlock = IoHandler.IO_Read(0x3cf);
            IoHandler.IO_Write(0x3cf, 0x05);
            IoHandler.IO_Write(0x3ce, 0x09);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0a);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0b);
            /*Bit8u*/short val = IoHandler.IO_Read(0x3cf);
            IoHandler.IO_Write(0x3cf, val & ~0x08);
            IoHandler.IO_Write(0x3ce, 0x0c);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0d);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0e);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0f);
            IoHandler.IO_Write(0x3cf, oldlock);

            if (VGA.svga.determine_mode!=null)
                VGA.svga.determine_mode.call();

            if(VGA.vga.mode != VGA.M_VGA) {
                VGA.vga.config.compatible_chain4 = false;
                VGA.vga.vmemwrap = VGA.vga.vmemsize;
            } else {
                VGA.vga.config.compatible_chain4 = true;
                VGA.vga.vmemwrap = 256*1024;
            }

            VGA_memory.VGA_SetupHandlers();
        }
    };

    private static VGA.tDetermineMode DetermineMode_PVGA1A = new VGA.tDetermineMode() {
        public void call() {
            // Close replica from the base implementation. It will stay here
            // until I figure a way to either distinguish M_VGA and M_LIN8 or
            // merge them.
            if ((VGA.vga.attr.mode_control & 1)!=0) {
                if ((VGA.vga.gfx.mode & 0x40)!=0) VGA.VGA_SetMode((pvga1a.biosMode<=0x13)?VGA.M_VGA:VGA.M_LIN8);
                else if ((VGA.vga.gfx.mode & 0x20)!=0) VGA.VGA_SetMode(VGA.M_CGA4);
                else if ((VGA.vga.gfx.miscellaneous & 0x0c)==0x0c) VGA.VGA_SetMode(VGA.M_CGA2);
                else VGA.VGA_SetMode((pvga1a.biosMode<=0x13)?VGA.M_EGA:VGA.M_LIN4);
            } else {
                VGA.VGA_SetMode(VGA.M_TEXT);
            }
        }
    };

    private static VGA.tSetClock SetClock_PVGA1A = new VGA.tSetClock() {
        public void call(/*Bitu*/int which,/*Bitu*/int target) {
            if (which < 4) {
                pvga1a.clockFreq[which]=1000*target;
                VGA.VGA_StartResize();
            }
        }
    };

    private static VGA.tGetClock GetClock_PVGA1A = new VGA.tGetClock() {
        public /*Bitu*/int call() {
            return pvga1a.clockFreq[(VGA.vga.misc_output >> 2) & 3];
        }
    };

    private static VGA.tAcceptsMode AcceptsMode_PVGA1A = new VGA.tAcceptsMode() {
        public boolean call(/*Bitu*/int modeNo) {
            return Int10_modes.VideoModeMemSize(modeNo) < VGA.vga.vmemsize;
        }
    };

    public static void SVGA_Setup_ParadisePVGA1A() {
        VGA.svga.write_p3cf = write_p3cf_pvga1a;
        VGA.svga.read_p3cf = read_p3cf_pvga1a;

        VGA.svga.set_video_mode = FinishSetMode_PVGA1A;
        VGA.svga.determine_mode = DetermineMode_PVGA1A;
        VGA.svga.set_clock = SetClock_PVGA1A;
        VGA.svga.get_clock = GetClock_PVGA1A;
        VGA.svga.accepts_mode = AcceptsMode_PVGA1A;

        VGA.VGA_SetClock(0,VGA.CLK_25);
        VGA.VGA_SetClock(1,VGA.CLK_28);
        VGA.VGA_SetClock(2,32400); // could not find documentation
        VGA.VGA_SetClock(3,35900);

        // Adjust memory, default to 512K
        if (VGA.vga.vmemsize == 0)
            VGA.vga.vmemsize = 512*1024;

        if (VGA.vga.vmemsize < 512*1024)	{
            VGA.vga.vmemsize = 256*1024;
            pvga1a.PR1 = 1<<6;
        } else if (VGA.vga.vmemsize > 512*1024) {
            VGA.vga.vmemsize = 1024*1024;
            pvga1a.PR1 = 3<<6;
        } else {
            pvga1a.PR1 = 2<<6;
        }

        // Paradise ROM signature
        /*PhysPt*/int rom_base=(int)Memory.PhysMake(0xc000,0);
        Memory.phys_writeb(rom_base+0x007d,'V');
        Memory.phys_writeb(rom_base+0x007e,'G');
        Memory.phys_writeb(rom_base+0x007f,'A');
        Memory.phys_writeb(rom_base+0x0080,'=');

        IoHandler.IO_Write(0x3cf, 0x05); // Enable!
    }

}
