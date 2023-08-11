package jdos.gui;

import jdos.sdl.JavaMapper;

import java.awt.event.KeyEvent;

public class KeyboardKey {
    static public boolean isLeft(Object key) {
        return ((KeyEvent)key).getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT;
    }

    static public boolean isRight(Object key) {
        return ((KeyEvent)key).getKeyLocation()==KeyEvent.KEY_LOCATION_RIGHT;
    }

    static public boolean isNumPad(Object key) {
        return ((KeyEvent)key).getKeyLocation()==KeyEvent.KEY_LOCATION_NUMPAD;
    }

    static public boolean isPressed(Object key) {
        return ((KeyEvent)key).getID()==KeyEvent.KEY_PRESSED;
    }

    static public boolean isReleased(Object key) {
        return ((KeyEvent)key).getID()==KeyEvent.KEY_RELEASED;
    }

    static public int getKeyCode(Object key) {
        return ((KeyEvent)key).getKeyCode();
    }

    static public void CreateDefaultBinds() {
        JavaMapper.CreateStringBind("mod_1 \"key "+KeyEvent.VK_CONTROL+" right\"");
        JavaMapper.CreateStringBind("mod_2 \"key "+KeyEvent.VK_ALT+" right\"");
    }

    static public int translateMapKey(int key) {
        switch (key) {
            case Mapper.MapKeys.MK_f1:case Mapper.MapKeys.MK_f2:case Mapper.MapKeys.MK_f3:case Mapper.MapKeys.MK_f4:
            case Mapper.MapKeys.MK_f5:case Mapper.MapKeys.MK_f6:case Mapper.MapKeys.MK_f7:case Mapper.MapKeys.MK_f8:
            case Mapper.MapKeys.MK_f9:case Mapper.MapKeys.MK_f10:case Mapper.MapKeys.MK_f11:case Mapper.MapKeys.MK_f12:
                key=KeyEvent.VK_F1+(key-Mapper.MapKeys.MK_f1);
                break;
            case Mapper.MapKeys.MK_return:
                key=KeyEvent.VK_ENTER;
                break;
            case Mapper.MapKeys.MK_kpminus:
                key=KeyEvent.VK_MINUS;
                break;
            case Mapper.MapKeys.MK_scrolllock:
                key=KeyEvent.VK_SCROLL_LOCK;
                break;
            case Mapper.MapKeys.MK_pause:
                key=KeyEvent.VK_PAUSE;
                break;
            case Mapper.MapKeys.MK_printscreen:
                key=KeyEvent.VK_PRINTSCREEN;
                break;
            case Mapper.MapKeys.MK_home:
                key=KeyEvent.VK_HOME;
                break;
        }
        return key;
    }

    static boolean ctrAltDel=false;

    static public void CheckEvent(Object e) {
        KeyEvent event = (KeyEvent)e;
        if (JavaMapper.mapper.mods == 3 && event.getKeyCode() == KeyEvent.VK_INSERT && event.getID()==KeyEvent.KEY_PRESSED) {
            ctrAltDel = true;
        }
        if (ctrAltDel && event.getKeyCode() == KeyEvent.VK_INSERT) {
            event.setKeyCode(KeyEvent.VK_DELETE);
        }
        JavaMapper.MAPPER_CheckEvent(e);

        if (ctrAltDel && event.getKeyCode() == KeyEvent.VK_INSERT && event.getID()==KeyEvent.KEY_RELEASED) {
            ctrAltDel = false;
        }
    }

    static public final JavaMapper.DefaultKey[] DefaultKeys = new JavaMapper.DefaultKey[] {
            new JavaMapper.DefaultKey("f1", KeyEvent.VK_F1),		new JavaMapper.DefaultKey("f2",KeyEvent.VK_F2),		new JavaMapper.DefaultKey("f3",KeyEvent.VK_F3),		new JavaMapper.DefaultKey("f4",KeyEvent.VK_F4),
            new JavaMapper.DefaultKey("f5",KeyEvent.VK_F5),		new JavaMapper.DefaultKey("f6",KeyEvent.VK_F6),		new JavaMapper.DefaultKey("f7",KeyEvent.VK_F7),		new JavaMapper.DefaultKey("f8",KeyEvent.VK_F8),
            new JavaMapper.DefaultKey("f9",KeyEvent.VK_F9),		new JavaMapper.DefaultKey("f10",KeyEvent.VK_F10),	    new JavaMapper.DefaultKey("f11",KeyEvent.VK_F11),	    new JavaMapper.DefaultKey("f12",KeyEvent.VK_F12),
    
            new JavaMapper.DefaultKey("1",KeyEvent.VK_1),		new JavaMapper.DefaultKey("2",KeyEvent.VK_2),		new JavaMapper.DefaultKey("3",KeyEvent.VK_3),		new JavaMapper.DefaultKey("4",KeyEvent.VK_4),
            new JavaMapper.DefaultKey("5",KeyEvent.VK_5),		new JavaMapper.DefaultKey("6",KeyEvent.VK_6),		new JavaMapper.DefaultKey("7",KeyEvent.VK_7),		new JavaMapper.DefaultKey("8",KeyEvent.VK_8),
            new JavaMapper.DefaultKey("9",KeyEvent.VK_9),		new JavaMapper.DefaultKey("0",KeyEvent.VK_0),
    
            new JavaMapper.DefaultKey("a",KeyEvent.VK_A),		new JavaMapper.DefaultKey("b",KeyEvent.VK_B),		new JavaMapper.DefaultKey("c",KeyEvent.VK_C),		new JavaMapper.DefaultKey("d",KeyEvent.VK_D),
            new JavaMapper.DefaultKey("e",KeyEvent.VK_E),		new JavaMapper.DefaultKey("f",KeyEvent.VK_F),		new JavaMapper.DefaultKey("g",KeyEvent.VK_G),		new JavaMapper.DefaultKey("h",KeyEvent.VK_H),
            new JavaMapper.DefaultKey("i",KeyEvent.VK_I),		new JavaMapper.DefaultKey("j",KeyEvent.VK_J),		new JavaMapper.DefaultKey("k",KeyEvent.VK_K),		new JavaMapper.DefaultKey("l",KeyEvent.VK_L),
            new JavaMapper.DefaultKey("m",KeyEvent.VK_M),		new JavaMapper.DefaultKey("n",KeyEvent.VK_N),		new JavaMapper.DefaultKey("o",KeyEvent.VK_O),		new JavaMapper.DefaultKey("p",KeyEvent.VK_P),
            new JavaMapper.DefaultKey("q",KeyEvent.VK_Q),		new JavaMapper.DefaultKey("r",KeyEvent.VK_R),		new JavaMapper.DefaultKey("s",KeyEvent.VK_S),		new JavaMapper.DefaultKey("t",KeyEvent.VK_T),
            new JavaMapper.DefaultKey("u",KeyEvent.VK_U),		new JavaMapper.DefaultKey("v",KeyEvent.VK_V),		new JavaMapper.DefaultKey("w",KeyEvent.VK_W),		new JavaMapper.DefaultKey("x",KeyEvent.VK_X),
            new JavaMapper.DefaultKey("y",KeyEvent.VK_Y),		new JavaMapper.DefaultKey("z",KeyEvent.VK_Z),		new JavaMapper.DefaultKey("space",KeyEvent.VK_SPACE),
    
            new JavaMapper.DefaultKey("esc",KeyEvent.VK_ESCAPE),	        new JavaMapper.DefaultKey("equals",KeyEvent.VK_EQUALS),		new JavaMapper.DefaultKey("grave",KeyEvent.VK_BACK_QUOTE),
            new JavaMapper.DefaultKey("tab",KeyEvent.VK_TAB),		        new JavaMapper.DefaultKey("enter",KeyEvent.VK_ENTER),		new JavaMapper.DefaultKey("bspace",KeyEvent.VK_BACK_SPACE),
            new JavaMapper.DefaultKey("lbracket",KeyEvent.VK_OPEN_BRACKET),new JavaMapper.DefaultKey("rbracket",KeyEvent.VK_CLOSE_BRACKET),
            new JavaMapper.DefaultKey("minus",KeyEvent.VK_MINUS),	        new JavaMapper.DefaultKey("capslock",KeyEvent.VK_CAPS_LOCK),	new JavaMapper.DefaultKey("semicolon",KeyEvent.VK_SEMICOLON),
            new JavaMapper.DefaultKey("quote", KeyEvent.VK_QUOTE),	    new JavaMapper.DefaultKey("backslash",KeyEvent.VK_BACK_SLASH),	new JavaMapper.DefaultKey("lshift",KeyEvent.VK_SHIFT, true, false, false),
            new JavaMapper.DefaultKey("rshift",KeyEvent.VK_SHIFT, false, true, false),	    new JavaMapper.DefaultKey("lalt",KeyEvent.VK_ALT, true, false, false),			new JavaMapper.DefaultKey("ralt",KeyEvent.VK_ALT, false, true, false),
            new JavaMapper.DefaultKey("lctrl",KeyEvent.VK_CONTROL, true, false, false),	        new JavaMapper.DefaultKey("rctrl",KeyEvent.VK_CONTROL, false, true, false),		    new JavaMapper.DefaultKey("comma",KeyEvent.VK_COMMA),
            new JavaMapper.DefaultKey("period",KeyEvent.VK_PERIOD),	    new JavaMapper.DefaultKey("slash",KeyEvent.VK_SLASH),		    new JavaMapper.DefaultKey("printscreen",KeyEvent.VK_PRINTSCREEN),
            new JavaMapper.DefaultKey("scrolllock",KeyEvent.VK_SCROLL_LOCK),new JavaMapper.DefaultKey("pause",KeyEvent.VK_PAUSE),		    new JavaMapper.DefaultKey("pagedown",KeyEvent.VK_PAGE_DOWN),
            new JavaMapper.DefaultKey("pageup",KeyEvent.VK_PAGE_UP),	    new JavaMapper.DefaultKey("insert",KeyEvent.VK_INSERT),		new JavaMapper.DefaultKey("home",KeyEvent.VK_HOME),
            new JavaMapper.DefaultKey("delete",KeyEvent.VK_DELETE),	    new JavaMapper.DefaultKey("end",KeyEvent.VK_END),			    new JavaMapper.DefaultKey("up",KeyEvent.VK_UP),
            new JavaMapper.DefaultKey("left",KeyEvent.VK_LEFT),		    new JavaMapper.DefaultKey("down",KeyEvent.VK_DOWN),			new JavaMapper.DefaultKey("right",KeyEvent.VK_RIGHT),
            new JavaMapper.DefaultKey("kp_0",KeyEvent.VK_NUMPAD0),	        new JavaMapper.DefaultKey("kp_1",KeyEvent.VK_NUMPAD1),	        new JavaMapper.DefaultKey("kp_2",KeyEvent.VK_NUMPAD2),	new JavaMapper.DefaultKey("kp_3",KeyEvent.VK_NUMPAD3),
            new JavaMapper.DefaultKey("kp_4",KeyEvent.VK_NUMPAD4),	        new JavaMapper.DefaultKey("kp_5",KeyEvent.VK_NUMPAD5),	        new JavaMapper.DefaultKey("kp_6",KeyEvent.VK_NUMPAD6),	new JavaMapper.DefaultKey("kp_7",KeyEvent.VK_NUMPAD7),
            new JavaMapper.DefaultKey("kp_8",KeyEvent.VK_NUMPAD8),	        new JavaMapper.DefaultKey("kp_9",KeyEvent.VK_NUMPAD9),	        new JavaMapper.DefaultKey("numlock",KeyEvent.VK_NUM_LOCK),
            new JavaMapper.DefaultKey("kp_divide",KeyEvent.VK_DIVIDE, false, false, true),	new JavaMapper.DefaultKey("kp_multiply",KeyEvent.VK_MULTIPLY, false, false, true),
            new JavaMapper.DefaultKey("kp_minus",KeyEvent.VK_SUBTRACT, false, false, true),	new JavaMapper.DefaultKey("kp_plus",KeyEvent.VK_ADD),
            new JavaMapper.DefaultKey("kp_period",KeyEvent.VK_PERIOD, false, false, true),	new JavaMapper.DefaultKey("kp_enter",KeyEvent.VK_ENTER, false, false, true),
            new JavaMapper.DefaultKey("lessthan",KeyEvent.VK_LESS)
        };
}
