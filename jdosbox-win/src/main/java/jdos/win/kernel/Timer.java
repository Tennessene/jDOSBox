package jdos.win.kernel;

import jdos.cpu.Callback;
import jdos.hardware.IO;
import jdos.win.builtin.HandlerBase;
import jdos.win.system.Scheduler;
import jdos.win.system.WinSystem;

public class Timer {
    public Timer(int frequency) {
        //init_timer(frequency);
        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    HandlerBase.tick = true;
                    try {Thread.sleep(15);} catch (Exception e) {}
                }
            }
        };
        thread.start();
    }

    Callback.Handler handler = new Callback.Handler() {
        int tickCount;
        public int call() {
            Scheduler.tick();
            return 0;
        }

        public String getName() {
            return "Timer";
        }
    };

    private void init_timer(int frequency) {
       // Firstly, register our timer callback.
       WinSystem.interrupts.registerHandler(Interrupts.IRQ0, handler);

       // The value we send to the PIT is the value to divide it's input clock
       // (1193180 Hz) by, to get our required frequency. Important to note is
       // that the divisor must be small enough to fit into 16-bits.
       int divisor = 1193180 / frequency;

       // Send the command byte.
       IO.IO_WriteB(0x43, 0x36);

       // Divisor has to be sent byte-wise, so split here into upper/lower bytes.
       int l = (divisor & 0xFF);
       int h = ( (divisor>>8) & 0xFF );

       // Send the frequency divisor.
       IO.IO_WriteB(0x40, l);
       IO.IO_WriteB(0x40, h);
    }
}
