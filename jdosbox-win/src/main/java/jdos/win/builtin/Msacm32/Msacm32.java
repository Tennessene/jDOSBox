package jdos.win.builtin.Msacm32;

import jdos.win.builtin.winmm.WAVEFORMATEX;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;

public class Msacm32 extends BuiltinModule {
    public Msacm32(Loader loader, int handle) {
        super(loader, "Msacm32.dll", handle);

        add(Msacm32.class, "acmMetrics", new String[] {"hao", "uMetric", "(HEX)pMetric"});
    }

    // MMRESULT acmMetrics(HACMOBJ hao, UINT uMetric, LPVOID pMetric)
    public static int acmMetrics(int hao, int uMetric, int pMetric) {
        switch (uMetric) {
            case ACM_METRIC_MAX_SIZE_FORMAT:
                if (pMetric != 0) {
                    writed(pMetric, WAVEFORMATEX.SIZE);
                    return MMSYSERR_NOERROR;
                }

        }
        return MMSYSERR_ERROR;
    }
}
