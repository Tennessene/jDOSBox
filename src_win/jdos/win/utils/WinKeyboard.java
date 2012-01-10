package jdos.win.utils;

import jdos.gui.Main;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

import java.awt.event.KeyEvent;

public class WinKeyboard {
    static public Main.KeyboardHandler defaultKeyboardHandler = new Main.KeyboardHandler() {
        public void handle(KeyEvent key) {
            int result = 0;
            boolean extended = false;

            switch (key.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:result=0x1B;break;
                case KeyEvent.VK_NUMPAD1:result=0x61;break;
                case KeyEvent.VK_1:result=0x31;break;
                case KeyEvent.VK_NUMPAD2:result=0x62;break;
                case KeyEvent.VK_2:result=0x32;break;
                case KeyEvent.VK_NUMPAD3:result=0x63;break;
                case KeyEvent.VK_3:result=0x33;break;
                case KeyEvent.VK_NUMPAD4:result=0x64;break;
                case KeyEvent.VK_4:result=0x34;break;
                case KeyEvent.VK_NUMPAD5:result=0x65;break;
                case KeyEvent.VK_5:result=0x35;break;
                case KeyEvent.VK_NUMPAD6:result=0x66;break;
                case KeyEvent.VK_6:result=0x36;break;
                case KeyEvent.VK_NUMPAD7:result=0x67;break;
                case KeyEvent.VK_7:result=0x37;break;
                case KeyEvent.VK_NUMPAD8:result=0x68;break;
                case KeyEvent.VK_8:result=0x38;break;
                case KeyEvent.VK_NUMPAD9:result=0x69;break;
                case KeyEvent.VK_9:result=0x39;break;
                case KeyEvent.VK_NUMPAD0:result=0x60;break;
                case KeyEvent.VK_0:result=0x30;break;
                case KeyEvent.VK_SUBTRACT:result=0x6D;break;
                case KeyEvent.VK_MINUS:break;
                case KeyEvent.VK_EQUALS:break;
                case KeyEvent.VK_BACK_SPACE:result=0x08;break;
                case KeyEvent.VK_TAB:result=0x09;break;

                case KeyEvent.VK_Q:result=0x51;break;
                case KeyEvent.VK_W:result=0x57;break;
                case KeyEvent.VK_E:result=0x45;break;
                case KeyEvent.VK_R:result=0x52;break;
                case KeyEvent.VK_T:result=0x54;break;
                case KeyEvent.VK_Y:result=0x59;break;
                case KeyEvent.VK_U:result=0x55;break;
                case KeyEvent.VK_I:result=0x49;break;
                case KeyEvent.VK_O:result=0x4F;break;
                case KeyEvent.VK_P:result=0x50;break;

                case KeyEvent.VK_OPEN_BRACKET:break;
                case KeyEvent.VK_CLOSE_BRACKET:break;
                case KeyEvent.VK_ENTER:result=0x0D;break;
                case KeyEvent.VK_CONTROL:
                    if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT)
                        extended = false;
                    else
                        extended = true;
                    result=0x11;
                    break;
                case KeyEvent.VK_A:result=0x41;break;
                case KeyEvent.VK_S:result=0x53;break;
                case KeyEvent.VK_D:result=0x44;break;
                case KeyEvent.VK_F:result=0x46;break;
                case KeyEvent.VK_G:result=0x47;break;
                case KeyEvent.VK_H:result=0x48;break;
                case KeyEvent.VK_J:result=0x4A;break;
                case KeyEvent.VK_K:result=0x4B;break;
                case KeyEvent.VK_L:result=0x4C;break;

                case KeyEvent.VK_SEMICOLON:break;
                case KeyEvent.VK_QUOTE:break;
                case KeyEvent.VK_BACK_QUOTE:break;
                case KeyEvent.VK_SHIFT:
                    if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT)
                        ;
                    else
                        ;
                    result = 0x10;
                    break;
                case KeyEvent.VK_BACK_SLASH:break;
                case KeyEvent.VK_Z:result=0x5A;break;
                case KeyEvent.VK_X:result=0x58;break;
                case KeyEvent.VK_C:result=0x43;break;
                case KeyEvent.VK_V:result=0x56;break;
                case KeyEvent.VK_B:result=0x42;break;
                case KeyEvent.VK_N:result=0x4E;break;
                case KeyEvent.VK_M:result=0x4D;break;

                case KeyEvent.VK_COMMA:break;
                case KeyEvent.VK_PERIOD:break;
                case KeyEvent.VK_DECIMAL:result=0x6E;break;
                case KeyEvent.VK_SLASH:break;
                case KeyEvent.VK_MULTIPLY:result=0x6A;break;
                case KeyEvent.VK_ALT:
                    if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT)
                        extended = false;
                    else
                        extended = true;
                    result=0x12;
                    break;
                case KeyEvent.VK_SPACE:result=0x20;break;
                case KeyEvent.VK_CAPS_LOCK:result=0x14;break;

                case KeyEvent.VK_F1:result=0x70;break;
                case KeyEvent.VK_F2:result=0x71;break;
                case KeyEvent.VK_F3:result=0x72;break;
                case KeyEvent.VK_F4:result=0x73;break;
                case KeyEvent.VK_F5:result=0x74;break;
                case KeyEvent.VK_F6:result=0x75;break;
                case KeyEvent.VK_F7:result=0x76;break;
                case KeyEvent.VK_F8:result=0x77;break;
                case KeyEvent.VK_F9:result=0x78;break;
                case KeyEvent.VK_F10:result=0x79;break;

                case KeyEvent.VK_NUM_LOCK:result=0x90;break;
                case KeyEvent.VK_SCROLL_LOCK:result=0x91;break;

                case KeyEvent.VK_PLUS:break;

                case KeyEvent.VK_LESS:break;
                case KeyEvent.VK_F11:result=0x7A;break;
                case KeyEvent.VK_F12:result=0x7B;break;

                //The Extended keys

                case KeyEvent.VK_DIVIDE:result=0x6F;break;
                case KeyEvent.VK_ADD:result=0x6B;break;
                case KeyEvent.VK_HOME:result=0x24;break;
                case KeyEvent.VK_UP:result=0x26;break;
                case KeyEvent.VK_PAGE_UP:result=0x21;break;
                case KeyEvent.VK_LEFT:result=0x25;break;
                case KeyEvent.VK_RIGHT:result=0x27;break;
                case KeyEvent.VK_END:result=0x23;break;
                case KeyEvent.VK_DOWN:result=0x28;break;
                case KeyEvent.VK_PAGE_DOWN:result=0x22;break;
                case KeyEvent.VK_INSERT:result=0x2D;break;
                case KeyEvent.VK_DELETE:result=0x2E;break;
                case KeyEvent.VK_PAUSE:result=0x13;break;
                case KeyEvent.VK_PRINTSCREEN:result=0x2C;break;
                default:
                    if (Log.level<= LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_GUI, LogSeverities.LOG_WARN, "Unknown key code: "+key.getKeyCode());
                    return;
            }
            if (result != 0) {
                int repeatCount=1;
                int oem=0;
                int reserved = 0;
                int contextCode = 0;
                int previousState = 0;
                int transitionState = 0;
                int msg = 0;

                if (key.getID() == KeyEvent.KEY_PRESSED) {
                    msg = WinWindow.WM_KEYDOWN;
                    transitionState = 0; // always 0
                } else if (key.getID() == KeyEvent.KEY_RELEASED) {
                    msg = WinWindow.WM_KEYUP;
                    repeatCount=1; // repeat count is always 1
                    previousState=1; // always 1
                    transitionState=1; // always 1
                }
                if (msg != 0 && WinSystem.getCurrentThread()!=null)
                    WinSystem.getCurrentThread().postMessage(WinWindow.currentFocus, msg, result, repeatCount | (oem << 16) | (extended? 1<<24 : 0) | (contextCode << 29) | (previousState << 30) | (transitionState << 31));
            }
        }
    };
}