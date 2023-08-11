package jdos.fpu;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.debug.Debug;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.MicroDouble;

public class SoftFPU {
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
            return d;
        }
        void ll(long l) {
            d = l;
        }

        L l = new L();
        public long d;
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


    public static final long PI		    =MicroDouble.PI;
    public static final long L2E		=MicroDouble.INV_LN2;
    public static final long L2T		=MicroDouble.parseDouble("3.3219280948873623");
    public static final long LN2		=MicroDouble.LN2;
    public static final long LG2		=MicroDouble.parseDouble("0.3010299956639812");


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

    static void FPU_FINIT() {
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

    static private void FPU_PUSH(long in){
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

    static private long FROUND(long in){
        switch(fpu.round){
        case ROUND_Nearest:
        {
            long floor = MicroDouble.floor(in);
            long diff = MicroDouble.sub(in, floor);
            if (MicroDouble.gt(diff, MicroDouble.ONE_HALF)) return MicroDouble.ceil(in);
            else if (MicroDouble.lt(diff, MicroDouble.ONE_HALF)) return floor;
            else return ((MicroDouble.longValue(floor)&1)!=0)?MicroDouble.ceil(in):floor;
        }
        case ROUND_Down:
            return MicroDouble.floor(in);
        case ROUND_Up:
            return MicroDouble.ceil(in);
        case ROUND_Chop:
            return MicroDouble.truncate(in);
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
    static private /*Real64*/long FPU_FLD80(/*PhysPt*/int addr) {
        Test test = new Test();
        test.eind.ll(Memory.mem_readd(addr), Memory.mem_readd(addr + 4));
        test.begin = (short)Memory.mem_readw(addr+8);

        /*Bit64s*/long exp64 = (((test.begin&0x7fff) - BIAS80));
        /*Bit64s*/long blah = ((exp64 >0)?exp64:-exp64)&0x3ff;
        /*Bit64s*/long exp64final = ((exp64 >0)?blah:-blah) +BIAS64;

        // 0x3FFF is for rounding
        /*Bit64s*/long mant64 = ((test.eind.ll()+0x3FF) >>> 11) & 0xfffffffffffffl;
        /*Bit64s*/long sign = (test.begin&0x8000)!=0?1:0;
        FPU_Reg result=new FPU_Reg();
        result.ll((sign <<63)|(exp64final << 52)| mant64);

        if(test.eind.ll() == 0x8000000000000000l && (test.begin & 0x7fff) == 0x7fff) {
		    //Detect INF and -INF (score 3.11 when drawing a slur.)
		    result.d = sign!=0?MicroDouble.NEGATIVE_INFINITY:MicroDouble.POSITIVE_INFINITY;
	    }
        return result.d;

        //mant64= test.mant80/2***64    * 2 **53
    }

    static private void FPU_ST80(/*PhysPt*/int addr,/*Bitu*/int reg) {
        Test test = new Test();
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
        test.begin = (short)(((short)(sign80)<<15)| (short)(exp80final));
        test.eind.ll(mant80final);
        Memory.mem_writed(addr,test.eind.l.lower());
        Memory.mem_writed(addr+4,test.eind.l.upper());
        Memory.mem_writew(addr+8,test.begin);
    }


    static private void FPU_FLD_F32(/*PhysPt*/int addr,/*Bitu*/int store_to) {
        fpu.regs[store_to].d= MicroDouble.floatToDouble(Memory.mem_readd(addr));
    }

    static private void FPU_FLD_F64(/*PhysPt*/int addr,/*Bitu*/int store_to) {
        fpu.regs[store_to].ll(Memory.mem_readd(addr), Memory.mem_readd(addr + 4));
    }

    static private void FPU_FLD_F80(/*PhysPt*/int addr) {
        fpu.regs[fpu.top].d=FPU_FLD80(addr);
    }

    static private void FPU_FLD_I16(/*PhysPt*/int addr,/*Bitu*/int store_to) {
        fpu.regs[store_to].d=MicroDouble.intToDouble((short)Memory.mem_readw(addr));
    }

    static private void FPU_FLD_I32(/*PhysPt*/int addr,/*Bitu*/int store_to) {
        fpu.regs[store_to].d=MicroDouble.intToDouble(Memory.mem_readd(addr));
    }

    static private void FPU_FLD_I64(/*PhysPt*/int addr,/*Bitu*/int store_to) {
        FPU_Reg blah=new FPU_Reg();
        blah.ll(Memory.mem_readd(addr), Memory.mem_readd(addr + 4));
        fpu.regs[store_to].d=MicroDouble.longToDouble(blah.ll());
    }

    static private void FPU_FBLD(/*PhysPt*/int addr,/*Bitu*/int store_to) {
        /*Bit64u*/long val = 0;
        /*Bitu*/int in = 0;
        /*Bit64u*/long base = 1;
        for(/*Bitu*/int i = 0;i < 9;i++){
            in = Memory.mem_readb(addr + i);
            val += ( (in&0xf) * base); //in&0xf shouldn't be higher then 9
            base *= 10;
            val += ((( in>>4)&0xf) * base);
            base *= 10;
        }
        //last number, only now convert to float in order to get
        //the best signification
        /*Real64*/long temp = MicroDouble.longToDouble(val);
        in = Memory.mem_readb(addr + 9);
        temp = MicroDouble.add(temp, MicroDouble.longToDouble((in&0xf) * base));
        if((in&0x80)!=0) temp = MicroDouble.negate(temp);
        fpu.regs[store_to].d=temp;
    }


    static private void FPU_FLD_F32_EA(/*PhysPt*/int addr) {
        FPU_FLD_F32(addr,8);
    }
    static private void FPU_FLD_F64_EA(/*PhysPt*/int addr) {
        FPU_FLD_F64(addr,8);
    }
    static private void FPU_FLD_I32_EA(/*PhysPt*/int addr) {
        FPU_FLD_I32(addr,8);
    }
    static private void FPU_FLD_I16_EA(/*PhysPt*/int addr) {
        FPU_FLD_I16(addr,8);
    }

    static private void FPU_FST_F32(/*PhysPt*/int addr) {
        //should depend on rounding method
        Memory.mem_writed(addr,MicroDouble.floatValue(fpu.regs[fpu.top].d));
    }

    static private void FPU_FST_F64(/*PhysPt*/int addr) {
        Memory.mem_writed(addr,fpu.regs[fpu.top].l.lower());
        Memory.mem_writed(addr+4,fpu.regs[fpu.top].l.upper());
    }

    static private void FPU_FST_F80(/*PhysPt*/int addr) {
        FPU_ST80(addr,fpu.top);
    }

    static private void FPU_FST_I16(/*PhysPt*/int addr) {
        Memory.mem_writew(addr,MicroDouble.shortValue(FROUND(fpu.regs[fpu.top].d)));
    }

    static private void FPU_FST_I32(/*PhysPt*/int addr) {
        Memory.mem_writed(addr,MicroDouble.intValue(FROUND(fpu.regs[fpu.top].d)));
    }

    static private void FPU_FST_I64(/*PhysPt*/int addr) {
        FPU_Reg blah=new FPU_Reg();
        blah.ll(MicroDouble.longValue(FROUND(fpu.regs[fpu.top].d)));
        Memory.mem_writed(addr,blah.l.lower());
        Memory.mem_writed(addr+4,blah.l.upper());
    }

    static private void FPU_FBST(/*PhysPt*/int addr) {
        String val = String.valueOf(MicroDouble.longValue(fpu.regs[fpu.top].d));
        boolean sign = false;
        if (val.startsWith("-")) {
            sign = true;
            val = val.substring(1);
        }
        while (val.length()<19) {
            val = "0"+val;
        }
        for (int i=0;i<9;i++) {
            short p = (short)((val.charAt(i*2)-'0') | ((val.charAt(i*2+1)-'0') << 4));
            Memory.mem_writeb(addr+i,p);
        }
        short p = (short)(val.charAt(18)-'0');
        if (sign)
            p|=0x80;
        Memory.mem_writeb(addr+9,p);
    }

    static private void FPU_FADD(/*Bitu*/int op1, /*Bitu*/int op2){
        fpu.regs[op1].d=MicroDouble.add(fpu.regs[op1].d,fpu.regs[op2].d);
        //flags and such :)
    }

    static private void FPU_FSIN(){
        fpu.regs[fpu.top].d = MicroDouble.sin(fpu.regs[fpu.top].d);
        FPU_SET_C2(0);
        //flags and such :)
    }

    static private void FPU_FSINCOS(){
        /*Real64*/long temp = fpu.regs[fpu.top].d;
        fpu.regs[fpu.top].d = MicroDouble.sin(temp);
        FPU_PUSH(MicroDouble.cos(temp));
        FPU_SET_C2(0);
        //flags and such :)
    }

    static private void FPU_FCOS(){
        fpu.regs[fpu.top].d = MicroDouble.cos(fpu.regs[fpu.top].d);
        FPU_SET_C2(0);
        //flags and such :)
    }

    static private void FPU_FSQRT(){
        fpu.regs[fpu.top].d = MicroDouble.sqrt(fpu.regs[fpu.top].d);
        //flags and such :)
    }
    static private void FPU_FPATAN(){
        fpu.regs[STV(1)].d = MicroDouble.atan(MicroDouble.div(fpu.regs[STV(1)].d, fpu.regs[fpu.top].d));
        FPU_FPOP();
        //flags and such :)
    }
    static private void FPU_FPTAN(){
        fpu.regs[fpu.top].d = MicroDouble.tan(fpu.regs[fpu.top].d);
        FPU_PUSH(MicroDouble.ONE);
        FPU_SET_C2(0);
        //flags and such :)
    }
    static private void FPU_FDIV(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d= MicroDouble.div(fpu.regs[st].d,fpu.regs[other].d);
        //flags and such :)
    }

    static private void FPU_FDIVR(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d= MicroDouble.div(fpu.regs[other].d,fpu.regs[st].d);
        // flags and such :)
    }

    static private void FPU_FMUL(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d=MicroDouble.mul(fpu.regs[st].d,fpu.regs[other].d);
        //flags and such :)
    }

    static private void FPU_FSUB(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d = MicroDouble.sub(fpu.regs[st].d, fpu.regs[other].d);
        //flags and such :)
    }

    static private void FPU_FSUBR(/*Bitu*/int st, /*Bitu*/int other){
        fpu.regs[st].d= MicroDouble.sub(fpu.regs[other].d,  fpu.regs[st].d);
        //flags and such :)
    }

    static private void FPU_FXCH(/*Bitu*/int st, /*Bitu*/int other){
        int tag = fpu.tags[other];
        long reg = fpu.regs[other].d;
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
            ((fpu.tags[other] != TAG_Valid) && (fpu.tags[other] != TAG_Zero))){
            FPU_SET_C3(1);FPU_SET_C2(1);FPU_SET_C0(1);return;
        }
        if(MicroDouble.eq(fpu.regs[st].d, fpu.regs[other].d)){
            FPU_SET_C3(1);FPU_SET_C2(0);FPU_SET_C0(0);return;
        }
        if(MicroDouble.lt(fpu.regs[st].d, fpu.regs[other].d)){
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
        fpu.regs[fpu.top].d=FROUND(fpu.regs[fpu.top].d);
    }

    static private void FPU_FPREM(){
        /*Real64*/long valtop = fpu.regs[fpu.top].d;
        /*Real64*/long valdiv = fpu.regs[STV(1)].d;
        /*Bit64s*/long ressaved = MicroDouble.floor(MicroDouble.div(valtop, valdiv));
    // Some backups
    //	/*Real64*/double res=valtop - ressaved*valdiv;
    //      res= fmod(valtop,valdiv);
        fpu.regs[fpu.top].d = MicroDouble.sub(valtop, MicroDouble.mul(ressaved,valdiv));
        ressaved = MicroDouble.longValue(ressaved);
        FPU_SET_C0((int)(ressaved&4));
        FPU_SET_C3((int)(ressaved&2));
        FPU_SET_C1((int)(ressaved&1));
        FPU_SET_C2(0);
    }

    static private void FPU_FPREM1(){
        /*Real64*/long valtop = fpu.regs[fpu.top].d;
        /*Real64*/long valdiv = fpu.regs[STV(1)].d;
        long quot = MicroDouble.div(valtop,valdiv);
        long quotf = MicroDouble.floor(quot);
        /*Bit64s*/long ressaved;
        if (MicroDouble.gt(MicroDouble.sub(quot, quotf), MicroDouble.ONE_HALF)) ressaved = MicroDouble.add(quotf, MicroDouble.ONE);
        else if (MicroDouble.lt(MicroDouble.sub(quot,quotf), MicroDouble.ONE_HALF)) ressaved = quotf;
        else ressaved = ((MicroDouble.longValue(quotf)&1)!=0)?MicroDouble.add(quotf,MicroDouble.ONE):quotf;
        ressaved = MicroDouble.longValue(ressaved);
        fpu.regs[fpu.top].d = MicroDouble.sub(valtop, MicroDouble.mul(MicroDouble.longToDouble(ressaved),valdiv));
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
        if(MicroDouble.isZero(fpu.regs[fpu.top].d))		//zero or normalized number.
        {
            FPU_SET_C3(1);FPU_SET_C2(0);FPU_SET_C0(0);
        }
        else
        {
            FPU_SET_C3(0);FPU_SET_C2(1);FPU_SET_C0(0);
        }
    }


    static private void FPU_F2XM1(){
        fpu.regs[fpu.top].d = MicroDouble.sub(MicroDouble.pow(MicroDouble.TWO,fpu.regs[fpu.top].d), MicroDouble.ONE);
    }

    static private void FPU_FYL2X(){
        fpu.regs[STV(1)].d=MicroDouble.mul(fpu.regs[STV(1)].d, MicroDouble.div(MicroDouble.log(fpu.regs[fpu.top].d),MicroDouble.log(MicroDouble.TWO)));
        FPU_FPOP();
    }

    static private void FPU_FYL2XP1(){
        fpu.regs[STV(1)].d=MicroDouble.mul(fpu.regs[STV(1)].d, MicroDouble.div(MicroDouble.log(MicroDouble.add(fpu.regs[fpu.top].d, MicroDouble.ONE)), MicroDouble.log(MicroDouble.TWO)));
        FPU_FPOP();
    }

    static private void FPU_FSCALE(){
        fpu.regs[fpu.top].d = MicroDouble.mul(fpu.regs[fpu.top].d, MicroDouble.pow(MicroDouble.TWO, MicroDouble.truncate(fpu.regs[STV(1)].d)));
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
            fpu.regs[STV(i)].d = FPU_FLD80(addr+start);
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
        exp80final = MicroDouble.longToDouble(exp80final);
        /*Real64*/long mant = MicroDouble.div(test.d , MicroDouble.pow(MicroDouble.TWO, exp80final));
        fpu.regs[fpu.top].d = exp80final;
        FPU_PUSH(mant);
    }

    static private void FPU_FCHS(){
        fpu.regs[fpu.top].d = MicroDouble.negate(fpu.regs[fpu.top].d);
    }

    static private void FPU_FABS(){
        fpu.regs[fpu.top].d = MicroDouble.abs(fpu.regs[fpu.top].d);
    }

    static private void FPU_FTST(){
        fpu.regs[8].d = MicroDouble.ZERO;
        FPU_FCOM(fpu.top,8);
    }

    static private void FPU_FLD1(){
        FPU_PREP_PUSH();
        fpu.regs[fpu.top].d = MicroDouble.ONE;
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
        fpu.regs[fpu.top].d = MicroDouble.ZERO;
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
        /* REGULAR TREE WITH 32 BITS REALS */
        FPU_FLD_F32_EA(addr);
        EATREE(rm);
    }

    static public void FPU_ESC0_Normal(/*Bitu*/int rm) {
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch (group){
        case 0x00:		/* FADD ST,STi */
            FPU_FADD(fpu.top,STV(sub));
            break;
        case 0x01:		/* FMUL ST,STi */
            FPU_FMUL(fpu.top,STV(sub));
            break;
        case 0x02:		/* FCOM STi */
            FPU_FCOM(fpu.top,STV(sub));
            break;
        case 0x03:		/* FCOMP STi */
            FPU_FCOM(fpu.top,STV(sub));
            FPU_FPOP();
            break;
        case 0x04:		/* FSUB ST,STi */
            FPU_FSUB(fpu.top,STV(sub));
            break;
        case 0x05:		/* FSUBR ST,STi */
            FPU_FSUBR(fpu.top,STV(sub));
            break;
        case 0x06:		/* FDIV ST,STi */
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
// floats
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00: /* FLD float*/
            FPU_PREP_PUSH();
            FPU_FLD_F32(addr,fpu.top);
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
        /* 32 bits integer operants */
        FPU_FLD_I32_EA(addr);
        EATREE(rm);
    }

    static public void FPU_ESC2_Normal(/*Bitu*/int rm) {
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
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00:	/* FILD */
            FPU_PREP_PUSH();
            FPU_FLD_I32(addr,fpu.top);
            break;
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
            FPU_PREP_PUSH();
            FPU_FLD_F80(addr);
            break;
        case 0x07:	/* FSTP 80 Bits Real */
            FPU_FST_F80(addr);
            FPU_FPOP();
            break;
        default:
            if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_FPU,LogSeverities.LOG_WARN,"ESC 3 EA:Unhandled group "+group+" subfunction "+sub);
        }
    }

    static public void FPU_ESC3_Normal(/*Bitu*/int rm) {
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
        /* REGULAR TREE WITH 64 BITS REALS */
        FPU_FLD_F64_EA(addr);
        EATREE(rm);
    }

    static public void FPU_ESC4_Normal(/*Bitu*/int rm) {
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
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00:  /* FLD double real*/
            FPU_PREP_PUSH();
            FPU_FLD_F64(addr,fpu.top);
            break;
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
        /* 16 bit (word integer) operants */
        FPU_FLD_I16_EA(addr);
        EATREE(rm);
    }

    static public void FPU_ESC6_Normal(/*Bitu*/int rm) {
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
        /*Bitu*/int group=(rm >> 3) & 7;
        /*Bitu*/int sub=(rm & 7);
        switch(group){
        case 0x00:  /* FILD Bit16s */
            FPU_PREP_PUSH();
            FPU_FLD_I16(addr,fpu.top);
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
            FPU_PREP_PUSH();
            FPU_FBLD(addr,fpu.top);
            break;
        case 0x05:  /* FILD Bit64s */
            FPU_PREP_PUSH();
            FPU_FLD_I64(addr,fpu.top);
            break;
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
}
