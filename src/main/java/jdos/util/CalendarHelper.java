package jdos.util;

import jdos.dos.Dos;

import java.util.Calendar;

public class CalendarHelper {
    public static int Dos_time(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        return Dos.DOS_PackTime(c.get(Calendar.HOUR), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
    }

    public static int Dos_date(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        return Dos.DOS_PackDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
    }
}
