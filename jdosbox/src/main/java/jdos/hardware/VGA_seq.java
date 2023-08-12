package jdos.hardware;

import jdos.Dosbox;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.Ptr;

public class VGA_seq {
    static private IoHandler.IO_ReadHandler read_p3c4 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return VGA.vga.seq.index;
        }
    };

    private static IoHandler.IO_WriteHandler write_p3c4 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            VGA.vga.seq.index=(short)val;
        }
    };

    private static IoHandler.IO_WriteHandler write_p3c5 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
        //	LOG_MSG("SEQ WRITE reg %X val %X",VGA.vga.seq.index),val);
            switch(VGA.vga.seq.index) {
            case 0:		/* Reset */
                VGA.vga.seq.reset=(short)val;
                break;
            case 1:		/* Clocking Mode */
                if (val!=(VGA.vga.seq.clocking_mode & 0xFF)) {
                    // don't resize if only the screen off bit was changed
                    if ((val&(0xDF))!=(VGA.vga.seq.clocking_mode&(0xDF))) {
                        VGA.vga.seq.clocking_mode=(byte)val;
                        VGA.VGA_StartResize();
                    } else {
                        VGA.vga.seq.clocking_mode=(byte)val;
                    }
                    if ((val & 0x20)!=0) VGA.vga.attr.disabled |= 0x2;
                    else VGA.vga.attr.disabled &= ~0x2;
                }
                /* TODO Figure this out :)
                    0	If set character clocks are 8 dots wide, else 9.
                    2	If set loads video serializers every other character
                        clock cycle, else every one.
                    3	If set the Dot Clock is Master Clock/2, else same as Master Clock
                        (See 3C2h bit 2-3). (Doubles pixels). Note: on some SVGA chipsets
                        this bit also affects the Sequencer mode.
                    4	If set loads video serializers every fourth character clock cycle,
                        else every one.
                    5	if set turns off screen and gives all memory cycles to the CPU
                        interface.
                */
                break;
            case 2:		/* Map Mask */
                VGA.vga.seq.map_mask=(byte)(val & 15);
                VGA.vga.config.full_map_mask=VGA.FillTable[val & 15];
                VGA.vga.config.full_not_map_mask=~VGA.vga.config.full_map_mask;
                /*
                    0  Enable writes to plane 0 if set
                    1  Enable writes to plane 1 if set
                    2  Enable writes to plane 2 if set
                    3  Enable writes to plane 3 if set
                */
                break;
            case 3:		/* Character Map Select */
                {
                    VGA.vga.seq.character_map_select=(short)val;
                    /*Bit8u*/int font1=(val & 0x3) << 1;
                    if (Dosbox.IS_VGA_ARCH()) font1|=(val & 0x10) >> 4;
                    VGA.vga.draw.font_tables[0]=new Ptr(VGA.vga.draw.font,font1*8*1024);
                    /*Bit8u*/int font2=((val & 0xc) >> 1);
                    if (Dosbox.IS_VGA_ARCH()) font2|=(val & 0x20) >> 5;
                    VGA.vga.draw.font_tables[1]=new Ptr(VGA.vga.draw.font,font2*8*1024);
                }
                /*
                    0,1,4  Selects VGA Character Map (0..7) if bit 3 of the character
                            attribute is clear.
                    2,3,5  Selects VGA Character Map (0..7) if bit 3 of the character
                            attribute is set.
                    Note: Character Maps are placed as follows:
                    Map 0 at 0k, 1 at 16k, 2 at 32k, 3: 48k, 4: 8k, 5: 24k, 6: 40k, 7: 56k
                */
                break;
            case 4:	/* Memory Mode */
                /*
                    0  Set if in an alphanumeric mode, clear in graphics modes.
                    1  Set if more than 64kbytes on the adapter.
                    2  Enables Odd/Even addressing mode if set. Odd/Even mode places all odd
                        bytes in plane 1&3, and all even bytes in plane 0&2.
                    3  If set address bit 0-1 selects video memory planes (256 color mode),
                        rather than the Map Mask and Read Map Select Registers.
                */
                VGA.vga.seq.memory_mode=(short)val;
                if (Dosbox.IS_VGA_ARCH()) {
                    /* Changing this means changing the VGA Memory Read/Write Handler */
                    if ((val&0x08)!=0) VGA.vga.config.chained=true;
                    else VGA.vga.config.chained=false;
                    VGA_memory.VGA_SetupHandlers();
                }
                break;
            default:
                if (VGA.svga.write_p3c5!=null) {
                    VGA.svga.write_p3c5.call(VGA.vga.seq.index, val, iolen);
                } else {
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:SEQ:Write to illegal index "+Integer.toString(VGA.vga.seq.index,16));
                }
                break;
            }
        }
    };

    static private IoHandler.IO_ReadHandler read_p3c5 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
        //	LOG_MSG("VGA:SEQ:Read from index %2X",VGA.vga.seq.index));
            switch(VGA.vga.seq.index) {
            case 0:			/* Reset */
                return VGA.vga.seq.reset;
            case 1:			/* Clocking Mode */
                return VGA.vga.seq.clocking_mode & 0xFF;
            case 2:			/* Map Mask */
                return VGA.vga.seq.map_mask & 0xFF;
            case 3:			/* Character Map Select */
                return VGA.vga.seq.character_map_select;
            case 4:			/* Memory Mode */
                return VGA.vga.seq.memory_mode;
            default:
                if (VGA.svga.read_p3c5 != null)
                    return VGA.svga.read_p3c5.call(VGA.vga.seq.index, iolen);
                break;
            }
            return 0;
        }
    };
    
    public static void VGA_SetupSEQ() {
        if (Dosbox.IS_EGAVGA_ARCH()) {
            IoHandler.IO_RegisterWriteHandler(0x3c4,write_p3c4,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x3c5,write_p3c5,IoHandler.IO_MB);
            if (Dosbox.IS_VGA_ARCH()) {
                IoHandler.IO_RegisterReadHandler(0x3c4,read_p3c4,IoHandler.IO_MB);
                IoHandler.IO_RegisterReadHandler(0x3c5,read_p3c5,IoHandler.IO_MB);
            }
        }
    }

    
}
