package jdos.hardware;

import jdos.ints.Int10_modes;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class VGA_s3 {
    private static VGA.tWritePort SVGA_S3_WriteCRTC = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
            switch (reg) {
            case 0x31:	/* CR31 Memory Configuration */
    //TODO Base address
                VGA.vga.s3.reg_31 = (short)val;
                VGA.vga.config.compatible_chain4 = (val&0x08)==0;
                if (VGA.vga.config.compatible_chain4) VGA.vga.vmemwrap = 256*1024;
                 else VGA.vga.vmemwrap = VGA.vga.vmemsize;
                VGA.vga.config.display_start = (VGA.vga.config.display_start&~0x30000)|((val&0x30)<<12);
                VGA.VGA_DetermineMode();
                VGA_memory.VGA_SetupHandlers();
                break;
                /*
                    0	Enable Base Address Offset (CPUA BASE). Enables bank operation if
                        set, disables if clear.
                    1	Two Page Screen Image. If set enables 2048 pixel wide screen setup
                    2	VGA 16bit Memory Bus Width. Set for 16bit, clear for 8bit
                    3	Use Enhanced Mode Memory Mapping (ENH MAP). Set to enable access to
                        video memory above 256k.
                    4-5	Bit 16-17 of the Display Start Address. For the 801/5,928 see index
                        51h, for the 864/964 see index 69h.
                    6	High Speed Text Display Font Fetch Mode. If set enables Page Mode
                        for Alpha Mode Font Access.
                    7	(not 864/964) Extended BIOS ROM Space Mapped out. If clear the area
                        C6800h-C7FFFh is mapped out, if set it is accessible.
                */
            case 0x35:	/* CR35 CRT Register Lock */
                if (VGA.vga.s3.reg_lock1 != 0x48) return;	//Needed for uvconfig detection
                VGA.vga.s3.reg_35=(short)(val & 0xf0);
                if (((VGA.vga.svga.bank_read & 0xf) ^ (val & 0xf))!=0) {
                    VGA.vga.svga.bank_read&=0xf0;
                    VGA.vga.svga.bank_read|=val & 0xf;
                    VGA.vga.svga.bank_write = VGA.vga.svga.bank_read;
                    VGA_memory.VGA_SetupHandlers();
                }
                break;
                /*
                    0-3	CPU Base Address. 64k bank number. For the 801/5 and 928 see 3d4h
                        index 51h bits 2-3. For the 864/964 see index 6Ah.
                    4	Lock Vertical Timing Registers (LOCK VTMG). Locks 3d4h index 6, 7
                        (bits 0,2,3,5,7), 9 bit 5, 10h, 11h bits 0-3, 15h, 16h if set
                    5	Lock Horizontal Timing Registers (LOCK HTMG). Locks 3d4h index
                        0,1,2,3,4,5,17h bit 2 if set
                    6	(911/924) Lock VSync Polarity.
                    7	(911/924) Lock HSync Polarity.
                */
            case 0x38:	/* CR38 Register Lock 1 */
                VGA.vga.s3.reg_lock1=(short)val;
                break;
            case 0x39:	/* CR39 Register Lock 2 */
                VGA.vga.s3.reg_lock2=(short)val;
                break;
            case 0x3a:
                VGA.vga.s3.reg_3a = (short)val;
                break;
            case 0x40:  /* CR40 System Config */
                VGA.vga.s3.reg_40 = (short)val;
                break;
            case 0x41:  /* CR41 BIOS flags */
                VGA.vga.s3.reg_41 = (short)val;
                break;
            case 0x43:	/* CR43 Extended Mode */
                VGA.vga.s3.reg_43=(short)(val & ~0x4);
                if ((((val & 0x4) ^ (VGA.vga.config.scan_len >> 6)) & 0x4)!=0) {
                    VGA.vga.config.scan_len&=0x2ff;
                    VGA.vga.config.scan_len|=(val & 0x4) << 6;
                    VGA_draw.VGA_CheckScanLength();
                }
                break;
                /*
                    2  Logical Screen Width bit 8. Bit 8 of the Display Offset Register/
                    (3d4h index 13h). (801/5,928) Only active if 3d4h index 51h bits 4-5
                    are 0
                */
            case 0x45:  /* Hardware cursor mode */
                VGA.vga.s3.hgc.curmode = (short)val;
                // Activate hardware cursor code if needed
                VGA_draw.VGA_ActivateHardwareCursor();
                break;
            case 0x46:
                VGA.vga.s3.hgc.originx = (VGA.vga.s3.hgc.originx & 0x00ff) | (val << 8);
                break;
            case 0x47:  /*  HGC orgX */
                VGA.vga.s3.hgc.originx = (VGA.vga.s3.hgc.originx & 0xff00) | val;
                break;
            case 0x48:
                VGA.vga.s3.hgc.originy = (VGA.vga.s3.hgc.originy & 0x00ff) | (val << 8);
                break;
            case 0x49:  /*  HGC orgY */
                VGA.vga.s3.hgc.originy = (VGA.vga.s3.hgc.originy & 0xff00) | val;
                break;
            case 0x4A:  /* HGC foreground stack */
                if (VGA.vga.s3.hgc.fstackpos > 2) VGA.vga.s3.hgc.fstackpos = 0;
                VGA.vga.s3.hgc.forestack.set(VGA.vga.s3.hgc.fstackpos, val);
                VGA.vga.s3.hgc.fstackpos++;
                break;
            case 0x4B:  /* HGC background stack */
                if (VGA.vga.s3.hgc.bstackpos > 2) VGA.vga.s3.hgc.bstackpos = 0;
                VGA.vga.s3.hgc.backstack.set(VGA.vga.s3.hgc.bstackpos, val);
                VGA.vga.s3.hgc.bstackpos++;
                break;
            case 0x4c:  /* HGC start address high byte*/
                VGA.vga.s3.hgc.startaddr &=0xff;
                VGA.vga.s3.hgc.startaddr |= ((val & 0xf) << 8);
                if ((((/*Bitu*/int)VGA.vga.s3.hgc.startaddr)<<10)+((64*64*2)/8) > VGA.vga.vmemsize) {
                    VGA.vga.s3.hgc.startaddr &= 0xff;	// put it back to some sane area;
                                                    // if read back of this address is ever implemented this needs to change
                    Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL,"VGA:S3:CRTC: HGC pattern address beyond video memory" );
                }
                break;
            case 0x4d:  /* HGC start address low byte*/
                VGA.vga.s3.hgc.startaddr &=0xff00;
                VGA.vga.s3.hgc.startaddr |= (val & 0xff);
                break;
            case 0x4e:  /* HGC pattern start X */
                VGA.vga.s3.hgc.posx = (short)(val & 0x3f);	// bits 0-5
                break;
            case 0x4f:  /* HGC pattern start Y */
                VGA.vga.s3.hgc.posy = (short)(val & 0x3f);	// bits 0-5
                break;
            case 0x50:  // Extended System Control 1
                VGA.vga.s3.reg_50 = (short)val;
                switch (val & VGA.S3_XGA_CMASK) {
                    case VGA.S3_XGA_32BPP: VGA.vga.s3.xga_color_mode = VGA.M_LIN32; break;
                    case VGA.S3_XGA_16BPP: VGA.vga.s3.xga_color_mode = VGA.M_LIN16; break;
                    case VGA.S3_XGA_8BPP: VGA.vga.s3.xga_color_mode = VGA.M_LIN8; break;
                }
                switch (val & VGA.S3_XGA_WMASK) {
                    case VGA.S3_XGA_1024: VGA.vga.s3.xga_screen_width = 1024; break;
                    case VGA.S3_XGA_1152: VGA.vga.s3.xga_screen_width = 1152; break;
                    case VGA.S3_XGA_640:  VGA.vga.s3.xga_screen_width = 640; break;
                    case VGA.S3_XGA_800:  VGA.vga.s3.xga_screen_width = 800; break;
                    case VGA.S3_XGA_1280: VGA.vga.s3.xga_screen_width = 1280; break;
                    default:  VGA.vga.s3.xga_screen_width = 1024; break;
                }
                break;
            case 0x51:	/* Extended System Control 2 */
                VGA.vga.s3.reg_51=(short)(val & 0xc0);		//Only store bits 6,7
                VGA.vga.config.display_start&=0xF3FFFF;
                VGA.vga.config.display_start|=(val & 3) << 18;
                if (((VGA.vga.svga.bank_read&0x30) ^ ((val&0xc)<<2))!=0) {
                    VGA.vga.svga.bank_read&=0xcf;
                    VGA.vga.svga.bank_read|=(val&0xc)<<2;
                    VGA.vga.svga.bank_write = VGA.vga.svga.bank_read;
                    VGA_memory.VGA_SetupHandlers();
                }
                if ((((val & 0x30) ^ (VGA.vga.config.scan_len >> 4)) & 0x30)!=0) {
                    VGA.vga.config.scan_len&=0xff;
                    VGA.vga.config.scan_len|=(val & 0x30) << 4;
                    VGA_draw.VGA_CheckScanLength();
                }
                break;
                /*
                    0	(80x) Display Start Address bit 18
                    0-1	(928 +) Display Start Address bit 18-19
                        Bits 16-17 are in index 31h bits 4-5, Bits 0-15 are in 3d4h index
                        0Ch,0Dh. For the 864/964 see 3d4h index 69h
                    2	(80x) CPU BASE. CPU Base Address Bit 18.
                    2-3	(928 +) Old CPU Base Address Bits 19-18.
                        64K Bank register bits 4-5. Bits 0-3 are in 3d4h index 35h.
                        For the 864/964 see 3d4h index 6Ah
                    4-5	Logical Screen Width Bit [8-9]. Bits 8-9 of the CRTC Offset register
                        (3d4h index 13h). If this field is 0, 3d4h index 43h bit 2 is active
                    6	(928,964) DIS SPXF. Disable Split Transfers if set. Spilt Transfers
                        allows transferring one half of the VRAM shift register data while
                        the other half is being output. For the 964 Split Transfers
                        must be enabled in enhanced modes (4AE8h bit 0 set). Guess: They
                        probably can't time the VRAM load cycle closely enough while the
                        graphics engine is running.
                    7	(not 864/964) Enable EPROM Write. If set enables flash memory write
                        control to the BIOS ROM address
                */
            case 0x52:  // Extended System Control 1
                VGA.vga.s3.reg_52 = (short)val;
                break;
            case 0x53:
                // Map or unmap MMIO
                // bit 4 = MMIO at A0000
                // bit 3 = MMIO at LFB + 16M (should be fine if its always enabled for now)
                if(VGA.vga.s3.ext_mem_ctrl!=val) {
                    VGA.vga.s3.ext_mem_ctrl = (short)val;
                    VGA_memory.VGA_SetupHandlers();
                }
                break;
            case 0x55:	/* Extended Video DAC Control */
                VGA.vga.s3.reg_55=(short)val;
                break;
                /*
                    0-1	DAC Register Select Bits. Passed to the RS2 and RS3 pins on the
                        RAMDAC, allowing access to all 8 or 16 registers on advanced RAMDACs.
                        If this field is 0, 3d4h index 43h bit 1 is active.
                    2	Enable General Input Port Read. If set DAC reads are disabled and the
                        STRD strobe for reading the General Input Port is enabled for reading
                        while DACRD is active, if clear DAC reads are enabled.
                    3	(928) Enable External SID Operation if set. If set video data is
                        passed directly from the VRAMs to the DAC rather than through the
                        VGA chip
                    4	Hardware Cursor MS/X11 Mode. If set the Hardware Cursor is in X11
                        mode, if clear in MS-Windows mode
                    5	(80x,928) Hardware Cursor External Operation Mode. If set the two
                        bits of cursor data ,is output on the HC[0-1] pins for the video DAC
                        The SENS pin becomes HC1 and the MID2 pin becomes HC0.
                    6	??
                    7	(80x,928) Disable PA Output. If set PA[0-7] and VCLK are tristated.
                        (864/964) TOFF VCLK. Tri-State Off VCLK Output. VCLK output tri
                        -stated if set
                */
            case 0x58:	/* Linear Address Window Control */
                VGA.vga.s3.reg_58=(short)val;
                break;
                /*
                    0-1	Linear Address Window Size. Must be less than or equal to video
                        memory size. 0: 64K, 1: 1MB, 2: 2MB, 3: 4MB (928)/8Mb (864/964)
                    2	(not 864/964) Enable Read Ahead Cache if set
                    3	(80x,928) ISA Latch Address. If set latches address during every ISA
                        cycle, unlatches during every ISA cycle if clear.
                        (864/964) LAT DEL. Address Latch Delay Control (VL-Bus only). If set
                        address latching occours in the T1 cycle, if clear in the T2 cycle
                        (I.e. one clock cycle delayed).
                    4	ENB LA. Enable Linear Addressing if set.
                    5	(not 864/964) Limit Entry Depth for Write-Post. If set limits Write
                        -Post Entry Depth to avoid ISA bus timeout due to wait cycle limit.
                    6	(928,964) Serial Access Mode (SAM) 256 Words Control. If set SAM
                        control is 256 words, if clear 512 words.
                    7	(928) RAS 6-MCLK. If set the random read/write cycle time is 6MCLKs,
                        if clear 7MCLKs
                */
            case 0x59:	/* Linear Address Window Position High */
                if (((VGA.vga.s3.la_window&0xff00) ^ (val << 8))!=0) {
                    VGA.vga.s3.la_window=(VGA.vga.s3.la_window&0x00ff) | (val << 8);
                    VGA_memory.VGA_StartUpdateLFB();
                }
                break;
            case 0x5a:	/* Linear Address Window Position Low */
                if (((VGA.vga.s3.la_window&0x00ff) ^ val)!=0) {
                    VGA.vga.s3.la_window=(VGA.vga.s3.la_window&0xff00) | val;
                    VGA_memory.VGA_StartUpdateLFB();
                }
                break;
            case 0x5D:	/* Extended Horizontal Overflow */
                if (((val ^ VGA.vga.s3.ex_hor_overflow) & 3)!=0) {
                    VGA.vga.s3.ex_hor_overflow=(short)val;
                    VGA.VGA_StartResize();
                } else VGA.vga.s3.ex_hor_overflow=(short)val;
                break;
                /*
                    0	Horizontal Total bit 8. Bit 8 of the Horizontal Total register (3d4h
                        index 0)
                    1	Horizontal Display End bit 8. Bit 8 of the Horizontal Display End
                        register (3d4h index 1)
                    2	Start Horizontal Blank bit 8. Bit 8 of the Horizontal Start Blanking
                        register (3d4h index 2).
                    3	(864,964) EHB+64. End Horizontal Blank +64. If set the /BLANK pulse
                        is extended by 64 DCLKs. Note: Is this bit 6 of 3d4h index 3 or
                        does it really extend by 64 ?
                    4	Start Horizontal Sync Position bit 8. Bit 8 of the Horizontal Start
                        Retrace register (3d4h index 4).
                    5	(864,964) EHS+32. End Horizontal Sync +32. If set the HSYNC pulse
                        is extended by 32 DCLKs. Note: Is this bit 5 of 3d4h index 5 or
                        does it really extend by 32 ?
                    6	(928,964) Data Transfer Position bit 8. Bit 8 of the Data Transfer
                        Position register (3d4h index 3Bh)
                    7	(928,964) Bus-Grant Terminate Position bit 8. Bit 8 of the Bus Grant
                        Termination register (3d4h index 5Fh).
                */
            case 0x5e:	/* Extended Vertical Overflow */
                VGA.vga.config.line_compare=(VGA.vga.config.line_compare & 0x3ff) | (val & 0x40) << 4;
                if (((val ^ VGA.vga.s3.ex_ver_overflow) & 0x3)!=0) {
                    VGA.vga.s3.ex_ver_overflow=(short)val;
                    VGA.VGA_StartResize();
                } else VGA.vga.s3.ex_ver_overflow=(short)val;
                break;
                /*
                    0	Vertical Total bit 10. Bit 10 of the Vertical Total register (3d4h
                        index 6). Bits 8 and 9 are in 3d4h index 7 bit 0 and 5.
                    1	Vertical Display End bit 10. Bit 10 of the Vertical Display End
                        register (3d4h index 12h). Bits 8 and 9 are in 3d4h index 7 bit 1
                        and 6
                    2	Start Vertical Blank bit 10. Bit 10 of the Vertical Start Blanking
                        register (3d4h index 15h). Bit 8 is in 3d4h index 7 bit 3 and bit 9
                        in 3d4h index 9 bit 5
                    4	Vertical Retrace Start bit 10. Bit 10 of the Vertical Start Retrace
                        register (3d4h index 10h). Bits 8 and 9 are in 3d4h index 7 bit 2
                        and 7.
                    6	Line Compare Position bit 10. Bit 10 of the Line Compare register
                        (3d4h index 18h). Bit 8 is in 3d4h index 7 bit 4 and bit 9 in 3d4h
                        index 9 bit 6.
                */
            case 0x67:	/* Extended Miscellaneous Control 2 */
                /*
                    0	VCLK PHS. VCLK Phase With Respect to DCLK. If clear VLKC is inverted
                        DCLK, if set VCLK = DCLK.
                    2-3 (Trio64V+) streams mode
                            00 disable Streams Processor
                            01 overlay secondary stream on VGA-mode background
                            10 reserved
                            11 full Streams Processor operation
                    4-7	Pixel format.
                            0  Mode  0: 8bit (1 pixel/VCLK)
                            1  Mode  8: 8bit (2 pixels/VCLK)
                            3  Mode  9: 15bit (1 pixel/VCLK)
                            5  Mode 10: 16bit (1 pixel/VCLK)
                            7  Mode 11: 24/32bit (2 VCLKs/pixel)
                            13  (732/764) 32bit (1 pixel/VCLK)
                */
                VGA.vga.s3.misc_control_2=(short)val;
                VGA.VGA_DetermineMode();
                break;
            case 0x69:	/* Extended System Control 3 */
                if ((((VGA.vga.config.display_start & 0x1f0000)>>16) ^ (val & 0x1f))!=0) {
                    VGA.vga.config.display_start&=0xffff;
                    VGA.vga.config.display_start|=(val & 0x1f) << 16;
                }
                break;
            case 0x6a:	/* Extended System Control 4 */
                VGA.vga.svga.bank_read=(short)(val & 0x7f);
                VGA.vga.svga.bank_write = VGA.vga.svga.bank_read;
                VGA_memory.VGA_SetupHandlers();
                break;
            case 0x6b:	// BIOS scratchpad: LFB address
                VGA.vga.s3.reg_6b=(short)val;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:S3:CRTC:Write to illegal index "+Integer.toString(reg,16));
                break;
            }
        }
    };

    private static VGA.tReadPort SVGA_S3_ReadCRTC = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            switch (reg) {
            case 0x24:	/* attribute controller index (read only) */
            case 0x26:
                return ((VGA.vga.attr.disabled & 1)!=0?0x00:0x20) | (VGA.vga.attr.index & 0x1f);
            case 0x2d:	/* Extended Chip ID (high byte of PCI device ID) */
                return 0x88;
            case 0x2e:	/* New Chip ID  (low byte of PCI device ID) */
                return 0x11;	// Trio64
            case 0x2f:	/* Revision */
                return 0x00;	// Trio64 (exact value?)
    //		return 0x44;	// Trio64 V+
            case 0x30:	/* CR30 Chip ID/REV register */
                return 0xe1;	// Trio+ dual byte
            case 0x31:	/* CR31 Memory Configuration */
    //TODO mix in bits from baseaddress;
                return 	VGA.vga.s3.reg_31;
            case 0x35:	/* CR35 CRT Register Lock */
                return VGA.vga.s3.reg_35|(VGA.vga.svga.bank_read & 0xf);
            case 0x36: /* CR36 Reset State Read 1 */
                return VGA.vga.s3.reg_36;
            case 0x37: /* Reset state read 2 */
                return 0x2b;
            case 0x38: /* CR38 Register Lock 1 */
                return VGA.vga.s3.reg_lock1;
            case 0x39: /* CR39 Register Lock 2 */
                return VGA.vga.s3.reg_lock2;
            case 0x3a:
                return VGA.vga.s3.reg_3a;
            case 0x40: /* CR40 system config */
                return VGA.vga.s3.reg_40;
            case 0x41: /* CR40 system config */
                return VGA.vga.s3.reg_41;
            case 0x42: // not interlaced
                return 0x0d;
            case 0x43:	/* CR43 Extended Mode */
                return VGA.vga.s3.reg_43|((VGA.vga.config.scan_len>>6)&0x4);
            case 0x45:  /* Hardware cursor mode */
                VGA.vga.s3.hgc.bstackpos = 0;
                VGA.vga.s3.hgc.fstackpos = 0;
                return VGA.vga.s3.hgc.curmode|0xa0;
            case 0x46:
                return VGA.vga.s3.hgc.originx>>8;
            case 0x47:  /*  HGC orgX */
                return VGA.vga.s3.hgc.originx&0xff;
            case 0x48:
                return VGA.vga.s3.hgc.originy>>8;
            case 0x49:  /*  HGC orgY */
                return VGA.vga.s3.hgc.originy&0xff;
            case 0x4A:  /* HGC foreground stack */
                return VGA.vga.s3.hgc.forestack.get(VGA.vga.s3.hgc.fstackpos);
            case 0x4B:  /* HGC background stack */
                return VGA.vga.s3.hgc.backstack.get(VGA.vga.s3.hgc.bstackpos);
            case 0x50:	// CR50 Extended System Control 1
                return VGA.vga.s3.reg_50;
            case 0x51:	/* Extended System Control 2 */
                return ((VGA.vga.config.display_start >> 16) & 3 ) |
                        ((VGA.vga.svga.bank_read & 0x30) >> 2) |
                        ((VGA.vga.config.scan_len & 0x300) >> 4) |
                        VGA.vga.s3.reg_51;
            case 0x52:	// CR52 Extended BIOS flags 1
                return VGA.vga.s3.reg_52;
            case 0x53:
                return VGA.vga.s3.ext_mem_ctrl;
            case 0x55:	/* Extended Video DAC Control */
                return VGA.vga.s3.reg_55;
            case 0x58:	/* Linear Address Window Control */
                return	VGA.vga.s3.reg_58;
            case 0x59:	/* Linear Address Window Position High */
                return (VGA.vga.s3.la_window >> 8);
            case 0x5a:	/* Linear Address Window Position Low */
                return (VGA.vga.s3.la_window & 0xff);
            case 0x5D:	/* Extended Horizontal Overflow */
                return VGA.vga.s3.ex_hor_overflow;
            case 0x5e:	/* Extended Vertical Overflow */
                return VGA.vga.s3.ex_ver_overflow;
            case 0x67:	/* Extended Miscellaneous Control 2 */
                return VGA.vga.s3.misc_control_2;
            case 0x69:	/* Extended System Control 3 */
                return ((VGA.vga.config.display_start & 0x1f0000)>>16);
            case 0x6a:	/* Extended System Control 4 */
                return (VGA.vga.svga.bank_read & 0x7f);
            case 0x6b:	// BIOS scatchpad: LFB address
                return VGA.vga.s3.reg_6b;
            default:
                return 0x00;
            }
        }
    };

    private static VGA.tWritePort SVGA_S3_WriteSEQ = new VGA.tWritePort() {
        public void call(/*Bitu*/int reg,/*Bitu*/int val,/*Bitu*/int iolen) {
            if (reg>0x8 && VGA.vga.s3.pll.lock!=0x6) return;
            switch (reg) {
            case 0x08:
                VGA.vga.s3.pll.lock=(short)val;
                break;
            case 0x10:		/* Memory PLL Data Low */
                VGA.vga.s3.mclk.n=(short)(val & 0x1f);
                VGA.vga.s3.mclk.r=(short)(val >> 5);
                break;
            case 0x11:		/* Memory PLL Data High */
                VGA.vga.s3.mclk.m=(short)(val & 0x7f);
                break;
            case 0x12:		/* Video PLL Data Low */
                VGA.vga.s3.clk[3].n=(short)(val & 0x1f);
                VGA.vga.s3.clk[3].r=(short)(val >> 5);
                break;
            case 0x13:		/* Video PLL Data High */
                VGA.vga.s3.clk[3].m=(short)(val & 0x7f);
                break;
            case 0x15:
                VGA.vga.s3.pll.cmd=(short)val;
                VGA.VGA_StartResize();
                break;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:S3:SEQ:Write to illegal index "+Integer.toString(reg,16));
                break;
            }
        }
    };

    private static VGA.tReadPort SVGA_S3_ReadSEQ = new VGA.tReadPort() {
        public /*Bitu*/int call(/*Bitu*/int reg,/*Bitu*/int iolen) {
            /* S3 specific group */
            if (reg>0x8 && VGA.vga.s3.pll.lock!=0x6) {
                if (reg<0x1b) return 0;
                else return reg;
            }
            switch (reg) {
            case 0x08:		/* PLL Unlock */
                return VGA.vga.s3.pll.lock;
            case 0x10:		/* Memory PLL Data Low */
                return (VGA.vga.s3.mclk.n!=0 || (VGA.vga.s3.mclk.r << 5)!=0)?1:0;
            case 0x11:		/* Memory PLL Data High */
                return VGA.vga.s3.mclk.m;
            case 0x12:		/* Video PLL Data Low */
                return (VGA.vga.s3.clk[3].n!=0 || (VGA.vga.s3.clk[3].r << 5)!=0)?1:0;
            case 0x13:		/* Video Data High */
                return VGA.vga.s3.clk[3].m;
            case 0x15:
                return VGA.vga.s3.pll.cmd;
            default:
                if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC,LogSeverities.LOG_NORMAL,"VGA:S3:SEQ:Read from illegal index "+Integer.toString(reg,16));
                return 0;
            }
        }
    };

    private static VGA.tGetClock SVGA_S3_GetClock = new VGA.tGetClock() {
        public /*Bitu*/int call() {
            /*Bitu*/int clock = (VGA.vga.misc_output >> 2) & 3;
            if (clock == 0)
                clock = 25175000;
            else if (clock == 1)
                clock = 28322000;
            else
                clock=1000*VGA.S3_CLOCK(VGA.vga.s3.clk[clock].m,VGA.vga.s3.clk[clock].n,VGA.vga.s3.clk[clock].r);
            /* Check for dual transfer, master clock/2 */
            if ((VGA.vga.s3.pll.cmd & 0x10)!=0) clock/=2;
            return clock;
        }
    };

    private static VGA.tHWCursorActive SVGA_S3_HWCursorActive = new VGA.tHWCursorActive() {
        public boolean call() {
            return (VGA.vga.s3.hgc.curmode & 0x1) != 0;
        }
    };

    private static VGA.tAcceptsMode SVGA_S3_AcceptsMode = new VGA.tAcceptsMode() {
        public boolean call(/*Bitu*/int mode) {
            return Int10_modes.VideoModeMemSize(mode) < VGA.vga.vmemsize;
        }
    };

    public static void SVGA_Setup_S3Trio() {
        VGA.svga.write_p3d5 = SVGA_S3_WriteCRTC;
        VGA.svga.read_p3d5 = SVGA_S3_ReadCRTC;
        VGA.svga.write_p3c5 = SVGA_S3_WriteSEQ;
        VGA.svga.read_p3c5 = SVGA_S3_ReadSEQ;
        VGA.svga.write_p3c0 = null; /* no S3-specific functionality */
        VGA.svga.read_p3c1 = null; /* no S3-specific functionality */

        VGA.svga.set_video_mode = null; /* implemented in core */
        VGA.svga.determine_mode = null; /* implemented in core */
        VGA.svga.set_clock = null; /* implemented in core */
        VGA.svga.get_clock = SVGA_S3_GetClock;
        VGA.svga.hardware_cursor_active = SVGA_S3_HWCursorActive;
        VGA.svga.accepts_mode = SVGA_S3_AcceptsMode;

        if (VGA.vga.vmemsize == 0)
            VGA.vga.vmemsize = 2*1024*1024; // the most common S3 configuration

        // Set CRTC 36 to specify amount of VRAM and PCI
        if (VGA.vga.vmemsize < 1024*1024) {
            VGA.vga.vmemsize = 512*1024;
            VGA.vga.s3.reg_36 = 0xfa;		// less than 1mb fast page mode
        } else if (VGA.vga.vmemsize < 2048*1024)	{
            VGA.vga.vmemsize = 1024*1024;
            VGA.vga.s3.reg_36 = 0xda;		// 1mb fast page mode
        } else if (VGA.vga.vmemsize < 3072*1024)	{
            VGA.vga.vmemsize = 2048*1024;
            VGA.vga.s3.reg_36 = 0x9a;		// 2mb fast page mode
        } else if (VGA.vga.vmemsize < 4096*1024)	{
            VGA.vga.vmemsize = 3072*1024;
            VGA.vga.s3.reg_36 = 0x5a;		// 3mb fast page mode
        } else if (VGA.vga.vmemsize < 8192*1024) {	// Trio64 supported only up to 4M
            VGA.vga.vmemsize = 4096*1024;
            VGA.vga.s3.reg_36 = 0x1a;		// 4mb fast page mode
        } else {	// 8M
            VGA.vga.vmemsize = 8192*1024;
            VGA.vga.s3.reg_36 = 0x7a;		// 8mb fast page mode
        }

        // S3 ROM signature
        /*PhysPt*/int rom_base=Memory.PhysMake(0xc000,0);
        Memory.phys_writeb(rom_base+0x003f,'S');
        Memory.phys_writeb(rom_base+0x0040,'3');
        Memory.phys_writeb(rom_base+0x0041,' ');
        Memory.phys_writeb(rom_base+0x0042,'8');
        Memory.phys_writeb(rom_base+0x0043,'6');
        Memory.phys_writeb(rom_base+0x0044,'C');
        Memory.phys_writeb(rom_base+0x0045,'7');
        Memory.phys_writeb(rom_base+0x0046,'6');
        Memory.phys_writeb(rom_base+0x0047,'4');
    }
    
}
