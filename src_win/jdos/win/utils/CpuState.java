package jdos.win.utils;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;

public class CpuState {
    int eax;
    int ecx = 1;
    int edx;
    int ebx;
    int esp;
    int ebp;
    int esi;
    int edi;
    int eip;

    int es;
    int cs;
    int ds;
    int ss;
    int fs;
    int gs;

    int esPhys;
    int csPhys;
    int dsPhys;
    int ssPhys;
    int fsPhys;
    int gsPhys;

    int stackMask = 0xffffffff;
    int stackNotMask;

    public void save() {
        eax = CPU_Regs.reg_eax.dword;
        ecx = CPU_Regs.reg_ecx.dword;
        edx = CPU_Regs.reg_edx.dword;
        ebx = CPU_Regs.reg_ebx.dword;
        esp = CPU_Regs.reg_esp.dword;
        ebp = CPU_Regs.reg_ebp.dword;
        esi = CPU_Regs.reg_esi.dword;
        edi = CPU_Regs.reg_edi.dword;
        eip = CPU_Regs.reg_eip;
        es = CPU.Segs_ESval;
        cs = CPU.Segs_CSval;
        ds = CPU.Segs_DSval;
        ss = CPU.Segs_SSval;
        fs = CPU.Segs_FSval;
        gs = CPU.Segs_GSval;
        esPhys = CPU.Segs_ESphys;
        csPhys = CPU.Segs_CSphys;
        dsPhys = CPU.Segs_DSphys;
        ssPhys = CPU.Segs_SSphys;
        fsPhys = CPU.Segs_FSphys;
        gsPhys = CPU.Segs_GSphys;
        stackMask=CPU.cpu.stack.mask;
        stackNotMask=CPU.cpu.stack.notmask;
    }

    public void load() {
        CPU_Regs.reg_eax.dword = eax;
        CPU_Regs.reg_ecx.dword = ecx;
        CPU_Regs.reg_edx.dword = edx;
        CPU_Regs.reg_ebx.dword = ebx;
        CPU_Regs.reg_esp.dword = esp;
        CPU_Regs.reg_ebp.dword = ebp;
        CPU_Regs.reg_esi.dword = esi;
        CPU_Regs.reg_edi.dword = edi;
        CPU_Regs.reg_eip = eip;
        //CPU.Segs_ESval = es;
        //CPU.Segs_CSval = cs;
        //CPU.Segs_DSval = ds;
        //CPU.Segs_SSval = ss;
        //CPU.Segs_FSval = fs;
        //CPU.Segs_GSval = gs;
        //CPU.Segs_ESphys = esPhys;
        //CPU.Segs_CSphys = csPhys;
        //CPU.Segs_DSphys = dsPhys;
        //CPU.Segs_SSphys = ssPhys;
        //CPU.Segs_FSphys = fsPhys;
        //CPU.Segs_GSphys = gsPhys;
        //CPU.cpu.stack.mask=stackMask;
        //CPU.cpu.stack.notmask=stackNotMask;
    }
}
