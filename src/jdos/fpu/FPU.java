package jdos.fpu;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.debug.Debug;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class FPU {
    static public void log() {
        Debug.log_long(Debug.FPU_REG0, fpu.regs[0].ll());
        Debug.log_long(Debug.FPU_REG1, fpu.regs[1].ll());
        Debug.log_long(Debug.FPU_REG2, fpu.regs[2].ll());
        Debug.log_long(Debug.FPU_REG3, fpu.regs[3].ll());
        Debug.log_long(Debug.FPU_REG4, fpu.regs[4].ll());
        Debug.log_long(Debug.FPU_REG5, fpu.regs[5].ll());
        Debug.log_long(Debug.FPU_REG6, fpu.regs[6].ll());
        Debug.log_long(Debug.FPU_REG7, fpu.regs[7].ll());
        Debug.log_long(Debug.FPU_REG8, fpu.regs[8].ll());
        Debug.log(Debug.FPU_CW, fpu.cw);
        Debug.log(Debug.FPU_CW_MASK, fpu.cw_mask_all);
        Debug.log(Debug.FPU_SW, fpu.sw);
        Debug.log(Debug.FPU_TOP, fpu.top);
        Debug.log(Debug.FPU_ROUND, fpu.round);
    }
    
    public static class FPU_Reg {
        // union
        public FPU_Reg() {
        }
        public FPU_Reg(FPU_Reg i) {
            d = i.d;
        }
        public void copy(FPU_Reg i) {
            d = i.d;
        }
        class L {
            /*Bit32u*/int lower() {
                return (int)ll();
            }
//            void lower(long l) {
//                long ll = ll();
//                ll &=0xFFFFFFFF00000000l;
//                ll |= l;
//                ll(ll);
//            }
            /*Bit32s*/int upper() {
                return (int)(ll()>>>32);
            }
//            void upper(long u) {
//                long ll = ll();
//                ll &=0xFFFFFFFFl;
//                ll |= (u<<32);
//                ll(ll);
//            }
        }
        void ll(int lower, int upper) {
            ll(lower & 0xFFFFFFFFl | ((upper & 0xFFFFFFFFl) << 32));
        }
        long ll() {
            return Double.doubleToRawLongBits(d);
        }
        void ll(long l) {
            d = Double.longBitsToDouble(l);
        }

        L l = new L();
        public double d;
    }

    private static class FPU_P_Reg {
        /*Bit32u*/long m1;
        /*Bit32u*/long m2;
        /*Bit16u*/int m3;

        /*Bit16u*/int d1;
        /*Bit32u*/long d2;
    }

    private static final int TAG_Valid = 0;
    private static final int TAG_Zero  = 1;
    private static final int TAG_Weird = 2;
    private static final int TAG_Empty = 3;

    public static final int ROUND_Nearest = 0;
    public static final int ROUND_Down    = 1;
    public static final int ROUND_Up      = 2;
    public static final int ROUND_Chop    = 3;

    public static class FPU_rec {
        public FPU_rec() {
            for (int i=0;i<regs.length;i++)
                regs[i] = new FPU_Reg();
            for (int i=0;i<p_regs.length;i++)
                p_regs[i] = new FPU_P_Reg();
        }
        public FPU_Reg[]		regs=new FPU_Reg[9];
        public FPU_P_Reg[]	    p_regs=new FPU_P_Reg[9];
        public int[]		    tags=new int[9];
        public /*Bit16u*/int   cw,cw_mask_all;
        public /*Bit16u*/int   sw;
        public /*Bitu*/int	    top;
        public int	            round;
    }


    //get pi from a real library
    public static final double PI		=3.14159265358979323846;
    public static final double L2E		=1.4426950408889634;
    public static final double L2T		=3.3219280948873623;
    public static final double LN2		=0.69314718055994531;
    public static final double LG2		=0.3010299956639812;


    //#define TOP fpu.top
    static private int STV(int i) { return ( (fpu.top+ (i) ) & 7 ); }

    static private void FPU_SetTag(/*Bit16u*/int tag){
        for(/*Bitu*/int i=0;i<8;i++)
            fpu.tags[i] = ((tag >>(2*i))&3);
    }

    static private void FPU_SetCW(/*Bitu*/int word){
        fpu.cw = (/*Bit16u*/int)word;
        fpu.cw_mask_all = (/*Bit16u*/int)(word | 0x3f);
        fpu.round = ((word >>> 10) & 3);
    }

    static private /*Bitu*/int FPU_GET_TOP() {
        return (fpu.sw & 0x3800)>>11;
    }

    static private void FPU_SET_TOP(/*Bitu*/int val){
        fpu.sw &= ~0x3800;
        fpu.sw |= (val&7)<<11;
    }


    static private void FPU_SET_C0(/*Bitu*/int C){
        fpu.sw &= ~0x0100;
        if(C!=0) fpu.sw |=  0x0100;
    }

    static private void FPU_SET_C1(/*Bitu*/int C){
        fpu.sw &= ~0x0200;
        if(C!=0) fpu.sw |=  0x0200;
    }

    static private void FPU_SET_C2(/*Bitu*/int C){
        fpu.sw &= ~0x0400;
        if(C!=0) fpu.sw |=  0x0400;
    }

    static private void FPU_SET_C3(/*Bitu*/int C){
        fpu.sw &= ~0x4000;
        if(C!=0) fpu.sw |= 0x4000;
    }

    static private void FPU_FINIT() {
        FPU_SetCW(0x37F);
        fpu.sw = 0;
        fpu.top=FPU_GET_TOP();
        fpu.tags[0] = TAG_Empty;
        fpu.tags[1] = TAG_Empty;
        fpu.tags[2] = TAG_Empty;
        fpu.tags[3] = TAG_Empty;
        fpu.tags[4] = TAG_Empty;
        fpu.tags[5] = TAG_Empty;
        fpu.tags[6] = TAG_Empty;
        fpu.tags[7] = TAG_Empty;
        fpu.tags[8] = TAG_Valid; // is only used by us
    }

    static private void FPU_FCLEX(){
        fpu.sw &= 0x7f00;			//should clear exceptions
    }

    static private void FPU_FNOP(){
    }

    static private void FPU_PUSH(double in){
        fpu.top = (fpu.top - 1) &7;
        //actually check if empty
        fpu.tags[fpu.top] = TAG_Valid;
        fpu.regs[fpu.top].d=in;
    //	LOG(LOG_FPU,LOG_ERROR)("Pushed at %d  %g to the stack",newtop,in);
    }

    static private void FPU_PREP_PUSH(){
        fpu.top = (fpu.top - 1) &7;
        fpu.tags[fpu.top] = TAG_Valid;
    }

    static public void FPU_FPOP(){
        fpu.tags[fpu.top]=TAG_Empty;
        //maybe set zero in it as well
        fpu.top = ((fpu.top+1)&7);
    //	LOG(LOG_FPU,LOG_ERROR)("popped from %d  %g off the stack",top,fpu.regs[top].d);
    }

    static private double FROUND(double in){
        switch(fpu.round){
        case ROUND_Nearest:
            if (in-Math.floor(in)>0.5) return (Math.floor(in)+1);
            else if (in-Math.floor(in)<0.5) return (Math.floor(in));
            else return ((((long)(Math.floor(in)))&1)!=0)?(Math.floor(in)+1):(Math.floor(in));
        case ROUND_Down:
            return (Math.floor(in));
        case ROUND_Up:
            return (Math.ceil(in));
        case ROUND_Chop:
            return in; //the cast afterwards will do it right maybe cast here
        default:
            return in;
        }
    }

    static private final int BIAS80 = 16383;
    static private final int BIAS64 = 1023;

    static private class Test {
        /*Bit16s*/short begin;
        FPU_Reg eind=new FPU_Reg();
    }
    static private /*Real64*/double FPU_FLD80(long eind, int begin) {
        /*Bit64s*/long exp64 = (((begin&0x7fff) - BIAS80));
        /*Bit64s*/long blah = ((exp64 >0)?exp64:-exp64)&0x3ff;
        /*Bit64s*/long exp64final = ((exp64 >0)?blah:-blah) +BIAS64;

        // 0x3FFF is for rounding
        int round = 0;
        if (fpu.round==ROUND_Nearest)
            round = 0x3FF;
        else if (fpu.round == ROUND_Up) {
            round = 0x7FF;
        }
        /*Bit64s*/long mant64 = ((eind+round) >>> 11) & 0xfffffffffffffl;
        /*Bit64s*/long sign = (begin&0x8000)!=0?1:0;
        FPU_Reg result=new FPU_Reg();
        result.ll((sign <<63)|(exp64final << 52)| mant64);

        if(eind == 0x8000000000000000l && (begin & 0x7fff) == 0x7fff) {
		    //Detect INF and -INF (score 3.11 when drawing a slur.)
		    result.d = sign!=0?Double.NEGATIVE_INFINITY:Double.POSITIVE_INFINITY;
	    }
        return result.d;

        //mant64= test.mant80/2***64    * 2 **53
    }

    static private void FPU_ST80(/*PhysPt*/int addr,/*Bitu*/int reg) {
        /*Bit64s*/long sign80 = (fpu.regs[reg].ll() & (0x8000000000000000l))!=0?1:0;
        /*Bit64s*/long exp80 =  fpu.regs[reg].ll() & (0x7ff0000000000000l);
        /*Bit64s*/long exp80final = (exp80>>52);
        /*Bit64s*/long mant80 = fpu.regs[reg].ll() & (0x000fffffffffffffl);
        /*Bit64s*/long mant80final = (mant80 << 11);
        if(fpu.regs[reg].d != 0){ //Zero is a special case
            // Elvira wants the 8 and tcalc doesn't
            mant80final |= 0x8000000000000000l;
            //Ca-cyber doesn't like this when result is zero.
            exp80final += (BIAS80 - BIAS64);
        }
        Memory.mem_writed(addr,(int)mant80final);
        Memory.mem_writed(addr+4,(int)(mant80final>>>32));
        Memory.mem_writew(addr+8,(int)((sign80 << 15) | (exp80final)));
    }


    static private void FPU_FLD_F32(/*PhysPt*/int value,/*Bitu*/int store_to) {
        fpu.regs[store_to].d=Float.intBitsToFloat(value);
    }

    static private void FPU_FLD_F64(long value,/*Bitu*/int store_to) {
        fpu.regs[store_to].ll(value);
    }

    static private void FPU_FLD_F80(long low, int high) {
        fpu.regs[fpu.top].d=FPU_FLD80(low, high);
    }

    static private void FPU_FLD_I16(short value,/*Bitu*/int store_to) {
        fpu.regs[store_to].d=value;
    }

    static private void FPU_FLD_I32(/*PhysPt*/int value,/*Bitu*/int store_to) {
        fpu.regs[store_to].d=value;
    }

    static private void FPU_FLD_I64(long value,/*Bitu*/int store_to) {
        fpu.regs[store_to].d=value;
    }

    static private void FPU_FBLD(byte[] data,/*Bitu*/int store_to) {
        /*Bit64u*/long val = 0;
        /*Bitu*/int in = 0;
        /*Bit64u*/long base = 1;
        for(/*Bitu*/int i = 0;i < 9;i++){
            in = data[i] & 0xFF;
            val += ( (in&0xf) * base); //in&0xf shouldn't be higher then 9
            base *= 10;
            val += ((( in>>4)&0xf) * base);
            base *= 10;
        }

        //last number, only now convert to float in order to get
        //the best signification
        /*Real64*/double temp = (double)(val);
        in = data[9] & 0xFF;
        temp += ( (in&0xf) * base );
        if((in&0x80)!=0) temp *= -1.0;
        fpu.regs[store_to].d=temp;
    }


    static private void FPU_FLD_F32_EA(/*PhysPt*/int addr) {
        FPU_FLD_F32(Memory.mem_readd(addr),8);
    }
    static private void FPU_FLD_F64_EA(/*PhysPt*/int addr) {
        FPU_FLD_F64(Memory.mem_readq(addr),8);
    }
    static private void FPU_FLD_I32_EA(/*PhysPt*/int addr) {
        FPU_FLD_I32(Memory.mem_readd(addr),8);
    }
    static private void FPU_FLD_I16_EA(/*PhysPt*/int addr) {
        FPU_FLD_I16((short)Memory.mem_readw(addr),8);
    }

    static private void FPU_FST_F32(/*PhysPt*/int addr) {
        //should depend on rounding method
        Memory.mem_writed(addr,Float.floatToIntBits((float) fpu.regs[fpu.top].d));
    }

    static private void FPU_FST_F64(/*PhysPt*/int addr) {
        Memory.mem_writed(addr,fpu.regs[fpu.top].l.lower());
        Memory.mem_writed(addr+4,fpu.regs[fpu.top].l.upper());
    }

    static private void FPU_FST_F80(/*PhysPt*/int addr) {
        FPU_ST80(addr,fpu.top);
    }

    static private void FPU_FST_I16(/*PhysPt*/int addr) {
        Memory.mem_writew(addr,(short)(FROUND(fpu.regs[fpu.top].d)));
    }

    static private void FPU_FST_I32(/*PhysPt*/int addr) {
        Memory.mem_writed(addr,(int)(FROUND(fpu.regs[fpu.top].d)));
    }

    static private void FPU_FST_I64(/*PhysPt*/int addr) {
        FPU_Reg blah=new FPU_Reg();
        blah.ll((long)(FROUND(fpu.regs[fpu.top].d)));
        Memory.mem_writed(addr,blah.l.lower());
        Memory.mem_writed(addr+4,blah.l.upper());
    }

    static private void FPU_FBST(/*PhysPt*/int addr) {
        FPU_Reg val = new FPU_Reg(fpu.regs[fpu.top]);
        boolean sign = false;
        if((fpu.regs[fpu.top].ll() & 0x8000000000000000l)!=0) { //sign
            sign=true;
            val.d=-val.d;
        }
        //numbers from back to front
        /*Real64*/double temp=val.d;
        /*Bitu*/int p;
        for(/*Bitu*/int i=0;i<9;i++){
            val.d=temp;
            temp = (double)((long)(Math.floor(val.d/10.0)));
            p = (int)(val.d - 10.0*temp);
            val.d=temp;
            temp = (double)((long)(Math.floor(val.d/10.0)));
            p |= ((int)(val.d - 10.0*temp)<<4);

            Memory.mem_writeb(addr+i,p);
        }
        val.d=temp;
        temp = (double)((long)(Math.floor(val.d/10.0)));
        p = (int)(val.d - 10.0*temp);
        if(sign)
            p|=0x80;
        Memory.mem_writeb(addr+9,p);
    }

    static private void FPU_FADD(/*Bitu*/int op1, /*Bitu*/int op2){
        fpu.regs[op1].d+=fpu.regs[op2].d;
        //flags and such :)
    }

    static private void FPU_FSIN(){
        fpu.regs[fpu.top].d = Math.sin(fpu.regs[fpu.top].d);
        FPU_SET_C2(0);
        //flags and such :)
    }

    static private void FPU_FSINCOS(){
        /*Real64*/double temp = fpu.regs[fpu.top].d;
        fpu.regs[fpu.top].d = Math.sin(temp);
        FPU_PUSH(Math.cos(temp));
        FPU_SET_C2(0);
        //flags and such :)
    }

    static private void FPU_FCOS(){
        fpu.regs[fpu.top].d = Math.cos(fpu.regs[fpu.top].d);
        FPU_SET_C2(0);
        //flags and such :)
    }

    static private void FPU_FSQRT(){
        fpu.regs[fpu.top].d = Math.sqrt(fpu.regs[fpu.top].d);
        //flags and such :)
    }
    static private void FPU_FPATAN(){
        fpu.regs[STV(1)].d = Math.atan2(fpu.regs[STV(1)].d, fpu.regs[fpu.top].d);
        FPU_FPOP();
        //flags and such :)
    }
    static private void FPU_FPTAN(){
        fpu.regs[fpu.top].d = Math.tan(fpu.regs[fpu.top].d);
        FPU_PUSH(1.0);
        FPU_SET_C2(0);
        //flags and such :)
    }
    static private void FPU_FDIV(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d= fpu.regs[st].d/fpu.regs[other].d;
        //flags and such :)
    }

    static private void FPU_FDIVR(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d= fpu.regs[other].d/fpu.regs[st].d;
        // flags and such :)
    }

    static private void FPU_FMUL(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d*=fpu.regs[other].d;
        //flags and such :)
    }

    static private void FPU_FSUB(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d = fpu.regs[st].d - fpu.regs[other].d;
        //flags and such :)
    }

    static private void FPU_FSUBR(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d= fpu.regs[other].d - fpu.regs[st].d;
        //flags and such :)
    }

    static private void FPU_FXCH(/*Bitu*/int st, /*Bitu*/int other){
        int tag = fpu.tags[other];
        double reg = fpu.regs[other].d;
        fpu.tags[other] = fpu.tags[st];
        fpu.regs[other].d = fpu.regs[st].d;
        fpu.tags[st] = tag;
        fpu.regs[st].d = reg;
    }

    static private void FPU_FST(/*Bitu*/int st, /*Bitu*/int other){
        fpu.tags[other] = fpu.tags[st];
        fpu.regs[other].d = fpu.regs[st].d;
    }


    static private void FPU_FCOM(/*Bitu*/int st, /*Bitu*/int other){
        if(((fpu.tags[st] != TAG_Valid) && (fpu.tags[st] != TAG_Zero)) ||
            ((fpu.tags[other] != TAG_Valid) && (fpu.tags[other] != TAG_Zero)) || Double.isNaN(fpu.regs[st].d) || Double.isNaN(fpu.regs[other].d)){
            FPU_SET_C3(1);FPU_SET_C2(1);FPU_SET_C0(1);return;
        }
        if(fpu.regs[st].d == fpu.regs[other].d){
            FPU_SET_C3(1);FPU_SET_C2(0);FPU_SET_C0(0);return;
        }
        if(fpu.regs[st].d < fpu.regs[other].d){
            FPU_SET_C3(0);FPU_SET_C2(0);FPU_SET_C0(1);return;
        }
        // st > other
        FPU_SET_C3(0);FPU_SET_C2(0);FPU_SET_C0(0);return;
    }

    static private void FPU_FUCOM(/*Bitu*/int st, /*Bitu*/int other){
        //does atm the same as fcom
        FPU_FCOM(st,other);
    }

    static private void FPU_FRNDINT(){
        /*Bit64s*/long temp= (long)(FROUND(fpu.regs[fpu.top].d));
        fpu.regs[fpu.top].d=(double)(temp);
    }

    static private void FPU_FPREM(){
        /*Real64*/double valtop = fpu.regs[fpu.top].d;
        /*Real64*/double valdiv = fpu.regs[STV(1)].d;
        /*Bit64s*/long ressaved = (long)( (valtop/valdiv) );
    // Some backups
    //	/*Real64*/double res=valtop - ressaved*valdiv;
    //      res= fmod(valtop,valdiv);
        fpu.regs[fpu.top].d = valtop - ressaved*valdiv;
        FPU_SET_C0((int)(ressaved&4));
        FPU_SET_C3((int)(ressaved&2));
        FPU_SET_C1((int)(ressaved&1));
        FPU_SET_C2(0);
    }

    static private void FPU_FPREM1(){
        /*Real64*/double valtop = fpu.regs[fpu.top].d;
        /*Real64*/double valdiv = fpu.regs[STV(1)].d;
        double quot = valtop/valdiv;
        double quotf = Math.floor(quot);
        /*Bit64s*/long ressaved;
        if (quot-quotf>0.5) ressaved = (long)(quotf+1);
        else if (quot-quotf<0.5) ressaved = (long)(quotf);
        else ressaved = (long)(((((long)(quotf))&1)!=0)?(quotf+1):(quotf));
        fpu.regs[fpu.top].d = valtop - ressaved*valdiv;
        FPU_SET_C0((int)(ressaved&4));
        FPU_SET_C3((int)(ressaved&2));
        FPU_SET_C1((int)(ressaved&1));
        FPU_SET_C2(0);
    }

    static private void FPU_FXAM(){
        if((fpu.regs[fpu.top].ll() & 0x8000000000000000l)!=0)	//sign
        {
            FPU_SET_C1(1);
        }
        else
        {
            FPU_SET_C1(0);
        }

        if(fpu.tags[fpu.top] == TAG_Empty)
        {
            FPU_SET_C3(1);FPU_SET_C2(0);FPU_SET_C0(1);
            return;
        }
        if(Double.isNaN(fpu.regs[fpu.top].d)) {
            FPU_SET_C3(0);FPU_SET_C2(0);FPU_SET_C0(1);
        } else if(Double.isInfinite(fpu.regs[fpu.top].d)) {
            FPU_SET_C3(0);FPU_SET_C2(1);FPU_SET_C0(1);
        } else if(fpu.regs[fpu.top].d == 0.0)		//zero or normalized number.
        {
            FPU_SET_C3(1);FPU_SET_C2(0);FPU_SET_C0(0);
        }
        else
        {
            FPU_SET_C3(0);FPU_SET_C2(1);FPU_SET_C0(0);
        }
    }


    static private void FPU_F2XM1(){
        fpu.regs[fpu.top].d = Math.pow(2.0,fpu.regs[fpu.top].d) - 1;
    }

    static private void FPU_FYL2X(){
        fpu.regs[STV(1)].d*=Math.log(fpu.regs[fpu.top].d)/Math.log(2.0);
        FPU_FPOP();
    }

    static private void FPU_FYL2XP1(){
        fpu.regs[STV(1)].d*=Math.log(fpu.regs[fpu.top].d+1.0)/Math.log(2.0);
        FPU_FPOP();
    }

    static private void FPU_FSCALE(){
        fpu.regs[fpu.top].d *= Math.pow(2.0, (double) ((long) (fpu.regs[STV(1)].d)));
        //2^x where x is chopped.
    }

    static private void FPU_FSTENV(/*PhysPt*/int addr){
        FPU_SET_TOP(fpu.top);
        if(!CPU.cpu.code.big) {
            Memory.mem_writew(addr+0,(fpu.cw));
            Memory.mem_writew(addr+2,(fpu.sw));
            Memory.mem_writew(addr+4,(FPU_GetTag()));
        } else {
            Memory.mem_writed(addr+0,(fpu.cw));
            Memory.mem_writed(addr+4,(fpu.sw));
            Memory.mem_writed(addr+8,(FPU_GetTag()));
        }
    }

    static private void FPU_FLDENV(/*PhysPt*/int addr){
        /*Bit16u*/int tag;
        /*Bit32u*/long tagbig;
        /*Bitu*/int cw;
        if(!CPU.cpu.code.big) {
            cw     = Memory.mem_readw(addr+0);
            fpu.sw = Memory.mem_readw(addr+2);
            tag    = Memory.mem_readw(addr+4);
        } else {
            cw     = Memory.mem_readd(addr + 0);
            fpu.sw = Memory.mem_readd(addr + 4);
            tagbig = Memory.mem_readd(addr + 8) & 0xFFFFFFFFl;
            tag    = (int)(tagbig);
        }
        FPU_SetTag(tag);
        FPU_SetCW(cw);
        fpu.top = FPU_GET_TOP();
    }

    static private void FPU_FSAVE(/*PhysPt*/int addr){
        FPU_FSTENV(addr);
        /*Bitu*/int start = (CPU.cpu.code.big?28:14);
        for(/*Bitu*/int i = 0;i < 8;i++){
            FPU_ST80(addr+start,STV(i));
            start += 10;
        }
        FPU_FINIT();
    }

    static private void FPU_FRSTOR(/*PhysPt*/int addr){
        FPU_FLDENV(addr);
        /*Bitu*/int start = (CPU.cpu.code.big?28:14);
        for(/*Bitu*/int i = 0;i < 8;i++){
            fpu.regs[STV(i)].d = FPU_FLD80(Memory.mem_readq(addr+start), Memory.mem_readw(addr+start+8));
            start += 10;
        }
    }

    static private void FPU_FXTRACT() {
        // function stores real bias in st and
        // pushes the significant number onto the stack
        // if double ever uses a different base please correct this function

        FPU_Reg test = new FPU_Reg(fpu.regs[fpu.top]);
        /*Bit64s*/long exp80 =  test.ll() & 0x7ff0000000000000l;
        /*Bit64s*/long exp80final = (exp80>>52) - BIAS64;
        /*Real64*/double mant = test.d / (Math.pow(2.0,(double)(exp80final)));
        fpu.regs[fpu.top].d = (double)(exp80final);
        FPU_PUSH(mant);
    }

    static private void FPU_FCHS(){
        fpu.regs[fpu.top].d = -1.0*(fpu.regs[fpu.top].d);
    }

    static private void FPU_FABS(){
        fpu.regs[fpu.top].d = Math.abs(fpu.regs[fpu.top].d);
    }

    static private void FPU_FTST(){
        fpu.regs[8].d = 0.0;
        FPU_FCOM(fpu.top,8);
    }

    static private void FPU_FLD1(){
        FPU_PREP_PUSH();
        fpu.regs[fpu.top].d = 1.0;
    }

    static private void FPU_FLDL2T(){
        FPU_PREP_PUSH();
        fpu.regs[fpu.top].d = L2T;
    }

    static private void FPU_FLDL2E(){
        FPU_PREP_PUSH();
        fpu.regs[fpu.top].d = L2E;
    }

    static private void FPU_FLDPI(){
        FPU_PREP_PUSH();
        fpu.regs[fpu.top].d = PI;
    }

    static private void FPU_FLDLG2(){
        FPU_PREP_PUSH();
        fpu.regs[fpu.top].d = LG2;
    }

    static private void FPU_FLDLN2(){
        FPU_PREP_PUSH();
        fpu.regs[fpu.top].d = LN2;
    }

    static private void FPU_FLDZ(){
        FPU_PREP_PUSH();
        fpu.regs[fpu.top].d = 0.0;
        fpu.tags[fpu.top] = TAG_Zero;
    }


    static private void FPU_FADD_EA(/*Bitu*/int op1){
        FPU_FADD(op1,8);
    }
    static private void FPU_FMUL_EA(/*Bitu*/int op1){
        FPU_FMUL(op1,8);
    }
    static private void FPU_FSUB_EA(/*Bitu*/int op1){
        FPU_FSUB(op1,8);
    }
    static private void FPU_FSUBR_EA(/*Bitu*/int op1){
        FPU_FSUBR(op1,8);
    }
    static private void FPU_FDIV_EA(/*Bitu*/int op1){
        FPU_FDIV(op1,8);
    }
    static private void FPU_FDIVR_EA(/*Bitu*/int op1){
        FPU_FDIVR(op1,8);
    }
    static private void FPU_FCOM_EA(/*Bitu*/int op1){
        FPU_FCOM(op1,8);
    }

    public static FPU_rec fpu=new FPU_rec();

    static private void FPU_FLDCW(/*PhysPt*/int addr){
        /*Bit16u*/int temp = Memory.mem_readw(addr);
        FPU_SetCW(temp);
    }

    static private /*Bit16u*/int FPU_GetTag(){
        /*Bit16u*/int tag=0;
        for(/*Bitu*/int i=0;i<8;i++)
            tag |= ( (fpu.tags[i]&3) <<(2*i));
        return tag;
    }

/* WATCHIT : ALWAYS UPDATE REGISTERS BEFORE AND AFTER USING THEM
			STATUS WORD =>	FPU_SET_TOP(fpu.top) BEFORE a read
			fpu.top=FPU_GET_TOP() after a write;
			*/

    static private void EATREE(/*Bitu*/int _rm){
        /*Bitu*/int group=(_rm >> 3) & 7;
        switch(group){
            case 0x00:	/* FADD */
                FPU_FADD_EA(fpu.top);
                break;
            case 0x01:	/* FMUL  */
                FPU_FMUL_EA(fpu.top);
                break;
            case 0x02:	/* FCOM */
                FPU_FCOM_EA(fpu.top);
                break;
            case 0x03:	/* FCOMP */
                FPU_FCOM_EA(fpu.top);
                FPU_FPOP();
                break;
            case 0x04:	/* FSUB */
                FPU_FSUB_EA(fpu.top);
                break;
            case 0x05:	/* FSUBR */
                FPU_FSUBR_EA(fpu.top);
                break;
            case 0x06:	/* FDIV */
                FPU_FDIV_EA(fpu.top);
                break;
            case 0x07:	/* FDIVR */
                FPU_FDIVR_EA(fpu.top);
                break;
            default:
                break;
        }
    }

    static public void FPU_ESC0_EA(/*Bitu*/int rm,/*PhysPt*/int addr) {
        if (softFPU) {
            SoftFPU.FPU_ESC0_EA(rm, addr);
            return;
        }
        /* REGULAR TREE WITH 32 BITS REALS */
        FPU_FLD_F32_EA(addr);
        EATREE(rm);
    }

    static public void FPU_ESC0_Normal(/*Bitu*/int rm) {
        if (softFPU) {
            SoftFPU.FPU_ESC0_Normal(rm);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch (group){
        case 0x00:		/* FADD ST,STi */
            FPU_FADD(fpu.top,STV(sub));
            break;
        case 0x01:		/* FMUL  ST,STi */
            FPU_FMUL(fpu.top,STV(sub));
            break;
        case 0x02:		/* FCOM  STi */
            FPU_FCOM(fpu.top,STV(sub));
            break;
        case 0x03:		/* FCOMP STi */
            FPU_FCOM(fpu.top,STV(sub));
            FPU_FPOP();
            break;
        case 0x04:		/* FSUB  ST,STi */
            FPU_FSUB(fpu.top,STV(sub));
            break;
        case 0x05:		/* FSUBR ST,STi */
            FPU_FSUBR(fpu.top,STV(sub));
            break;
        case 0x06:		/* FDIV  ST,STi */
            FPU_FDIV(fpu.top,STV(sub));
            break;
        case 0x07:		/* FDIVR ST,STi */
            FPU_FDIVR(fpu.top,STV(sub));
            break;
        default:
            break;
        }
    }

    static public void FPU_ESC1_EA(/*Bitu*/int rm,/*PhysPt*/int addr) {
        if (softFPU) {
            SoftFPU.FPU_ESC1_EA(rm, addr);
            return;
        }
        // floats
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00: /* FLD float*/
            int value = Memory.mem_readd(addr); // might generate PF, so do before we adjust the stack
            FPU_PREP_PUSH();
            FPU_FLD_F32(value,fpu.top);
            break;
        case 0x01: /* UNKNOWN */
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU, LogSeverities.LOG_WARN,"ESC EA 1:Unhandled group "+group+" subfunction "+sub);
            break;
        case 0x02: /* FST float*/
            FPU_FST_F32(addr);
            break;
        case 0x03: /* FSTP float*/
            FPU_FST_F32(addr);
            FPU_FPOP();
            break;
        case 0x04: /* FLDENV */
            FPU_FLDENV(addr);
            break;
        case 0x05: /* FLDCW */
            FPU_FLDCW(addr);
            break;
        case 0x06: /* FSTENV */
            FPU_FSTENV(addr);
            break;
        case 0x07:  /* FNSTCW*/
            Memory.mem_writew(addr,fpu.cw);
            break;
        default:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC EA 1:Unhandled group "+group+" subfunction "+sub);
            break;
        }
    }

    static public void FPU_ESC1_Normal(/*Bitu*/int rm) {
        if (softFPU) {
            SoftFPU.FPU_ESC1_Normal(rm);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch (group){
        case 0x00: /* FLD STi */
            {
                /*Bitu*/int reg_from=STV(sub);
                FPU_PREP_PUSH();
                FPU_FST(reg_from, fpu.top);
                break;
            }
        case 0x01: /* FXCH STi */
            FPU_FXCH(fpu.top,STV(sub));
            break;
        case 0x02: /* FNOP */
            FPU_FNOP();
            break;
        case 0x03: /* FSTP STi */
            FPU_FST(fpu.top,STV(sub));
            FPU_FPOP();
            break;
        case 0x04:
            switch(sub){
            case 0x00:       /* FCHS */
                FPU_FCHS();
                break;
            case 0x01:       /* FABS */
                FPU_FABS();
                break;
            case 0x02:       /* UNKNOWN */
            case 0x03:       /* ILLEGAL */
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 1:Unhandled group "+group+" subfunction "+sub);
                break;
            case 0x04:       /* FTST */
                FPU_FTST();
                break;
            case 0x05:       /* FXAM */
                FPU_FXAM();
                break;
            case 0x06:       /* FTSTP (cyrix)*/
            case 0x07:       /* UNKNOWN */
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 1:Unhandled group "+group+" subfunction "+sub);
                break;
            }
            break;
        case 0x05:
            switch(sub){
            case 0x00:       /* FLD1 */
                FPU_FLD1();
                break;
            case 0x01:       /* FLDL2T */
                FPU_FLDL2T();
                break;
            case 0x02:       /* FLDL2E */
                FPU_FLDL2E();
                break;
            case 0x03:       /* FLDPI */
                FPU_FLDPI();
                break;
            case 0x04:       /* FLDLG2 */
                FPU_FLDLG2();
                break;
            case 0x05:       /* FLDLN2 */
                FPU_FLDLN2();
                break;
            case 0x06:       /* FLDZ*/
                FPU_FLDZ();
                break;
            case 0x07:       /* ILLEGAL */
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 1:Unhandled group "+group+" subfunction "+sub);
                break;
            }
            break;
        case 0x06:
            switch(sub){
            case 0x00:	/* F2XM1 */
                FPU_F2XM1();
                break;
            case 0x01:	/* FYL2X */
                FPU_FYL2X();
                break;
            case 0x02:	/* FPTAN  */
                FPU_FPTAN();
                break;
            case 0x03:	/* FPATAN */
                FPU_FPATAN();
                break;
            case 0x04:	/* FXTRACT */
                FPU_FXTRACT();
                break;
            case 0x05:	/* FPREM1 */
                FPU_FPREM1();
                break;
            case 0x06:	/* FDECSTP */
                fpu.top = (fpu.top - 1) & 7;
                break;
            case 0x07:	/* FINCSTP */
                fpu.top = (fpu.top + 1) & 7;
                break;
            default:
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 1:Unhandled group "+group+" subfunction "+sub);
                break;
            }
            break;
        case 0x07:
            switch(sub){
            case 0x00:		/* FPREM */
                FPU_FPREM();
                break;
            case 0x01:		/* FYL2XP1 */
                FPU_FYL2XP1();
                break;
            case 0x02:		/* FSQRT */
                FPU_FSQRT();
                break;
            case 0x03:		/* FSINCOS */
                FPU_FSINCOS();
                break;
            case 0x04:		/* FRNDINT */
                FPU_FRNDINT();
                break;
            case 0x05:		/* FSCALE */
                FPU_FSCALE();
                break;
            case 0x06:		/* FSIN */
                FPU_FSIN();
                break;
            case 0x07:		/* FCOS */
                FPU_FCOS();
                break;
            default:
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 1:Unhandled group "+group+" subfunction "+sub);
                break;
            }
            break;
            default:
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 1:Unhandled group "+group+" subfunction "+sub);
        }
    }


    static public void FPU_ESC2_EA(/*Bitu*/int rm,/*PhysPt*/int addr) {
        if (softFPU) {
            SoftFPU.FPU_ESC2_EA(rm, addr);
            return;
        }
        /* 32 bits integer operants */
        FPU_FLD_I32_EA(addr);
        EATREE(rm);
    }

    static public void FPU_ESC2_Normal(/*Bitu*/int rm) {
        if (softFPU) {
            SoftFPU.FPU_ESC2_Normal(rm);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x05:
            switch(sub){
            case 0x01:		/* FUCOMPP */
                FPU_FUCOM(fpu.top,STV(1));
                FPU_FPOP();
                FPU_FPOP();
                break;
            default:
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 2:Unhandled group "+group+" subfunction "+sub);
                break;
            }
            break;
        default:
               if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 2:Unhandled group "+group+" subfunction "+sub);
            break;
        }
    }


    static public void FPU_ESC3_EA(/*Bitu*/int rm,/*PhysPt*/int addr) {
        if (softFPU) {
            SoftFPU.FPU_ESC3_EA(rm, addr);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00:	/* FILD */
        {
            int value = Memory.mem_readd(addr); // might generate PF, so do before we adjust the stack
            FPU_PREP_PUSH();
            FPU_FLD_I32(value,fpu.top);
            break;
        }
        case 0x01:	/* FISTTP */
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 3 EA:Unhandled group "+group+" subfunction "+sub);
            break;
        case 0x02:	/* FIST */
            FPU_FST_I32(addr);
            break;
        case 0x03:	/* FISTP */
            FPU_FST_I32(addr);
            FPU_FPOP();
            break;
        case 0x05:	/* FLD 80 Bits Real */
        {
            long low = Memory.mem_readq(addr); // might generate PF, so do before we adjust the stack
            int high = Memory.mem_readw(addr+8);
            FPU_PREP_PUSH();
            FPU_FLD_F80(low, high);
            break;
        }
        case 0x07:	/* FSTP 80 Bits Real */
            FPU_FST_F80(addr);
            FPU_FPOP();
            break;
        default:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 3 EA:Unhandled group "+group+" subfunction "+sub);
        }
    }

    static public void FPU_ESC3_Normal(/*Bitu*/int rm) {
        if (softFPU) {
            SoftFPU.FPU_ESC3_Normal(rm);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch (group) {
        case 0x04:
            switch (sub) {
            case 0x00:				//FNENI
            case 0x01:				//FNDIS
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_ERROR,"8087 only fpu code used esc 3: group 4: subfuntion :"+sub);
                break;
            case 0x02:				//FNCLEX FCLEX
                FPU_FCLEX();
                break;
            case 0x03:				//FNINIT FINIT
                FPU_FINIT();
                break;
            case 0x04:				//FNSETPM
            case 0x05:				//FRSTPM
//			Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_ERROR,"80267 protected mode (un)set. Nothing done");
                FPU_FNOP();
                break;
            default:
                Log.exit("ESC 3:ILLEGAL OPCODE group "+group+" subfunction "+sub);
            }
            break;
        default:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 3:Unhandled group "+group+" subfunction "+sub);
            break;
        }
    }


    static public void FPU_ESC4_EA(/*Bitu*/int rm,/*PhysPt*/int addr) {
        if (softFPU) {
            SoftFPU.FPU_ESC4_EA(rm, addr);
            return;
        }
        /* REGULAR TREE WITH 64 BITS REALS */
        FPU_FLD_F64_EA(addr);
        EATREE(rm);
    }

    static public void FPU_ESC4_Normal(/*Bitu*/int rm) {
        if (softFPU) {
            SoftFPU.FPU_ESC4_Normal(rm);
            return;
        }
        /* LOOKS LIKE number 6 without popping */
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00:	/* FADD STi,ST*/
            FPU_FADD(STV(sub),fpu.top);
            break;
        case 0x01:	/* FMUL STi,ST*/
            FPU_FMUL(STV(sub),fpu.top);
            break;
        case 0x02:  /* FCOM*/
            FPU_FCOM(fpu.top,STV(sub));
            break;
        case 0x03:  /* FCOMP*/
            FPU_FCOM(fpu.top,STV(sub));
            FPU_FPOP();
            break;
        case 0x04:  /* FSUBR STi,ST*/
            FPU_FSUBR(STV(sub),fpu.top);
            break;
        case 0x05:  /* FSUB  STi,ST*/
            FPU_FSUB(STV(sub),fpu.top);
            break;
        case 0x06:  /* FDIVR STi,ST*/
            FPU_FDIVR(STV(sub),fpu.top);
            break;
        case 0x07:  /* FDIV STi,ST*/
            FPU_FDIV(STV(sub),fpu.top);
            break;
        default:
            break;
        }
    }

    static public void FPU_ESC5_EA(/*Bitu*/int rm,/*PhysPt*/int addr) {
        if (softFPU) {
            SoftFPU.FPU_ESC5_EA(rm, addr);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00:  /* FLD double real*/
        {
            long value = Memory.mem_readq(addr); // might generate PF, so do before we adjust the stack
            FPU_PREP_PUSH();
            FPU_FLD_F64(value,fpu.top);
            break;
        }
        case 0x01:  /* FISTTP longint*/
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 5 EA:Unhandled group "+group+" subfunction "+sub);
            break;
        case 0x02:   /* FST double real*/
            FPU_FST_F64(addr);
            break;
        case 0x03:	/* FSTP double real*/
            FPU_FST_F64(addr);
            FPU_FPOP();
            break;
        case 0x04:	/* FRSTOR */
            FPU_FRSTOR(addr);
            break;
        case 0x06:	/* FSAVE */
            FPU_FSAVE(addr);
            break;
        case 0x07:   /*FNSTSW    NG DISAGREES ON THIS*/
            FPU_SET_TOP(fpu.top);
            Memory.mem_writew(addr,fpu.sw);
            //seems to break all dos4gw games :)
            break;
        default:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 5 EA:Unhandled group "+group+" subfunction "+sub);
        }
    }

    static public void FPU_ESC5_Normal(/*Bitu*/int rm) {
        if (softFPU) {
            SoftFPU.FPU_ESC5_Normal(rm);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00: /* FFREE STi */
            fpu.tags[STV(sub)]=TAG_Empty;
            break;
        case 0x01: /* FXCH STi*/
            FPU_FXCH(fpu.top,STV(sub));
            break;
        case 0x02: /* FST STi */
            FPU_FST(fpu.top,STV(sub));
            break;
        case 0x03:  /* FSTP STi*/
            FPU_FST(fpu.top,STV(sub));
            FPU_FPOP();
            break;
        case 0x04:	/* FUCOM STi */
            FPU_FUCOM(fpu.top,STV(sub));
            break;
        case 0x05:	/*FUCOMP STi */
            FPU_FUCOM(fpu.top,STV(sub));
            FPU_FPOP();
            break;
        default:
        if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 5:Unhandled group "+group+" subfunction "+sub);
        break;
        }
    }

    static public void FPU_ESC6_EA(/*Bitu*/int rm,/*PhysPt*/int addr) {
        if (softFPU) {
            SoftFPU.FPU_ESC6_EA(rm, addr);
            return;
        }
        /* 16 bit (word integer) operants */
        FPU_FLD_I16_EA(addr);
        EATREE(rm);
    }

    static public void FPU_ESC6_Normal(/*Bitu*/int rm) {
        if (softFPU) {
            SoftFPU.FPU_ESC6_Normal(rm);
            return;
        }

        /* all P variants working only on registers */
        /* get top before switch and pop afterwards */
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00:	/*FADDP STi,ST*/
            FPU_FADD(STV(sub),fpu.top);
            break;
        case 0x01:	/* FMULP STi,ST*/
            FPU_FMUL(STV(sub),fpu.top);
            break;
        case 0x02:  /* FCOMP5*/
            FPU_FCOM(fpu.top,STV(sub));
            break;	/* TODO IS THIS ALLRIGHT ????????? */
        case 0x03:  /*FCOMPP*/
            if(sub != 1) {
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 6:Unhandled group "+group+" subfunction "+sub);
                return;
            }
            FPU_FCOM(fpu.top,STV(1));
            FPU_FPOP(); /* extra pop at the bottom*/
            break;
        case 0x04:  /* FSUBRP STi,ST*/
            FPU_FSUBR(STV(sub),fpu.top);
            break;
        case 0x05:  /* FSUBP  STi,ST*/
            FPU_FSUB(STV(sub),fpu.top);
            break;
        case 0x06:	/* FDIVRP STi,ST*/
            FPU_FDIVR(STV(sub),fpu.top);
            break;
        case 0x07:  /* FDIVP STi,ST*/
            FPU_FDIV(STV(sub),fpu.top);
            break;
        default:
            break;
        }
        FPU_FPOP();
    }


    static public void FPU_ESC7_EA(/*Bitu*/int rm,/*PhysPt*/int addr) {
        if (softFPU) {
            SoftFPU.FPU_ESC7_EA(rm, addr);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00:  /* FILD Bit16s */
        {
            short value = (short)Memory.mem_readw(addr); // might generate PF, so do before we adjust the stack
            FPU_PREP_PUSH();
            FPU_FLD_I16(value,fpu.top);
        }
            break;
        case 0x01:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 7 EA:Unhandled group "+group+" subfunction "+sub);
            break;
        case 0x02:   /* FIST Bit16s */
            FPU_FST_I16(addr);
            break;
        case 0x03:	/* FISTP Bit16s */
            FPU_FST_I16(addr);
            FPU_FPOP();
            break;
        case 0x04:   /* FBLD packed BCD */
        {
            byte[] value = new byte[10];
            Memory.mem_memcpy(value, 0, addr, 10); // might generate PF, so do before we adjust the stack
            FPU_PREP_PUSH();
            FPU_FBLD(value,fpu.top);
            break;
        }
        case 0x05:  /* FILD Bit64s */
        {
            long value = Memory.mem_readq(addr); // might generate PF, so do before we adjust the stack
            FPU_PREP_PUSH();
            FPU_FLD_I64(value,fpu.top);
            break;
        }
        case 0x06:	/* FBSTP packed BCD */
            FPU_FBST(addr);
            FPU_FPOP();
            break;
        case 0x07:  /* FISTP Bit64s */
            FPU_FST_I64(addr);
            FPU_FPOP();
            break;
        default:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 7 EA:Unhandled group "+group+" subfunction "+sub);
            break;
        }
    }

    static public void FPU_ESC7_Normal(/*Bitu*/int rm) {
        if (softFPU) {
            SoftFPU.FPU_ESC7_Normal(rm);
            return;
        }
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch (group){
        case 0x00: /* FFREEP STi*/
            fpu.tags[STV(sub)]=TAG_Empty;
            FPU_FPOP();
            break;
        case 0x01: /* FXCH STi*/
            FPU_FXCH(fpu.top,STV(sub));
            break;
        case 0x02:  /* FSTP STi*/
        case 0x03:  /* FSTP STi*/
            FPU_FST(fpu.top,STV(sub));
            FPU_FPOP();
            break;
        case 0x04:
            switch(sub){
                case 0x00:     /* FNSTSW AX*/
                    FPU_SET_TOP(fpu.top);
                    CPU_Regs.reg_eax.word(fpu.sw);
                    break;
                default:
                    if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 7:Unhandled group "+group+" subfunction "+sub);
                    break;
            }
            break;
        default:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 7:Unhandled group "+group+" subfunction "+sub);
            break;
        }
    }

    public static boolean softFPU = false;

    public static Section.SectionFunction FPU_Init = new Section.SectionFunction() {
        public void call(Section configuration) {
            FPU_FINIT();
            SoftFPU.FPU_FINIT();
            Section_prop section=(Section_prop)configuration;
            softFPU = section.Get_bool("softfpu");
        }
    };
}
