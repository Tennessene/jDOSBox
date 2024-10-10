package jdos.win.builtin.directx.dsound;

import jdos.util.IntRef;
import jdos.util.Ptr;
import jdos.util.ShortPtr;
import jdos.win.builtin.directx.Guid;
import jdos.win.builtin.winmm.WAVEFORMATEX;
import jdos.win.builtin.winmm.WAVEFORMATEXTENSIBLE;

import java.util.Arrays;

public class DSMixer extends IDirectSoundBuffer {
    public final static int DEVICE_SAMPLE_RATE = 44100;
    public final static int DEVICE_CHANNELS = 2;
    public final static int DEVICE_BITS_PER_SAMEPLE = 16;
    public final static int DEVICE_BLOCK_ALIGN = DEVICE_CHANNELS * DEVICE_BITS_PER_SAMEPLE / 8;


    final static Guid KSDATAFORMAT_SUBTYPE_PCM = new Guid(0x00000001, 0x0000, 0x0010, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);
    final static Guid KSDATAFORMAT_SUBTYPE_IEEE_FLOAT = new Guid(0x00000003, 0x0000, 0x0010, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);
    final static Guid KSDATAFORMAT_SUBTYPE_MULAW = new Guid(0x00000007, 0x0000, 0x0010, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);
    final static Guid KSDATAFORMAT_SUBTYPE_ALAW = new Guid(0x00000006, 0x0000, 0x0010, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);

    /**
     * Mix at most the given amount of data into the allocated temporary buffer
     * of the given secondary buffer, starting from the dsb's first currently
     * unsampled frame (writepos), translating frequency (pitch), stereo/mono
     * and bits-per-sample so that it is ideal for the primary buffer.
     * Doesn't perform any mixing - this is a straight copy/convert operation.
     *
     * dsb = the secondary buffer
     * writepos = Starting position of changed buffer
     * len = number of bytes to resample from writepos
     *
     * NOTE: writepos + len <= buflen. When called by mixer, MixOne makes sure of this.
     */
    static void DSOUND_MixToTemporary(IDirectSoundBuffer.Data dsb, int writepos, int len)
    {
        WAVEFORMATEX wfx = dsb.wfx();
        int	size;
        int ibp;
        Ptr obp = new Ptr(dsb.tmp_buffer, 0);
        Ptr obp_begin;

        int	iAdvance = wfx.nBlockAlign;
        int oAdvance = DEVICE_BLOCK_ALIGN;
        int freqAcc = 0, target_writepos = 0, overshot, maxlen;

        assert(writepos + len <= dsb.buflen);

        if (dsb.tmp_buffer_copied) {
            dsb.tmp_buffer = dsb.tmp_buffer.clone();
            dsb.tmp_buffer_copied = false;
        }

        maxlen = DSOUND_secpos_to_bufpos(dsb, len, 0, null);

        ibp = dsb.buffer + writepos;
        obp_begin = new Ptr(obp);

        size = len / iAdvance;

        /* Check for same sample rate */
        if (dsb.freq == DEVICE_SAMPLE_RATE) {
            obp = obp_begin;
            obp.inc(writepos/iAdvance*oAdvance);
            cp_fields(dsb, ibp, obp, iAdvance, oAdvance, size, 0, 1 << DSOUND_FREQSHIFT);
            //DSOUND_MixerVol(dsb);
            return;
        }

        /* Mix in different sample rates */
        IntRef r = new IntRef(freqAcc);
        target_writepos = DSOUND_secpos_to_bufpos(dsb, writepos, dsb.sec_mixpos, r);
        freqAcc = r.value;
        overshot = freqAcc >> DSOUND_FREQSHIFT;
        if (overshot != 0)
        {
            if (overshot >= size)
                return;
            size -= overshot;
            writepos += overshot * iAdvance;
            if (writepos >= dsb.buflen)
                return;
            ibp = dsb.buffer + writepos;
            freqAcc &= (1 << DSOUND_FREQSHIFT) - 1;
        }

        obp = new Ptr(obp_begin, target_writepos);

        /* FIXME: Small problem here when we're overwriting buf_mixpos, it then STILL uses old freqAcc, not sure if it matters or not */
        cp_fields(dsb, ibp, obp, iAdvance, oAdvance, size, freqAcc, dsb.freqAdjust);
        // DSOUND_MixerVol(dsb); // :TODO: volume is broken, it needs to take into account start/len
    }

    /**
     * Recalculate the size for temporary buffer, and new writelead
     * Should be called when one of the following things occur:
     * - Primary buffer format is changed
     * - This buffer format (frequency) is changed
     *
     * After this, DSOUND_MixToTemporary(dsb, 0, dsb.buflen) should
     * be called to refill the temporary buffer with data.
     */
    static void DSOUND_RecalcFormat(IDirectSoundBuffer.Data dsb)
    {
        boolean needremix = true;
        boolean needresample = dsb.freq != DEVICE_SAMPLE_RATE;
        WAVEFORMATEX wfx = dsb.wfx();
        int bAlign = wfx.nBlockAlign;
        int pAlign = DEVICE_BLOCK_ALIGN;

        WAVEFORMATEXTENSIBLE pwfxe = dsb.wfxe();
        boolean ieee = false;

        if ((pwfxe.Format.wFormatTag == WAVE_FORMAT_IEEE_FLOAT) || (pwfxe.Format.wFormatTag == WAVE_FORMAT_EXTENSIBLE && pwfxe.SubFormat.equals(KSDATAFORMAT_SUBTYPE_IEEE_FLOAT)))
            ieee = true;

        /* calculate the 10ms write lead */
        dsb.writelead=(dsb.freq / 100) * wfx.nBlockAlign;

        if ((wfx.wBitsPerSample == DEVICE_BITS_PER_SAMEPLE) && (wfx.nChannels == DEVICE_CHANNELS) && !needresample && !ieee)
            needremix = false;
        dsb.max_buffer_len = 0;
        dsb.freqAcc = 0;
        dsb.freqAccNext = 0;
        dsb.freqneeded = needresample;

        if (ieee)
            dsb.convert = DSConvert.convertbpp[4][DEVICE_BITS_PER_SAMEPLE/8 - 1];
        else
            dsb.convert = DSConvert.convertbpp[wfx.wBitsPerSample/8 - 1][DEVICE_BITS_PER_SAMEPLE/8 - 1];

        if (needremix)
        {
            if (needresample)
                DSOUND_RecalcFreqAcc(dsb);
            else
                dsb.tmp_buffer_len = dsb.buflen / bAlign * pAlign;
            dsb.max_buffer_len = dsb.tmp_buffer_len;
            if (dsb.tmp_buffer==null || dsb.tmp_buffer_len>dsb.tmp_buffer.length || dsb.tmp_buffer_copied)
                dsb.tmp_buffer = new byte[dsb.max_buffer_len];
            Arrays.fill(dsb.tmp_buffer, DEVICE_BITS_PER_SAMEPLE == 8 ? (byte)128 : 0);
            dsb.tmp_buffer_copied = false;
        }
        else {
            dsb.max_buffer_len=dsb.buflen;
            dsb.tmp_buffer_len=dsb.buflen;
        }
        dsb.buf_mixpos=DSOUND_secpos_to_bufpos(dsb, dsb.sec_mixpos, 0, null);
    }

    /**
     * Copy a single frame from the given input buffer to the given output buffer.
     * Translate 8 <. 16 bits and mono <. stereo
     */
    static private void cp_fields(IDirectSoundBuffer.Data dsb, int ibuf, Ptr obuf, int istride, int ostride, int count, int freqAcc, int adj)
    {
        WAVEFORMATEX wfx = dsb.wfx();

        int istep = wfx.wBitsPerSample / 8;
        int ostep = DEVICE_BITS_PER_SAMEPLE / 8;

        if (DEVICE_CHANNELS == wfx.nChannels ||
            (DEVICE_CHANNELS == 2 && wfx.nChannels == 6) ||
            (DEVICE_CHANNELS == 8 && wfx.nChannels == 2) ||
            (DEVICE_CHANNELS == 6 && wfx.nChannels == 2)) {
            dsb.convert.call(ibuf, obuf, istride, ostride, count, freqAcc, adj);
            if (DEVICE_CHANNELS == 2 || wfx.nChannels == 2)
                dsb.convert.call(ibuf + istep, new Ptr(obuf, ostep), istride, ostride, count, freqAcc, adj);
            return;
        }

        if (DEVICE_CHANNELS == 1 && wfx.nChannels == 2)
        {
            dsb.convert.call(ibuf, obuf, istride, ostride, count, freqAcc, adj);
            return;
        }

        if (DEVICE_CHANNELS == 2 && wfx.nChannels == 1)
        {
            dsb.convert.call(ibuf, obuf, istride, ostride, count, freqAcc, adj);
            dsb.convert.call(ibuf, new Ptr(obuf, ostep), istride, ostride, count, freqAcc, adj);
            return;
        }

        warn("Unable to remap channels: device=" + DEVICE_CHANNELS + ", buffer=" + wfx.nChannels);
    }


    static public void DSOUND_RecalcVolPan(DSVOLUMEPAN volpan)
    {
        double temp;
        /* the AmpFactors are expressed in 16.16 fixed point */
        volpan.dwVolAmpFactor = (int)(Math.pow(2.0, volpan.lVolume / 600.0) * 0xffff);
        /* FIXME: dwPan{Left|Right}AmpFactor */

        /* FIXME: use calculated vol and pan ampfactors */
        temp = (double) (volpan.lVolume - (volpan.lPan > 0 ? volpan.lPan : 0));
        volpan.dwTotalLeftAmpFactor = (int) (Math.pow(2.0, temp / 600.0) * 0xffff);
        temp = (double) (volpan.lVolume + (volpan.lPan < 0 ? volpan.lPan : 0));
        volpan.dwTotalRightAmpFactor = (int) (Math.pow(2.0, temp / 600.0) * 0xffff);
    }

    /** Apply volume to the given soundbuffer from (primary) position writepos and length len
     * Returns: NULL if no volume needs to be applied
     * or else a memory handle that holds 'len' volume adjusted buffer */
    static void DSOUND_MixerVol(IDirectSoundBuffer.Data dsb)
    {
        int flags = dsb.flags();
        int len = dsb.tmp_buffer_len;

        if (((flags & DSBufferDesc.DSBCAPS_CTRLPAN)==0 || (dsb.volpan.lPan == 0)) &&
            ((flags & DSBufferDesc.DSBCAPS_CTRLVOLUME)==0 || (dsb.volpan.lVolume == 0)) &&
             (flags & DSBufferDesc.DSBCAPS_CTRL3D)==0)
            return; /* Nothing to do */

        if (DEVICE_CHANNELS != 1 && DEVICE_CHANNELS != 2)
        {
            warn("IDirectSoundBuffer: There is no support for " + DEVICE_CHANNELS + " channel volume");
            return;
        }

        if (DEVICE_BITS_PER_SAMEPLE != 8 && DEVICE_BITS_PER_SAMEPLE != 16)
        {
            warn("IDirectSoundBuffer: There is no support for " + DEVICE_BITS_PER_SAMEPLE + "-bit volume");
            return;
        }

        int vLeft = dsb.volpan.dwTotalLeftAmpFactor;
        int vRight;
        if (DEVICE_CHANNELS > 1)
            vRight = dsb.volpan.dwTotalRightAmpFactor;
        else
            vRight = vLeft;

        byte[] buffer = dsb.tmp_buffer;
        switch (DEVICE_BITS_PER_SAMEPLE) {
        case 8:
            /* 8-bit WAV is unsigned, but we need to operate */
            /* on signed data for this to work properly */
            for (int i = 0; i < len-1; i+=2) {
                buffer[i] = (byte) ((((buffer[i] - 128) * vLeft) >> 16) + 128);
                buffer[i] = (byte) ((((buffer[i] - 128) * vRight) >> 16) + 128);
            }
            if (len % 2 == 1 && DEVICE_CHANNELS == 1)
                buffer[len-1] = (byte) ((((buffer[len-1] - 128) * vLeft) >> 16) + 128);
            break;
        case 16:
            /* 16-bit WAV is signed -- much better */
            ShortPtr p = new ShortPtr(buffer, 0);
            for (int i = 0; i < len-3; i += 4) {
                p.set((p.get () * vLeft) >> 16);
                p.inc();
                p.set((p.get () * vRight) >> 16);
                p.inc();
            }
            if (len % 4 == 2 && DEVICE_CHANNELS == 1)
                p.set((p.get () * vLeft) >> 16);
            break;
        }
    }

    /**
     * Move freqAccNext to freqAcc, and find new values for buffer length and freqAccNext
     */
    static void DSOUND_RecalcFreqAcc(IDirectSoundBuffer.Data dsb)
    {
        if (!dsb.freqneeded) return;
        dsb.freqAcc=dsb.freqAccNext;
        IntRef overshoot = new IntRef(dsb.freqAccNext);
        dsb.tmp_buffer_len=DSOUND_secpos_to_bufpos(dsb, dsb.buflen, 0, overshoot);
        dsb.freqAccNext=overshoot.value;
    }

    /* NOTE: Not all secpos have to always be mapped to a bufpos, other way around is always the case
     * DWORD64 is used here because a single DWORD wouldn't be big enough to fit the freqAcc for big buffers
     */
    /** This function converts a 'native' sample pointer to a resampled pointer that fits for primary
     * secmixpos is used to decide which freqAcc is needed
     * overshot tells what the 'actual' secpos is now (optional)
     */
    static private int DSOUND_secpos_to_bufpos(IDirectSoundBuffer.Data dsb, int secpos, int secmixpos, IntRef overshot)
    {
        WAVEFORMATEX wfx = dsb.wfx();
        long framelen = secpos / wfx.nBlockAlign;
        long freqAdjust = dsb.freqAdjust;
        long acc, freqAcc;

        if (secpos < secmixpos)
            freqAcc = dsb.freqAccNext;
        else
            freqAcc = dsb.freqAcc;
        acc = (framelen << DSOUND_FREQSHIFT) + (freqAdjust - 1 - freqAcc);
        if (freqAdjust == 0) {
            int ii=0;
        }
        acc /= freqAdjust;
        if (overshot != null) {
            long oshot = acc * freqAdjust + freqAcc;
            assert(oshot >= framelen << DSOUND_FREQSHIFT);
            oshot -= framelen << DSOUND_FREQSHIFT;
            overshot.value = (int)oshot;
            assert(overshot.value < dsb.freqAdjust);
        }
        return (int)(acc * DEVICE_BLOCK_ALIGN);
    }

}
