package jdos.win.builtin.directx.dsound;

import jdos.util.IntRef;
import jdos.util.Ptr;
import jdos.util.ShortPtr;
import jdos.win.builtin.WinAPI;

public class DSConvert extends WinAPI {
    private static short le16(int v) {
        return (short)((v & 0xFF << 8) | ((v >> 8) & 0xFF));
    }

    static private void src_advance(IntRef src, int stride, IntRef count, IntRef freqAcc, int adj) {
        freqAcc.value += adj;
        if (freqAcc.value >= (1 << DSOUND_FREQSHIFT))
        {
            long adv = (freqAcc.value >>> DSOUND_FREQSHIFT);
            freqAcc.value &= (1 << DSOUND_FREQSHIFT) - 1;
            src.value += adv * stride;
            count.value -= adv;
        }
    }

    public static interface bitsconvertfunc {
        public void call(int src, Ptr dst, int src_stride, int dst_stride, int count, int freqAcc, int adj);
    }

    static private bitsconvertfunc convert_8_to_16 = new bitsconvertfunc() {
        public void call(int s, Ptr dst, int src_stride, int dst_stride, int c, int f, int adj) {
            IntRef count = new IntRef(c);
            IntRef src = new IntRef(s);
            IntRef freqAcc = new IntRef(f);
            ShortPtr dest16 = new ShortPtr(dst);
            while (count.value > 0)
            {
                dest16.set(0, readb(src.value) * 257 - 32768);
                dest16.inc(dst_stride>>1);
                src_advance(src, src_stride, count, freqAcc, adj);
            }
        }
    };

    static private bitsconvertfunc convert_16_to_16 = new bitsconvertfunc() {
        public void call(int s, Ptr dst, int src_stride, int dst_stride, int c, int f, int adj) {
            IntRef count = new IntRef(c);
            IntRef src = new IntRef(s);
            IntRef freqAcc = new IntRef(f);
            ShortPtr dest = new ShortPtr(dst);
            while (count.value > 0)
            {
                dest.set(readw(src.value));
                dest.inc(dst_stride>>1);
                src_advance(src, src_stride, count, freqAcc, adj);
            }
        }
    };

    static private bitsconvertfunc convert_24_to_16 = new bitsconvertfunc() {
        public void call(int s, Ptr dst, int src_stride, int dst_stride, int c, int f, int adj) {
            IntRef count = new IntRef(c);
            IntRef src = new IntRef(s);
            IntRef freqAcc = new IntRef(f);
            ShortPtr dest16 = new ShortPtr(dst);

            while (count.value > 0)
            {
                dest16.set(0, readw(src.value+1));
                dest16.inc(dst_stride>>1);
                src_advance(src, src_stride, count, freqAcc, adj);
            }
        }
    };

    static private bitsconvertfunc convert_32_to_16 = new bitsconvertfunc() {
        public void call(int s, Ptr dst, int src_stride, int dst_stride, int c, int f, int adj) {
            IntRef count = new IntRef(c);
            IntRef src = new IntRef(s);
            IntRef freqAcc = new IntRef(f);
            ShortPtr dest16 = new ShortPtr(dst);

            while (count.value > 0)
            {
                dest16.set(0, readw(src.value+2));
                dest16.inc(dst_stride>>1);
                src_advance(src, src_stride, count, freqAcc, adj);
            }
        }
    };

    static private bitsconvertfunc convert_ieee_32_to_16 = new bitsconvertfunc() {
        public void call(int s, Ptr dst, int src_stride, int dst_stride, int c, int f, int adj) {
            IntRef count = new IntRef(c);
            IntRef src = new IntRef(s);
            IntRef freqAcc = new IntRef(f);
            ShortPtr dest16 = new ShortPtr(dst);

            while (count.value > 0)
            {
                float v = Float.intBitsToFloat(readd(src.value)); // :TODO: is the endianness correct
                warn("Need to verify correct endianness when converting floating point wave to 16-bit");
                if (v < -1.0f)
                    dest16.set(-32768);
                else if (v >  1.0f)
                    dest16.set(32767);
                else
                    dest16.set((short)(v * 32767.5f - 0.5f));
                dest16.inc(dst_stride>>1);
                src_advance(src, src_stride, count, freqAcc, adj);
            }
        }
    };

    public static final bitsconvertfunc[][] convertbpp = new bitsconvertfunc[][] {
    { null, convert_8_to_16, null, null },
    { null, convert_16_to_16, null, null },
    { null, convert_24_to_16, null, null },
    { null, convert_32_to_16, null, null },
    { null, convert_ieee_32_to_16, null, null },
};
}
