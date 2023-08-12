package jdos.win.system;

import jdos.gui.Main;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.win.builtin.user32.Input;
import jdos.win.builtin.user32.WinWindow;

import java.awt.event.KeyEvent;
import java.util.BitSet;

public class WinKeyboard {
    static public BitSet keyState = new BitSet();

    static public int win2java(int winVirtualKeyCode) {
        switch (winVirtualKeyCode) {
            case 0x1B: return KeyEvent.VK_ESCAPE;
            case 0x61: return KeyEvent.VK_NUMPAD1;
            case 0x31: return KeyEvent.VK_1;
            case 0x62: return KeyEvent.VK_NUMPAD2;
            case 0x32: return KeyEvent.VK_2;
            case 0x63: return KeyEvent.VK_NUMPAD3;
            case 0x33: return KeyEvent.VK_3;
            case 0x64: return KeyEvent.VK_NUMPAD4;
            case 0x34: return KeyEvent.VK_4;
            case 0x65: return KeyEvent.VK_NUMPAD5;
            case 0x35: return KeyEvent.VK_5;
            case 0x66: return KeyEvent.VK_NUMPAD6;
            case 0x36: return KeyEvent.VK_6;
            case 0x67: return KeyEvent.VK_NUMPAD7;
            case 0x37: return KeyEvent.VK_7;
            case 0x68: return KeyEvent.VK_NUMPAD8;
            case 0x38: return KeyEvent.VK_8;
            case 0x69: return KeyEvent.VK_NUMPAD9;
            case 0x39: return KeyEvent.VK_9;
            case 0x60: return KeyEvent.VK_NUMPAD0;
            case 0x30: return KeyEvent.VK_0;
            case 0x6D: return KeyEvent.VK_SUBTRACT;
            //case KeyEvent.VK_MINUS:break;
            //case KeyEvent.VK_EQUALS:break;
            case 0x08: return KeyEvent.VK_BACK_SPACE;
            case 0x09: return KeyEvent.VK_TAB;

            case 0x51: return KeyEvent.VK_Q;
            case 0x57: return KeyEvent.VK_W;
            case 0x45: return KeyEvent.VK_E;
            case 0x52: return KeyEvent.VK_R;
            case 0x54: return KeyEvent.VK_T;
            case 0x59: return KeyEvent.VK_Y;
            case 0x55: return KeyEvent.VK_U;
            case 0x49: return KeyEvent.VK_I;
            case 0x4F: return KeyEvent.VK_O;
            case 0x50: return KeyEvent.VK_P;

            //case KeyEvent.VK_OPEN_BRACKET:break;
            //case KeyEvent.VK_CLOSE_BRACKET:break;
            case 0x0D: return KeyEvent.VK_ENTER;
            case 0x11: return KeyEvent.VK_CONTROL;
            case 0x41: return KeyEvent.VK_A;
            case 0x53: return KeyEvent.VK_S;
            case 0x44: return KeyEvent.VK_D;
            case 0x46: return KeyEvent.VK_F;
            case 0x47: return KeyEvent.VK_G;
            case 0x48: return KeyEvent.VK_H;
            case 0x4A: return KeyEvent.VK_J;
            case 0x4B: return KeyEvent.VK_K;
            case 0x4C: return KeyEvent.VK_L;

            //case KeyEvent.VK_SEMICOLON:break;
            //case KeyEvent.VK_QUOTE:break;
            //case KeyEvent.VK_BACK_QUOTE:break;
            case 0x10: return KeyEvent.VK_SHIFT;

            //case KeyEvent.VK_BACK_SLASH:break;
            case 0x5A: return KeyEvent.VK_Z;
            case 0x58: return KeyEvent.VK_X;
            case 0x43: return KeyEvent.VK_C;
            case 0x56: return KeyEvent.VK_V;
            case 0x42: return KeyEvent.VK_B;
            case 0x4E: return KeyEvent.VK_N;
            case 0x4D: return KeyEvent.VK_M;

            //case KeyEvent.VK_COMMA:break;
            //case KeyEvent.VK_PERIOD:break;
            case 0x6E: return KeyEvent.VK_DECIMAL;
            //case KeyEvent.VK_SLASH:break;
            case 0x6A: return KeyEvent.VK_MULTIPLY;
            case 0x12: return KeyEvent.VK_ALT;
            case 0x20: return KeyEvent.VK_SPACE;
            case 0x14: return KeyEvent.VK_CAPS_LOCK;

            case 0x70: return KeyEvent.VK_F1;
            case 0x71: return KeyEvent.VK_F2;
            case 0x72: return KeyEvent.VK_F3;
            case 0x73: return KeyEvent.VK_F4;
            case 0x74: return KeyEvent.VK_F5;
            case 0x75: return KeyEvent.VK_F6;
            case 0x76: return KeyEvent.VK_F7;
            case 0x77: return KeyEvent.VK_F8;
            case 0x78: return KeyEvent.VK_F9;
            case 0x79: return KeyEvent.VK_F10;

            case 0x90: return KeyEvent.VK_NUM_LOCK;
            case 0x91: return KeyEvent.VK_SCROLL_LOCK;

            // case KeyEvent.VK_PLUS:break;

            // case KeyEvent.VK_LESS:break;
            case 0x7A: return KeyEvent.VK_F11;
            case 0x7B: return KeyEvent.VK_F12;

            //The Extended keys

            case 0x6F: return KeyEvent.VK_DIVIDE;
            case 0x6B: return KeyEvent.VK_ADD;
            case 0x24: return KeyEvent.VK_HOME;
            case 0x26: return KeyEvent.VK_UP;
            case 0x21: return KeyEvent.VK_PAGE_UP;
            case 0x25: return KeyEvent.VK_LEFT;
            case 0x27: return KeyEvent.VK_RIGHT;
            case 0x23: return KeyEvent.VK_END;
            case 0x28: return KeyEvent.VK_DOWN;
            case 0x22: return KeyEvent.VK_PAGE_DOWN;
            case 0x2D: return KeyEvent.VK_INSERT;
            case 0x2E: return KeyEvent.VK_DELETE;
            case 0x13: return KeyEvent.VK_PAUSE;
            case 0x2C: return KeyEvent.VK_PRINTSCREEN;
        }
        return -1;
    }

    static public Main.KeyboardHandler defaultKeyboardHandler = new Main.KeyboardHandler() {
        public void handle(KeyEvent key) {
            int result = 0;
            int additional = 0;
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
                case KeyEvent.VK_MINUS:result=0xBD;break;
                case KeyEvent.VK_EQUALS:result=0xBB;break;
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

                case KeyEvent.VK_OPEN_BRACKET:result=0xDB;break;
                case KeyEvent.VK_CLOSE_BRACKET:result=0xDD;break;
                case KeyEvent.VK_ENTER:result=0x0D;break;
                case KeyEvent.VK_CONTROL:
                    if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT){
                        additional = 0xA2;
                        extended = false;
                    } else {
                        additional = 0xA3;
                        extended = true;
                    }
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

                case KeyEvent.VK_SEMICOLON:result=0xBA;break;
                case KeyEvent.VK_QUOTE:result=0xDE;break;
                case KeyEvent.VK_BACK_QUOTE:result=0xCE;break;
                case KeyEvent.VK_SHIFT:
                    if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT) {
                        extended = false;
                        additional = 0xA0;
                    } else {
                        extended = true;
                        additional = 0xA1;
                    }
                    result = 0x10;
                    break;
                case KeyEvent.VK_BACK_SLASH:result=0xDC;break;
                case KeyEvent.VK_Z:result=0x5A;break;
                case KeyEvent.VK_X:result=0x58;break;
                case KeyEvent.VK_C:result=0x43;break;
                case KeyEvent.VK_V:result=0x56;break;
                case KeyEvent.VK_B:result=0x42;break;
                case KeyEvent.VK_N:result=0x4E;break;
                case KeyEvent.VK_M:result=0x4D;break;

                case KeyEvent.VK_COMMA:result=0xBC;break;
                case KeyEvent.VK_PERIOD:result=0xBE;break;
                case KeyEvent.VK_DECIMAL:result=0x6E;break;
                case KeyEvent.VK_SLASH:result=0xBF;break;
                case KeyEvent.VK_MULTIPLY:result=0x6A;break;
                case KeyEvent.VK_ALT:
                    if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT) {
                        additional = 0xA4;
                        extended = false;
                    } else {
                        additional = 0xA5;
                        extended = true;
                    }
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
                    if (keyState.get(result))
                        return;
                    keyState.set(result);
                    if (additional != 0)
                        keyState.set(additional);
                } else if (key.getID() == KeyEvent.KEY_RELEASED) {
                    msg = WinWindow.WM_KEYUP;
                    repeatCount=1; // repeat count is always 1
                    previousState=1; // always 1
                    transitionState=1; // always 1
                    keyState.clear(result);
                    if (additional != 0)
                        keyState.clear(additional);
                }
                if (msg != 0)
                    Input.addKeyboardMsg(msg, result, repeatCount | (oem << 16) | (extended? 1<<24 : 0) | (contextCode << 29) | (previousState << 30) | (transitionState << 31), (BitSet)keyState.clone());
            }
        }
    };
}