package jdos.win.utils;

import jdos.hardware.Memory;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SystemTime {
    public static void write(int lpSystemTime, TimeZone tz, long javaTime) {
        Calendar c = Calendar.getInstance(tz);
        c.setTime(new Date(javaTime));
        Memory.mem_writew(lpSystemTime, c.get(Calendar.YEAR));lpSystemTime+=2;
        Memory.mem_writew(lpSystemTime, c.get(Calendar.MONTH)+1);lpSystemTime+=2;
        Memory.mem_writew(lpSystemTime, c.get(Calendar.DAY_OF_WEEK)-1);lpSystemTime+=2;
        Memory.mem_writew(lpSystemTime, c.get(Calendar.DAY_OF_MONTH));lpSystemTime+=2;
        Memory.mem_writew(lpSystemTime, c.get(Calendar.HOUR_OF_DAY));lpSystemTime+=2;
        Memory.mem_writew(lpSystemTime, c.get(Calendar.MINUTE));lpSystemTime+=2;
        Memory.mem_writew(lpSystemTime, c.get(Calendar.SECOND));lpSystemTime+=2;
        Memory.mem_writew(lpSystemTime, c.get(Calendar.MILLISECOND));lpSystemTime+=2;
    }
}
