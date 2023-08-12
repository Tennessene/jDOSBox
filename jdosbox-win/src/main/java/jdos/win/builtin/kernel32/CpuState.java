package jdos.win.builtin.kernel32;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;

public class CpuState {
    public int eax;
    public int ecx = 1;
    public int edx;
    public int ebx;
    public int esp;
    public int ebp;
    public int esi;
    public int edi;
    public int eip;

    public int es;
    public int cs;
    public int ds;
    public int ss;
    public int fs;
    public int gs;

    public int esPhys;
    public int csPhys;
    public int dsPhys;
    public int ssPhys;
    public int fsPhys;
    public int gsPhys;

    public int stackMask = 0xffffffff;
    public int stackNotMask;

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
        es = CPU_Regs.reg_esVal.dword;
        cs = CPU_Regs.reg_csVal.dword;
        ds = CPU_Regs.reg_dsVal.dword;
        ss = CPU_Regs.reg_ssVal.dword;
        fs = CPU_Regs.reg_fsVal.dword;
        gs = CPU_Regs.reg_gsVal.dword;
        esPhys = CPU_Regs.reg_esPhys.dword;
        csPhys = CPU_Regs.reg_csPhys.dword;
        dsPhys = CPU_Regs.reg_dsPhys.dword;
        ssPhys = CPU_Regs.reg_ssPhys.dword;
        fsPhys = CPU_Regs.reg_fsPhys.dword;
        gsPhys = CPU_Regs.reg_gsPhys.dword;
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
        //CPU_Regs.reg_esVal.dword = es;
        //CPU_Regs.reg_csVal.dword = cs;
        //CPU_Regs.reg_dsVal.dword = ds;
        //CPU_Regs.reg_ssVal.dword = ss;
        //CPU_Regs.reg_fsVal.dword = fs;
        //CPU_Regs.reg_gsVal.dword = gs;
        //CPU_Regs.reg_esPhys.dword = esPhys;
        //CPU_Regs.reg_csPhys.dword = csPhys;
        //CPU_Regs.reg_dsPhys.dword = dsPhys;
        //CPU_Regs.reg_ssPhys.dword = ssPhys;
        CPU_Regs.reg_fsPhys.dword = fsPhys;
        //CPU_Regs.reg_gsPhys.dword = gsPhys;
        //CPU.cpu.stack.mask=stackMask;
        //CPU.cpu.stack.notmask=stackNotMask;
    }
}
