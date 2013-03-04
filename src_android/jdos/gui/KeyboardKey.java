package jdos.gui;

import android.view.KeyEvent;
import jdos.sdl.JavaMapper;

public class KeyboardKey {
    static public boolean isLeft(Object key) {
        return false;
    }

    static public boolean isRight(Object key) {
        return false;
    }

    static public boolean isNumPad(Object key) {
        return false;
    }

    static public boolean isPressed(Object key) {
        return ((KeyEvent)key).getAction()==KeyEvent.ACTION_DOWN;
    }

    static public boolean isReleased(Object key) {
        return ((KeyEvent)key).getAction()==KeyEvent.ACTION_UP;
    }

    static public int getKeyCode(Object key) {
        return ((KeyEvent)key).getKeyCode();
    }

    static public void CreateDefaultBinds() {
        JavaMapper.CreateStringBind("mod_1 \"key " + KeyEvent.KEYCODE_CTRL_RIGHT + "\"");
        JavaMapper.CreateStringBind("mod_2 \"key "+KeyEvent.KEYCODE_ALT_RIGHT+"\"");
    }

    static public int translateMapKey(int key) {
        switch (key) {
            case Mapper.MapKeys.MK_f1:case Mapper.MapKeys.MK_f2:case Mapper.MapKeys.MK_f3:case Mapper.MapKeys.MK_f4:
            case Mapper.MapKeys.MK_f5:case Mapper.MapKeys.MK_f6:case Mapper.MapKeys.MK_f7:case Mapper.MapKeys.MK_f8:
            case Mapper.MapKeys.MK_f9:case Mapper.MapKeys.MK_f10:case Mapper.MapKeys.MK_f11:case Mapper.MapKeys.MK_f12:
                key=KeyEvent.KEYCODE_F1+(key-Mapper.MapKeys.MK_f1);
                break;
            case Mapper.MapKeys.MK_return:
                key=KeyEvent.KEYCODE_ENTER;
                break;
            case Mapper.MapKeys.MK_kpminus:
                key=KeyEvent.KEYCODE_MINUS;
                break;
            case Mapper.MapKeys.MK_scrolllock:
                key=KeyEvent.KEYCODE_SCROLL_LOCK;
                break;
            case Mapper.MapKeys.MK_pause:
                key=KeyEvent.KEYCODE_BREAK;
                break;
            case Mapper.MapKeys.MK_printscreen:
                key=KeyEvent.KEYCODE_SYSRQ;
                break;
            case Mapper.MapKeys.MK_home:
                key=KeyEvent.KEYCODE_HOME;
                break;
        }
        return key;
    }

    static boolean ctrAltDel=false;

    static public void CheckEvent(Object e) {
//        KeyEvent event = (KeyEvent)e;
//        if (JavaMapper.mapper.mods == 3 && event.getKeyCode() == KeyEvent.KEYCODE_INSERT && event.getAction()==KeyEvent.ACTION_DOWN) {
//            ctrAltDel = true;
//        }
//        if (ctrAltDel && event.getKeyCode() == KeyEvent.KEYCODE_INSERT) {
//            event.setKeyCode(KeyEvent.KEYCODE_DEL);
//        }
        JavaMapper.MAPPER_CheckEvent(e);
//
//        if (ctrAltDel && event.getKeyCode() == KeyEvent.KEYCODE_INSERT && event.getAction()==KeyEvent.ACTION_UP) {
//            ctrAltDel = false;
//        }
    }

    static public final JavaMapper.DefaultKey[] DefaultKeys = new JavaMapper.DefaultKey[] {
            new JavaMapper.DefaultKey("f1", KeyEvent.KEYCODE_F1),		new JavaMapper.DefaultKey("f2",KeyEvent.KEYCODE_F2),		new JavaMapper.DefaultKey("f3",KeyEvent.KEYCODE_F3),		new JavaMapper.DefaultKey("f4",KeyEvent.KEYCODE_F4),
            new JavaMapper.DefaultKey("f5",KeyEvent.KEYCODE_F5),		new JavaMapper.DefaultKey("f6",KeyEvent.KEYCODE_F6),		new JavaMapper.DefaultKey("f7",KeyEvent.KEYCODE_F7),		new JavaMapper.DefaultKey("f8",KeyEvent.KEYCODE_F8),
            new JavaMapper.DefaultKey("f9",KeyEvent.KEYCODE_F9),		new JavaMapper.DefaultKey("f10",KeyEvent.KEYCODE_F10),	    new JavaMapper.DefaultKey("f11",KeyEvent.KEYCODE_F11),	    new JavaMapper.DefaultKey("f12",KeyEvent.KEYCODE_F12),
    
            new JavaMapper.DefaultKey("1",KeyEvent.KEYCODE_1),		new JavaMapper.DefaultKey("2",KeyEvent.KEYCODE_2),		new JavaMapper.DefaultKey("3",KeyEvent.KEYCODE_3),		new JavaMapper.DefaultKey("4",KeyEvent.KEYCODE_4),
            new JavaMapper.DefaultKey("5",KeyEvent.KEYCODE_5),		new JavaMapper.DefaultKey("6",KeyEvent.KEYCODE_6),		new JavaMapper.DefaultKey("7",KeyEvent.KEYCODE_7),		new JavaMapper.DefaultKey("8",KeyEvent.KEYCODE_8),
            new JavaMapper.DefaultKey("9",KeyEvent.KEYCODE_9),		new JavaMapper.DefaultKey("0",KeyEvent.KEYCODE_0),
    
            new JavaMapper.DefaultKey("a",KeyEvent.KEYCODE_A),		new JavaMapper.DefaultKey("b",KeyEvent.KEYCODE_B),		new JavaMapper.DefaultKey("c",KeyEvent.KEYCODE_C),		new JavaMapper.DefaultKey("d",KeyEvent.KEYCODE_D),
            new JavaMapper.DefaultKey("e",KeyEvent.KEYCODE_E),		new JavaMapper.DefaultKey("f",KeyEvent.KEYCODE_F),		new JavaMapper.DefaultKey("g",KeyEvent.KEYCODE_G),		new JavaMapper.DefaultKey("h",KeyEvent.KEYCODE_H),
            new JavaMapper.DefaultKey("i",KeyEvent.KEYCODE_I),		new JavaMapper.DefaultKey("j",KeyEvent.KEYCODE_J),		new JavaMapper.DefaultKey("k",KeyEvent.KEYCODE_K),		new JavaMapper.DefaultKey("l",KeyEvent.KEYCODE_L),
            new JavaMapper.DefaultKey("m",KeyEvent.KEYCODE_M),		new JavaMapper.DefaultKey("n",KeyEvent.KEYCODE_N),		new JavaMapper.DefaultKey("o",KeyEvent.KEYCODE_O),		new JavaMapper.DefaultKey("p",KeyEvent.KEYCODE_P),
            new JavaMapper.DefaultKey("q",KeyEvent.KEYCODE_Q),		new JavaMapper.DefaultKey("r",KeyEvent.KEYCODE_R),		new JavaMapper.DefaultKey("s",KeyEvent.KEYCODE_S),		new JavaMapper.DefaultKey("t",KeyEvent.KEYCODE_T),
            new JavaMapper.DefaultKey("u",KeyEvent.KEYCODE_U),		new JavaMapper.DefaultKey("v",KeyEvent.KEYCODE_V),		new JavaMapper.DefaultKey("w",KeyEvent.KEYCODE_W),		new JavaMapper.DefaultKey("x",KeyEvent.KEYCODE_X),
            new JavaMapper.DefaultKey("y",KeyEvent.KEYCODE_Y),		new JavaMapper.DefaultKey("z",KeyEvent.KEYCODE_Z),		new JavaMapper.DefaultKey("space",KeyEvent.KEYCODE_SPACE),
    
            new JavaMapper.DefaultKey("esc",KeyEvent.KEYCODE_ESCAPE),	        new JavaMapper.DefaultKey("equals",KeyEvent.KEYCODE_EQUALS),		new JavaMapper.DefaultKey("grave",KeyEvent.KEYCODE_GRAVE),
            new JavaMapper.DefaultKey("tab",KeyEvent.KEYCODE_TAB),		        new JavaMapper.DefaultKey("enter",KeyEvent.KEYCODE_ENTER),		/*new JavaMapper.DefaultKey("bspace",KeyEvent.KEYCODE_BACKSPACE),*/
            new JavaMapper.DefaultKey("lbracket",KeyEvent.KEYCODE_LEFT_BRACKET),new JavaMapper.DefaultKey("rbracket",KeyEvent.KEYCODE_RIGHT_BRACKET),
            new JavaMapper.DefaultKey("minus",KeyEvent.KEYCODE_MINUS),	        new JavaMapper.DefaultKey("capslock",KeyEvent.KEYCODE_CAPS_LOCK),	new JavaMapper.DefaultKey("semicolon",KeyEvent.KEYCODE_SEMICOLON),
            /*new JavaMapper.DefaultKey("quote", KeyEvent.KEYCODE_QUOTE),*/	    new JavaMapper.DefaultKey("backslash",KeyEvent.KEYCODE_BACKSLASH),	new JavaMapper.DefaultKey("lshift",KeyEvent.KEYCODE_SHIFT_LEFT, false, false, false),
            new JavaMapper.DefaultKey("rshift",KeyEvent.KEYCODE_SHIFT_RIGHT, false, false, false),	    new JavaMapper.DefaultKey("lalt",KeyEvent.KEYCODE_ALT_LEFT, false, false, false),			new JavaMapper.DefaultKey("ralt",KeyEvent.KEYCODE_ALT_RIGHT, false, false, false),
            new JavaMapper.DefaultKey("lctrl",KeyEvent.KEYCODE_CTRL_LEFT, false, false, false),	        new JavaMapper.DefaultKey("rctrl",KeyEvent.KEYCODE_CTRL_RIGHT, false, false, false),		    new JavaMapper.DefaultKey("comma",KeyEvent.KEYCODE_COMMA),
            new JavaMapper.DefaultKey("period",KeyEvent.KEYCODE_PERIOD),	    new JavaMapper.DefaultKey("slash",KeyEvent.KEYCODE_SLASH),		    new JavaMapper.DefaultKey("printscreen",KeyEvent.KEYCODE_SYSRQ),
            new JavaMapper.DefaultKey("scrolllock",KeyEvent.KEYCODE_SCROLL_LOCK),new JavaMapper.DefaultKey("pause",KeyEvent.KEYCODE_BREAK),		    new JavaMapper.DefaultKey("pagedown",KeyEvent.KEYCODE_PAGE_DOWN),
            new JavaMapper.DefaultKey("pageup",KeyEvent.KEYCODE_PAGE_UP),	    new JavaMapper.DefaultKey("insert",KeyEvent.KEYCODE_INSERT),		new JavaMapper.DefaultKey("home",KeyEvent.KEYCODE_MOVE_HOME),
            new JavaMapper.DefaultKey("delete",KeyEvent.KEYCODE_DEL),	    new JavaMapper.DefaultKey("end",KeyEvent.KEYCODE_MOVE_END),			    new JavaMapper.DefaultKey("up",KeyEvent.KEYCODE_DPAD_UP),
            new JavaMapper.DefaultKey("left",KeyEvent.KEYCODE_DPAD_LEFT),		    new JavaMapper.DefaultKey("down",KeyEvent.KEYCODE_DPAD_DOWN),			new JavaMapper.DefaultKey("right",KeyEvent.KEYCODE_DPAD_RIGHT),
            new JavaMapper.DefaultKey("kp_0",KeyEvent.KEYCODE_NUMPAD_0),	        new JavaMapper.DefaultKey("kp_1",KeyEvent.KEYCODE_NUMPAD_1),	        new JavaMapper.DefaultKey("kp_2",KeyEvent.KEYCODE_NUMPAD_2),	new JavaMapper.DefaultKey("kp_3",KeyEvent.KEYCODE_NUMPAD_3),
            new JavaMapper.DefaultKey("kp_4",KeyEvent.KEYCODE_NUMPAD_4),	        new JavaMapper.DefaultKey("kp_5",KeyEvent.KEYCODE_NUMPAD_5),	        new JavaMapper.DefaultKey("kp_6",KeyEvent.KEYCODE_NUMPAD_6),	new JavaMapper.DefaultKey("kp_7",KeyEvent.KEYCODE_NUMPAD_7),
            new JavaMapper.DefaultKey("kp_8",KeyEvent.KEYCODE_NUMPAD_8),	        new JavaMapper.DefaultKey("kp_9",KeyEvent.KEYCODE_NUMPAD_9),	        new JavaMapper.DefaultKey("numlock",KeyEvent.KEYCODE_NUM_LOCK),
            new JavaMapper.DefaultKey("kp_divide",KeyEvent.KEYCODE_NUMPAD_DIVIDE, false, false, true),	new JavaMapper.DefaultKey("kp_multiply",KeyEvent.KEYCODE_NUMPAD_MULTIPLY, false, false, true),
            new JavaMapper.DefaultKey("kp_minus",KeyEvent.KEYCODE_NUMPAD_SUBTRACT, false, false, true),	new JavaMapper.DefaultKey("kp_plus",KeyEvent.KEYCODE_NUMPAD_ADD),
            new JavaMapper.DefaultKey("kp_period",KeyEvent.KEYCODE_PERIOD, false, false, true),	new JavaMapper.DefaultKey("kp_enter",KeyEvent.KEYCODE_ENTER, false, false, true),
            /*new JavaMapper.DefaultKey("lessthan",KeyEvent.KEYCODE_LESS)*/
        };
}
