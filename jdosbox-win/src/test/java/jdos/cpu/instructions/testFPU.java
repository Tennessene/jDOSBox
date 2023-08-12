package jdos.cpu.instructions;

import jdos.cpu.CPU_Regs;
import jdos.fpu.FPU;
import jdos.hardware.Memory;

public class testFPU extends InstructionsTestCase {
    protected void setUp() throws java.lang.Exception  {
        super.setUp();
        //FPU.FPU_Init.call(null);
        init();
    }

    private void init() {
        newInstruction(0xdb);
        pushIb(rm(false, 4, 3));
        decoder.call();
    }
    private void fldf32(float f) {
        newInstruction(0xd9);
        pushIb(rm(true, 0, 0));
        writeF(f);
        decoder.call();
    }

    private float getTopFloat() {
        newInstruction(0xd9);
        pushIb(rm(true, 2, 0));
        decoder.call();
        return Float.intBitsToFloat(Memory.mem_readd(MEM_BASE_DS));
    }

    private void writeF(float f) {
        Memory.mem_writed(MEM_BASE_DS, Float.floatToRawIntBits(f));
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 4, 0xCDCDCDCD);
    }
    private void writeD(long f) {
        Memory.mem_writed(MEM_BASE_DS, (int)f);
        Memory.mem_writed(MEM_BASE_DS+4, (int)(f>>>32));
        Memory.mem_writed(MEM_BASE_DS - 4, 0xCDCDCDCD);
        Memory.mem_writed(MEM_BASE_DS + 8, 0xCDCDCDCD);
    }
    private byte rm(boolean ea, int group, int sub) {
        int result = (group & 7) << 3 | (sub & 7);
        if (!ea)
            result |= 0xC0;
        return (byte)result;
    }

    private int getStackPos() {
        newInstruction(0xdf);
        pushIb(rm(false, 4, 0));
        decoder.call();
        return (CPU_Regs.reg_eax.word() & 0x3800) >> 11;

    }
    private void doF32Instruction(int op1, int group1, int op2, int group2, float x, float y, float r) {
        init();

        fldf32(x);
        writeF(y);

        newInstruction(op1);
        pushIb(rm(true, group1, 0));
        pushId(MEM_BASE_DS);
        decoder.call();
        float result = getTopFloat();
        assertTrue((Float.isNaN(result) && Float.isNaN(r)) || result == r);
        assertTrue(getStackPos()==7); // nothing was popped

        init();

        fldf32(y);
        fldf32(x);
        newInstruction(op2);
        pushIb(rm(false, group2, 1));
        decoder.call();
        result = getTopFloat();
        assertTrue((Float.isNaN(result) && Float.isNaN(r)) || result == r);
        assertTrue(getStackPos()==6); // nothing was popped
    }

    private void F32Add(float x, float y, float r) {
        doF32Instruction(0xd8, 0, 0xd8, 0, x, y, r);
    }

    private void doF32Add() {
        F32Add(0.0f, 0.0f, 0.0f);
        F32Add(-0.0f, 0.0f, 0.0f);
        F32Add(0.0f, -0.0f, 0.0f);

        F32Add(0.0f, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        F32Add(Float.POSITIVE_INFINITY, 0.0f, Float.POSITIVE_INFINITY);
        F32Add(0.0f, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        F32Add(Float.NEGATIVE_INFINITY, 0.0f, Float.NEGATIVE_INFINITY);
        F32Add(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN);
        F32Add(Float.POSITIVE_INFINITY, 1.0f, Float.POSITIVE_INFINITY);
        F32Add(Float.POSITIVE_INFINITY, 2.0f, Float.POSITIVE_INFINITY);
        F32Add(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        F32Add(Float.NaN, 2.0f, Float.NaN);
        F32Add(Float.NaN, Float.NaN, Float.NaN);
        F32Add(-2.0f, Float.NaN, Float.NaN);

        F32Add(0.0f, 1.0f, 1.0f);
        F32Add(1.0f, 0.0f, 1.0f);
        F32Add(0.0f, -1.0f, -1.0f);
        F32Add(-1.0f, 0.0f, -1.0f);
        F32Add(-1.0f, 1.0f, 0.0f);
        F32Add(1.0f, -1.0f, 0.0f);
        F32Add(-1.0f, -1.0f, -2.0f);
        F32Add(1.0f, 1.0f, 2.0f);

        F32Add(100.01f, 0.001f, 100.011f);
    }

    public void testF32Add() {
        FPU.softFPU = false;
        doF32Add();
    }

    public void testF32AddSoft() {
        FPU.softFPU = true;
        doF32Add();
    }

    private void F32Sub(float x, float y, float r) {
        doF32Instruction(0xd8, 4, 0xd8, 4, x, y, r);
    }

    private void doF32Sub() {
        F32Sub(0.0f, 0.0f, 0.0f);
        F32Sub(-0.0f, 0.0f, 0.0f);
        F32Sub(0.0f, -0.0f, 0.0f);

        F32Sub(0.0f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
        F32Sub(Float.POSITIVE_INFINITY, 0.0f, Float.POSITIVE_INFINITY);
        F32Sub(0.0f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        F32Sub(Float.NEGATIVE_INFINITY, 0.0f, Float.NEGATIVE_INFINITY);
        F32Sub(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
        F32Sub(Float.POSITIVE_INFINITY, 1.0f, Float.POSITIVE_INFINITY);
        F32Sub(Float.POSITIVE_INFINITY, 2.0f, Float.POSITIVE_INFINITY);
        F32Sub(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN);

        F32Sub(Float.NaN, 2.0f, Float.NaN);
        F32Sub(Float.NaN, Float.NaN, Float.NaN);
        F32Sub(-2.0f, Float.NaN, Float.NaN);

        F32Sub(0.0f, 1.0f, -1.0f);
        F32Sub(1.0f, 0.0f, 1.0f);
        F32Sub(0.0f, -1.0f, 1.0f);
        F32Sub(-1.0f, 0.0f, -1.0f);
        F32Sub(-1.0f, 1.0f, -2.0f);
        F32Sub(1.0f, -1.0f, 2.0f);
        F32Sub(-1.0f, -1.0f, 0.0f);
        F32Sub(1.0f, 1.0f, 0.0f);

        F32Sub(100.01f, 0.001f, 100.009f);
    }

    public void testF32Sub() {
        FPU.softFPU = false;
        doF32Sub();
    }

    public void testF32SubSoft() {
        FPU.softFPU = true;
        doF32Sub();
    }

    private void F32SubR(float x, float y, float r) {
        doF32Instruction(0xd8, 5, 0xd8, 5, x, y, r);
    }

    private void doF32SubR() {
        F32SubR(0.0f, 0.0f, 0.0f);
        F32SubR(-0.0f, 0.0f, 0.0f);
        F32SubR(0.0f, -0.0f, 0.0f);

        F32SubR(0.0f, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        F32SubR(Float.POSITIVE_INFINITY, 0.0f, Float.NEGATIVE_INFINITY);
        F32SubR(0.0f, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        F32SubR(Float.NEGATIVE_INFINITY, 0.0f, Float.POSITIVE_INFINITY);
        F32SubR(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        F32SubR(Float.POSITIVE_INFINITY, 1.0f, Float.NEGATIVE_INFINITY);
        F32SubR(Float.POSITIVE_INFINITY, 2.0f, Float.NEGATIVE_INFINITY);
        F32SubR(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN);

        F32SubR(Float.NaN, 2.0f, Float.NaN);
        F32SubR(Float.NaN, Float.NaN, Float.NaN);
        F32SubR(-2.0f, Float.NaN, Float.NaN);

        F32SubR(0.0f, 1.0f, 1.0f);
        F32SubR(1.0f, 0.0f, -1.0f);
        F32SubR(0.0f, -1.0f, -1.0f);
        F32SubR(-1.0f, 0.0f, 1.0f);
        F32SubR(-1.0f, 1.0f, 2.0f);
        F32SubR(1.0f, -1.0f, -2.0f);
        F32SubR(-1.0f, -1.0f, 0.0f);
        F32SubR(1.0f, 1.0f, 0.0f);

        F32SubR(100.01f, 0.001f, -100.009f);
    }

    public void testF32SubR() {
        FPU.softFPU = false;
        doF32SubR();
    }

    public void testF32SubSoftR() {
        FPU.softFPU = true;
        doF32SubR();
    }

    private void F32Mul(float x, float y, float r) {
        doF32Instruction(0xd8, 1, 0xd8, 1, x, y, r);
    }

    private void doF32Mul() {
        F32Mul(0.0f, 0.0f, 0.0f);
        F32Mul(-0.0f, 0.0f, 0.0f);
        F32Mul(0.0f, -0.0f, 0.0f);

        F32Mul(0.0f, Float.POSITIVE_INFINITY, Float.NaN);
        F32Mul(Float.POSITIVE_INFINITY, 0.0f, Float.NaN);
        F32Mul(0.0f, Float.NEGATIVE_INFINITY, Float.NaN);
        F32Mul(Float.NEGATIVE_INFINITY, 0.0f, Float.NaN);
        F32Mul(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
        F32Mul(Float.POSITIVE_INFINITY, 1.0f, Float.POSITIVE_INFINITY);
        F32Mul(Float.POSITIVE_INFINITY, 2.0f, Float.POSITIVE_INFINITY);
        F32Mul(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        F32Mul(Float.NaN, 2.0f, Float.NaN);
        F32Mul(Float.NaN, Float.NaN, Float.NaN);
        F32Mul(-2.0f, Float.NaN, Float.NaN);

        F32Mul(0.0f, 1.0f, 0.0f);
        F32Mul(1.0f, 0.0f, 0.0f);
        F32Mul(0.0f, -1.0f, 0.0f);
        F32Mul(-1.0f, 0.0f, 0.0f);
        F32Mul(-1.0f, 1.0f, -1.0f);
        F32Mul(1.0f, -1.0f, -1.0f);
        F32Mul(-1.0f, -1.0f, 1.0f);
        F32Mul(1.0f, 1.0f, 1.0f);

        F32Mul(100.01f, 0.001f, .10001001f);
    }

    public void testF32Mul() {
        FPU.softFPU = false;
        doF32Mul();
    }

    public void testF32MulSoft() {
        FPU.softFPU = true;
        doF32Mul();
    }

    private void F32Div(float x, float y, float r) {
        doF32Instruction(0xd8, 6, 0xd8, 6, x, y, r);
    }

    private void doF32Div() {
        F32Div(0.0f, 0.0f, Float.NaN);
        F32Div(-0.0f, 0.0f, Float.NaN);
        F32Div(0.0f, -0.0f, Float.NaN);

        F32Div(0.0f, Float.POSITIVE_INFINITY, 0.0f);
        F32Div(Float.POSITIVE_INFINITY, 0.0f, Float.POSITIVE_INFINITY);
        F32Div(0.0f, Float.NEGATIVE_INFINITY, -0.0f);
        F32Div(Float.NEGATIVE_INFINITY, 0.0f, Float.NEGATIVE_INFINITY);
        F32Div(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN);
        F32Div(Float.POSITIVE_INFINITY, 1.0f, Float.POSITIVE_INFINITY);
        F32Div(Float.POSITIVE_INFINITY, 2.0f, Float.POSITIVE_INFINITY);
        F32Div(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN);

        F32Div(Float.NaN, 2.0f, Float.NaN);
        F32Div(Float.NaN, Float.NaN, Float.NaN);
        F32Div(-2.0f, Float.NaN, Float.NaN);

        F32Div(0.0f, 1.0f, 0.0f);
        F32Div(1.0f, 0.0f, Float.POSITIVE_INFINITY);
        F32Div(0.0f, -1.0f, 0.0f);
        F32Div(-1.0f, 0.0f, Float.NEGATIVE_INFINITY);
        F32Div(-1.0f, 1.0f, -1.0f);
        F32Div(1.0f, -1.0f, -1.0f);
        F32Div(-1.0f, -1.0f, 1.0f);
        F32Div(1.0f, 1.0f, 1.0f);

        F32Div(100.01f, 0.001f, 100010.0f);
    }

    public void testF32Div() {
        FPU.softFPU = false;
        doF32Div();
    }

    public void testF32DivSoft() {
        FPU.softFPU = true;
        doF32Div();
    }

    private void F32DivR(float x, float y, float r) {
        doF32Instruction(0xd8, 7, 0xd8, 7, x, y, r);
    }

    private void doF32DivR() {
        F32DivR(0.0f, 0.0f, Float.NaN);
        F32DivR(-0.0f, 0.0f, Float.NaN);
        F32DivR(0.0f, -0.0f, Float.NaN);

        F32DivR(0.0f, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        F32DivR(Float.POSITIVE_INFINITY, 0.0f, 0.0f);
        F32DivR(0.0f, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        F32DivR(Float.NEGATIVE_INFINITY, 0.0f, -0.0f);
        F32DivR(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN);
        F32DivR(Float.POSITIVE_INFINITY, 1.0f, 0.0f);
        F32DivR(Float.POSITIVE_INFINITY, 2.0f, 0.0f);
        F32DivR(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN);

        F32DivR(Float.NaN, 2.0f, Float.NaN);
        F32DivR(Float.NaN, Float.NaN, Float.NaN);
        F32DivR(-2.0f, Float.NaN, Float.NaN);

        F32DivR(0.0f, 1.0f, Float.POSITIVE_INFINITY);
        F32DivR(1.0f, 0.0f, 0.0f);
        F32DivR(0.0f, -1.0f, Float.NEGATIVE_INFINITY);
        F32DivR(-1.0f, 0.0f, 0.0f);
        F32DivR(-1.0f, 1.0f, -1.0f);
        F32DivR(1.0f, -1.0f, -1.0f);
        F32DivR(-1.0f, -1.0f, 1.0f);
        F32DivR(1.0f, 1.0f, 1.0f);

        F32DivR(100.01f, 0.001f, .000009999f);
    }

    public void testF32DivR() {
        FPU.softFPU = false;
        doF32DivR();
    }

    public void testF32DivRSoft() {
        FPU.softFPU = true;
        doF32DivR();
    }

    static private final int UNORDERED = 0x100 | 0x400 | 0x4000;
    static private final int LESS = 0x100;
    static private final int GREATER = 0x0;
    static private final int EQUAL = 0x4000;
    static private final int MASK = 0x100 | 0x200 | 0x400 | 0x4000;

    private void assertTest(int r) {
        newInstruction(0xdf);
        pushIb(rm(false, 4, 0));
        decoder.call();
        assertTrue((CPU_Regs.reg_eax.word() & MASK) == r);
    }

    private void F32ComBase(int op, int group, float x, float y, int r, int popCount) {
        init();

        fldf32(x);
        writeF(y);

        newInstruction(op);
        pushIb(rm(true, group, 0));
        pushId(MEM_BASE_DS);
        decoder.call();
        assertTest(r);
        assertTrue(getStackPos()==((7+popCount)&7));

        init();

        fldf32(y);
        fldf32(x);
        newInstruction(op);
        pushIb(rm(false, group, 1));
        decoder.call();
        assertTest(r);
        assertTrue(getStackPos()==((6+popCount)&7));
    }
    private void F32Com(float x, float y, int r) {
        F32ComBase(0xd8, 2, x, y, r, 0);
    }
    private void doF32Com() {
        F32Com(0.0f, 0.0f, EQUAL);
        F32Com(-0.0f, 0.0f, EQUAL);
        F32Com(0.0f, -0.0f, EQUAL);

        F32Com(0.0f, Float.POSITIVE_INFINITY, LESS);
        F32Com(Float.POSITIVE_INFINITY, 0.0f, GREATER);
        F32Com(0.0f, Float.NEGATIVE_INFINITY, GREATER);
        F32Com(Float.NEGATIVE_INFINITY, 0.0f, LESS);
        F32Com(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, LESS);
        F32Com(Float.POSITIVE_INFINITY, 1.0f, GREATER);
        F32Com(Float.POSITIVE_INFINITY, 2.0f, GREATER);
        F32Com(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, EQUAL);

        F32Com(Float.NaN, 2.0f, UNORDERED);
        F32Com(Float.NaN, Float.NaN, UNORDERED);
        F32Com(-2.0f, Float.NaN, UNORDERED);

        F32Com(0.0f, 1.0f, LESS);
        F32Com(1.0f, 0.0f, GREATER);
        F32Com(0.0f, -1.0f, GREATER);
        F32Com(-1.0f, 0.0f, LESS);
        F32Com(-1.0f, 1.0f, LESS);
        F32Com(1.0f, -1.0f, GREATER);
        F32Com(-1.0f, -1.0f, EQUAL);
        F32Com(1.0f, 1.0f, EQUAL);

        F32Com(100.01f, 0.001f, GREATER);
    }

    public void testF32Com() {
        FPU.softFPU = false;
        doF32Com();
    }

    public void testF32ComSoft() {
        FPU.softFPU = true;
        doF32Com();
    }

    private void F32ComP(float x, float y, int r) {
        F32ComBase(0xd8, 3, x, y, r, 1);
    }

    private void doF32ComP() {
        F32ComP(0.0f, 0.0f, EQUAL);
        F32ComP(-0.0f, 0.0f, EQUAL);
        F32ComP(0.0f, -0.0f, EQUAL);

        F32ComP(0.0f, Float.POSITIVE_INFINITY, LESS);
        F32ComP(Float.POSITIVE_INFINITY, 0.0f, GREATER);
        F32ComP(0.0f, Float.NEGATIVE_INFINITY, GREATER);
        F32ComP(Float.NEGATIVE_INFINITY, 0.0f, LESS);
        F32ComP(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, LESS);
        F32ComP(Float.POSITIVE_INFINITY, 1.0f, GREATER);
        F32ComP(Float.POSITIVE_INFINITY, 2.0f, GREATER);
        F32ComP(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, EQUAL);

        F32ComP(Float.NaN, 2.0f, UNORDERED);
        F32ComP(Float.NaN, Float.NaN, UNORDERED);
        F32ComP(-2.0f, Float.NaN, UNORDERED);

        F32ComP(0.0f, 1.0f, LESS);
        F32ComP(1.0f, 0.0f, GREATER);
        F32ComP(0.0f, -1.0f, GREATER);
        F32ComP(-1.0f, 0.0f, LESS);
        F32ComP(-1.0f, 1.0f, LESS);
        F32ComP(1.0f, -1.0f, GREATER);
        F32ComP(-1.0f, -1.0f, EQUAL);
        F32ComP(1.0f, 1.0f, EQUAL);

        F32ComP(100.01f, 0.001f, GREATER);
    }

    public void testF32ComP() {
        FPU.softFPU = false;
        doF32ComP();
    }

    public void testF32ComSoftP() {
        FPU.softFPU = true;
        doF32ComP();
    }

    public void FSTFloat(int op, int group, float f, boolean pop) {
        init();

        fldf32(f);
        Memory.mem_writed(MEM_BASE_DS, 0xCDCDCDCD);

        newInstruction(op);
        pushIb(rm(true, group, 0));
        decoder.call();
        float result = Float.intBitsToFloat(Memory.mem_readd(MEM_BASE_DS));
        assertTrue(result==f || (Float.isNaN(result) && Float.isNaN(f)));
        assertTrue(getStackPos()==(pop?0:7));
    }
    public void doFSTFloat(int op, int group, boolean pop) {
        FSTFloat(op, group, 0.0f, pop);
        FSTFloat(op, group, 1.0f, pop);
        FSTFloat(op, group, -1.0f, pop);
        FSTFloat(op, group, 0.00001f, pop);
        FSTFloat(op, group, -0.00001f, pop);
        FSTFloat(op, group, 1010.01f, pop);
        FSTFloat(op, group, -1010.01f, pop);
        FSTFloat(op, group, Float.NaN, pop);
        FSTFloat(op, group, Float.POSITIVE_INFINITY, pop);
        FSTFloat(op, group, Float.NEGATIVE_INFINITY, pop);
    }

    public void testFSTFloat() {
        FPU.softFPU = false;
        doFSTFloat(0xd9, 2, false);
    }

    public void testFSTFloatSoft() {
        FPU.softFPU = true;
        doFSTFloat(0xd9, 2, false);
    }

    public void testFSTPFloat() {
        FPU.softFPU = false;
        doFSTFloat(0xd9, 3, true);
    }

    public void testFSTPFloatSoft() {
        FPU.softFPU = true;
        doFSTFloat(0xd9, 3, true);
    }

    public void doFLDSti() {
        fldf32(4.0f);
        fldf32(3.0f);
        fldf32(2.0f);
        fldf32(1.0f);

        newInstruction(0xd9);
        pushIb(rm(false, 0, 0));
        decoder.call();
        assertTrue(getTopFloat()==1.0f);

        newInstruction(0xd9);
        pushIb(rm(false, 0, 2));
        decoder.call();
        assertTrue(getTopFloat()==2.0f);

        newInstruction(0xd9);
        pushIb(rm(false, 0, 4));
        decoder.call();
        assertTrue(getTopFloat()==3.0f);

        newInstruction(0xd9);
        pushIb(rm(false, 0, 6));
        decoder.call();
        assertTrue(getTopFloat()==4.0f);
    }

    public void testFLDSTi() {
        FPU.softFPU = false;
        doFLDSti();
    }

    public void testFLDSTiSoft() {
        FPU.softFPU = true;
        doFLDSti();
    }

    public void doFXCHSTi() {
        fldf32(4.0f);
        fldf32(3.0f);
        fldf32(2.0f);
        fldf32(1.0f);

        newInstruction(0xd9);
        pushIb(rm(false, 1, 3));
        decoder.call();
        assertTrue(getTopFloat()==4.0f);
    }

    public void testFXCHSTi() {
        FPU.softFPU = false;
        doFXCHSTi();
    }

    public void testFXCHSTiSoft() {
        FPU.softFPU = true;
        doFXCHSTi();
    }

    public void doFSTPSTi() {
        fldf32(4.0f);
        fldf32(3.0f);
        fldf32(2.0f);
        fldf32(1.0f);

        newInstruction(0xd9);
        pushIb(rm(false, 3, 2));
        decoder.call();
        assertTrue(getTopFloat()==2.0f);

        newInstruction(0xd9);
        pushIb(rm(false, 3, 2));
        decoder.call();
        assertTrue(getTopFloat()==1.0f);
    }

    public void testFSTPSTi() {
        FPU.softFPU = false;
        doFSTPSTi();
    }

    public void testFSTPSTiSoft() {
        FPU.softFPU = false;
        doFSTPSTi();
    }

    public void doFCHS() {
        fldf32(432.1f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 0));
        decoder.call();
        assertTrue(getTopFloat()==-432.1f);

        fldf32(-0.001234f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 0));
        decoder.call();
        assertTrue(getTopFloat()==0.001234f);

        fldf32(Float.NaN);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 0));
        decoder.call();
        assertTrue(Float.isNaN(getTopFloat()));

        fldf32(Float.POSITIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 0));
        decoder.call();
        assertTrue(getTopFloat()==Float.NEGATIVE_INFINITY);

        fldf32(Float.NEGATIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 0));
        decoder.call();
        assertTrue(getTopFloat()==Float.POSITIVE_INFINITY);
    }

    public void testFCHS() {
        FPU.softFPU = false;
        doFCHS();
    }

    public void testFCHSSoft() {
        FPU.softFPU = false;
        doFCHS();
    }

    public void doFABS() {
        fldf32(432.1f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 1));
        decoder.call();
        assertTrue(getTopFloat()==432.1f);

        fldf32(-0.001234f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 1));
        decoder.call();
        assertTrue(getTopFloat()==0.001234f);

        fldf32(Float.NaN);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 1));
        decoder.call();
        assertTrue(Float.isNaN(getTopFloat()));

        fldf32(Float.POSITIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 1));
        decoder.call();
        assertTrue(getTopFloat()==Float.POSITIVE_INFINITY);

        fldf32(Float.NEGATIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 1));
        decoder.call();
        assertTrue(getTopFloat()==Float.POSITIVE_INFINITY);
    }

    public void testFABS() {
        FPU.softFPU = false;
        doFABS();
    }

    public void testFABSSoft() {
        FPU.softFPU = false;
        doFABS();
    }

    public void doFTST() {
        fldf32(432.1f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 4));
        decoder.call();
        assertTest(GREATER);

        fldf32(-0.00001f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 4));
        decoder.call();
        assertTest(LESS);

        fldf32(0.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 4));
        decoder.call();
        assertTest(EQUAL);

        fldf32(Float.POSITIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 4));
        decoder.call();
        assertTest(GREATER);

        fldf32(Float.NEGATIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 4));
        decoder.call();
        assertTest(LESS);

        fldf32(Float.NaN);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 4));
        decoder.call();
        assertTest(UNORDERED);
    }

    public void testFTST() {
        FPU.softFPU = false;
        doFTST();
    }

    public void testFTSTSoft() {
        FPU.softFPU = true;
        doFTST();
    }

    public void doFXAM() {
        fldf32(0.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTest(0x4000);

        fldf32(Float.NaN);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTest(0x100);

        fldf32(Float.POSITIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTest(0x100 | 0x400);

        fldf32(Float.NEGATIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTest(0x100 | 0x200 | 0x400);

        fldf32(1.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTest(0x400);

        fldf32(-2.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 4, 5));
        decoder.call();
        assertTest(0x200 | 0x400);
    }

    public void testFXAM() {
        FPU.softFPU = false;
        doFXAM();
    }

    public void testFXAMSoft() {
        FPU.softFPU = true;
        doFXAM();
    }

    public void doFLD1() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 0));
        decoder.call();
        assertTrue(getTopFloat() == 1.0f);
    }

    public void testFLD1() {
        FPU.softFPU = false;
        doFLD1();
    }

    public void testFLD1Soft() {
        FPU.softFPU = true;
        doFLD1();
    }

    public void doFLDL2T() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 1));
        decoder.call();
        assertTrue(getTopFloat() == 3.321928f);
    }

    public void testFLDL2T() {
        FPU.softFPU = false;
        doFLDL2T();
    }

    public void testFLDL2TSoft() {
        FPU.softFPU = true;
        doFLDL2T();
    }

    public void doFLDL2E() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 2));
        decoder.call();
        assertTrue(getTopFloat() == 1.442695f);
    }

    public void testFLDL2E() {
        FPU.softFPU = false;
        doFLDL2E();
    }

    public void testFLDL2ESoft() {
        FPU.softFPU = true;
        doFLDL2E();
    }

    public void doFLDPI() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 3));
        decoder.call();
        assertTrue(getTopFloat() == 3.1415927f);
    }

    public void testFLDPI() {
        FPU.softFPU = false;
        doFLDPI();
    }

    public void testFLDPISoft() {
        FPU.softFPU = true;
        doFLDPI();
    }

    public void doFLDLG2() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 4));
        decoder.call();
        assertTrue(getTopFloat() == .30103f);
    }

    public void testFLDLG2() {
        FPU.softFPU = false;
        doFLDLG2();
    }

    public void testFLDLG2Soft() {
        FPU.softFPU = true;
        doFLDLG2();
    }

    public void doFLDLN2() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 5));
        decoder.call();
        assertTrue(getTopFloat() == 0.6931472f);
    }

    public void testFLDLN2() {
        FPU.softFPU = false;
        doFLDLN2();
    }

    public void testFLDLN2Soft() {
        FPU.softFPU = true;
        doFLDLN2();
    }

    public void doFLDZ() {
        newInstruction(0xd9);
        pushIb(rm(false, 5, 6));
        decoder.call();
        assertTrue(getTopFloat() == 0f);
    }

    public void testFLDZ() {
        FPU.softFPU = false;
        doFLDZ();
    }

    public void testFLDZSoft() {
        FPU.softFPU = true;
        doFLDZ();
    }

    public void doF2XM1() {
        fldf32(0.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 0));
        decoder.call();
        assertTrue(getTopFloat() == 0.0f);

        fldf32(Float.NaN);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 0));
        decoder.call();
        assertTrue(Float.isNaN(getTopFloat()));

        fldf32(Float.POSITIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 0));
        decoder.call();
        assertTrue(getTopFloat() == Float.POSITIVE_INFINITY);

        fldf32(Float.NEGATIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 0));
        decoder.call();
        assertTrue(getTopFloat() == -1.0f);

        fldf32(-1.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 0));
        decoder.call();
        assertTrue(getTopFloat() == -0.5f);

        fldf32(1.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 0));
        decoder.call();
        assertTrue(getTopFloat() == 1.0f);

        fldf32(-0.5f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 0));
        decoder.call();
        assertTrue(getTopFloat() == -0.29289323f);
    }

    public void testF2XM1() {
        FPU.softFPU = false;
        doF2XM1();
    }

    public void testF2XM1Soft() {
        FPU.softFPU = true;
        doF2XM1();
    }

    public void doFYL2X() {
        fldf32(1.0f);
        fldf32(0.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(getTopFloat() == Float.NEGATIVE_INFINITY);

        init();
        fldf32(2.0f);
        fldf32(1.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(getTopFloat() == 0.0f);

        init();
        fldf32(8.0f);
        fldf32(2.5f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(getTopFloat() == 10.575425f);

        init();
        fldf32(8.0f);
        fldf32(2.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(getTopFloat() == 8.0f);

        init();
        fldf32(8.0f);
        fldf32(-2.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(Float.isNaN(getTopFloat()));

        init();
        fldf32(10.0f);
        fldf32(8.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(getTopFloat() == 30.0f);

        init();
        fldf32(10.0f);
        fldf32(Float.NaN);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(Float.isNaN(getTopFloat()));

        init();
        fldf32(10.0f);
        fldf32(Float.POSITIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(getTopFloat() == Float.POSITIVE_INFINITY);

        init();
        fldf32(10.0f);
        fldf32(Float.NEGATIVE_INFINITY);
        newInstruction(0xd9);
        pushIb(rm(false, 6, 1));
        decoder.call();
        assertTrue(Float.isNaN(getTopFloat()));
    }

    public void testFYL2X() {
        FPU.softFPU = false;
        doFYL2X();
    }

    public void testFYL2XSoft() {
        FPU.softFPU = true;
        doFYL2X();
    }

    public void doFSQRT() {
        init();
        fldf32(0.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 2));
        decoder.call();
        assertTrue(getTopFloat() == 0.0f);

        init();
        fldf32(1.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 2));
        decoder.call();
        assertTrue(getTopFloat() == 1.0f);

        init();
        fldf32(2.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 2));
        decoder.call();
        assertTrue(getTopFloat() == 1.4142135f);

        init();
        fldf32(4.0f);
        newInstruction(0xd9);
        pushIb(rm(false, 7, 2));
        decoder.call();
        assertTrue(getTopFloat() == 2.0f);
    }

    public void testFSQRT() {
        FPU.softFPU = false;
        doFSQRT();
    }

    public void testFSQRTSoft() {
        FPU.softFPU = true;
        doFSQRT();
    }

}
