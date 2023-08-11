package jdos.sdl;

import jdos.gui.KeyboardKey;
import jdos.gui.Mapper;
import jdos.hardware.Keyboard;
import jdos.misc.Log;
import jdos.misc.setup.Prop_path;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.util.FileIOFactory;
import jdos.util.StringHelper;
import jdos.util.StringRef;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Vector;

public class JavaMapper {
    private static final int CLR_BLACK=0;
	private static final int CLR_WHITE=1;
	private static final int CLR_RED=2;
	private static final int CLR_BLUE=3;
	private static final int CLR_GREEN=4;

    private static final int BMOD_Mod1 = 0x0001;
    private static final int BMOD_Mod2 = 0x0002;
    private static final int BMOD_Mod3 = 0x0004;

    private static final int BFLG_Hold = 0x0001;
    private static final int BFLG_Repeat = 0x0004;
    private static final int BFLG_Right = 0x0010;
    private static final int BFLG_Left = 0x0020;
    private static final int BFLG_Numpad = 0x0040;

    private static final int BC_Mod1 = 1;
    private static final int BC_Mod2 = 2;
    private static final int BC_Mod3 = 3;
	private static final int BC_Hold = 4;

    private static final int BB_Next = 1;
    private static final int BB_Add = 2;
    private static final int BB_Del = 3;
	private static final int BB_Save = 4;
    private static final int BB_Exit = 5;

    public static boolean autofire = false;

    static final private Vector<CEvent> events = new Vector<CEvent>();
    static final private Vector<CButton> buttons = new Vector<CButton>();
    static final private Vector<CBindGroup> bindgroups = new Vector<CBindGroup>();
    static final private Vector<CHandlerEvent> handlergroup = new Vector<CHandlerEvent>();

    final private static class CBindList extends Vector<CBind>{}
    static private CEventButton last_clicked = null;
    static private CKeyEvent caps_lock_event = null;
    static private CKeyEvent num_lock_event = null;

    static private int MapSDLCode(int skey) {
	    return skey;
    }

    static private int GetKeyCode(int keysym) {
        return keysym;
    }

    static private String SDL_GetKeyName(int keysym) {
        return "";
    }

    static private class CBindBut {
        CCaptionButton event_title;
        CCaptionButton bind_title;
        CCaptionButton selected;
        CCaptionButton action;
        CBindButton save;
        CBindButton exit;
        CBindButton add;
        CBindButton del;
        CBindButton next;
        CCheckButton mod1;
        CCheckButton mod2;
        CCheckButton mod3;
        CCheckButton hold;
    }

    static private final CBindBut bind_but = new CBindBut();

    static CBindList holdlist = new CBindList();
    static public final CMapper mapper = new CMapper();
    static public String mapperfile = "mapper.txt";

    static public class CMapper {
        //SDL_Surface * surface;
        //SDL_Surface * draw_surface;
        boolean exit;
        CEvent aevent;				//Active Event
        CBind abind;					//Active Bind
        int abindit;			//Location of active bind in list
        boolean redraw;
        boolean addbind;
        public int mods;
        /*
        struct {
            Bitu num_groups,num;
            CStickBindGroup * stick[MAXSTICKS];
        } sticks;
        */
        String filename;
    }

    static private abstract class CEvent {
        public CEvent(String _entry) {
            entry=_entry;
            events.add(this);
            bindlist.clear();
            activity=0;
            current_value=0;
        }
        public void AddBind(CBind bind) {
            bindlist.add(bind);
	        bind.event=this;
        }

        public void destroy() {}
        public abstract void Active(boolean yesno);
        public abstract void ActivateEvent(boolean ev_trigger,boolean skip_action);
        public abstract void DeActivateEvent(boolean ev_trigger);

        public void DeActivateAll() {
            for (CBind bit : bindlist) {
                bit.DeActivateBind(true);
            }
        }

        public void SetValue(/*Bits*/int value){
            current_value=value;
        }

        public /*Bits*/int GetValue() {
            return current_value;
        }

        public String GetName() { return entry; }
        public abstract boolean IsTrigger();
        CBindList bindlist = new CBindList();

        /*Bitu*/int activity;
        String entry;
        /*Bits*/int current_value;
    }

    static abstract private class CBindGroup {
        public CBindGroup() {
            bindgroups.add(this);
        }
        public void destroy() {
        }
        void ActivateBindList(CBindList list,Object key, int value,boolean ev_trigger) {
            int validmod=0;
            for (CBind it : list) {
                if ((it.mods & mapper.mods) == it.mods) {
                    if (validmod<it.mods) validmod=it.mods;
                }
            }
            for (CBind it: list) {
            /*BUG:CRASH if keymapper key is removed*/
                if (validmod==it.mods && (!it.isLeft() && !it.isRight() && !it.isNumpad()) || (it.isLeft() && KeyboardKey.isLeft(key)) || (it.isRight() && KeyboardKey.isRight(key)) || (it.isNumpad() && KeyboardKey.isNumPad(key)))
                    it.ActivateBind(value,ev_trigger);
            }
        }
        void DeactivateBindList(CBindList list,boolean ev_trigger) {
            for (CBind it : list) {
                it.DeActivateBind(ev_trigger);
            }
        }
        public abstract CBind CreateConfigBind(StringRef buf);
        public abstract CBind CreateEventBind(Object event);

        public abstract boolean CheckEvent(Object event);
        public abstract String ConfigStart();
        public abstract String BindStart();
    }

    static private class CKeyBind extends CBind {
        public CKeyBind(CBindList _list,int _key) {
            super(_list);
            key = _key;
        }
        public String BindName() {
            return "Key "+SDL_GetKeyName(MapSDLCode(key));
        }
        public String ConfigName() {
            return "key "+MapSDLCode(key);
        }
        public int key;
    }

    static private class CKeyBindGroup extends CBindGroup {
        public CKeyBindGroup(int _keys) {
            lists=new CBindList[_keys];
            for (int i=0;i<_keys;i++) lists[i] = new CBindList();
            keys=_keys;
            configname="key";
        }

        public CBind CreateConfigBind(StringRef buf) {
            if (!buf.value.startsWith(configname)) return null;
            StringHelper.StripWord(buf);String num=StringHelper.StripWord(buf);
            int code;
            try {code = Integer.parseInt(num);} catch (Exception e){code=0;}
            //if (usescancodes) {
            //    if (code<MAX_SDLKEYS) code=scancode_map[code];
            //    else code=0;
            //}
            CBind bind=CreateKeyBind(code);
            return bind;
        }
        public CBind CreateEventBind(Object event) {
            if (!KeyboardKey.isPressed(event)) return null;
            return CreateKeyBind(GetKeyCode(KeyboardKey.getKeyCode(event)));
        }

        public boolean CheckEvent(Object event) {
            if (!KeyboardKey.isPressed(event) && !KeyboardKey.isReleased(event)) return false;
            int key=GetKeyCode(KeyboardKey.getKeyCode(event));
//		LOG_MSG("key type %i is %x [%x %x]",event->type,key,event->key.keysym.sym,event->key.keysym.scancode);
            //assert(Bitu(event->key.keysym.sym)<keys);
            if (KeyboardKey.isPressed(event)) ActivateBindList(lists[key],event,0x7fff,true);
            else DeactivateBindList(lists[key],true);
            return false;
        }

        public CBind CreateKeyBind(int _key) {
            //if (!usescancodes) assert(_key<keys);
            return new CKeyBind(lists[_key],_key);
        }
        public String ConfigStart() {
            return configname;
        }
        public String BindStart() {
            return "Key";
        }

        String configname;
        protected CBindList[] lists;
        protected int keys;
    }

    abstract private static class CBind {
        public CBind(CBindList _list) {
            list=_list;
            _list.add(this);
            mods=flags=0;
            event=null;
            active=holding=false;
        }
        public void destroy() {
            list.remove(this);
    //		event->bindlist.remove(this);
        }
        public String AddFlags() {
            String buf = "";
            if ((mods & BMOD_Mod1)!=0) buf+=" mod1";
            if ((mods & BMOD_Mod2)!=0) buf+=" mod2";
            if ((mods & BMOD_Mod3)!=0) buf+=" mod3";
            if ((flags & BFLG_Hold)!=0) buf+=" hold";
            if ((flags & BFLG_Right)!=0) buf+=" right";
            if ((flags & BFLG_Left)!=0) buf+=" left";
            if ((flags & BFLG_Numpad)!=0) buf+=" numpad";
            return buf;
        }
        void SetFlags(String buf) {
            StringRef line = new StringRef(buf);
            while (true) {
                if (line.value.length()==0)
                    break;
                String word = StringHelper.StripWord(line);
                if (word.equalsIgnoreCase("mod1")) mods|=BMOD_Mod1;
                if (word.equalsIgnoreCase("mod2")) mods|=BMOD_Mod2;
                if (word.equalsIgnoreCase("mod3")) mods|=BMOD_Mod3;
                if (word.equalsIgnoreCase("hold")) flags|=BFLG_Hold;
                if (word.equalsIgnoreCase("right")) flags|=BFLG_Right;
                if (word.equalsIgnoreCase("left")) flags|=BFLG_Left;
                if (word.equalsIgnoreCase("numpad")) flags|=BFLG_Numpad;
            }
        }
        void ActivateBind(/*Bits*/int _value,boolean ev_trigger) {
            ActivateBind(_value, ev_trigger, false);
        }
        void ActivateBind(/*Bits*/int _value,boolean ev_trigger,boolean skip_action) {
            if (event.IsTrigger()) {
                /* use value-boundary for on/off events */
                if (_value>25000) {
                    event.SetValue(_value);
                    if (active) return;
                    event.ActivateEvent(ev_trigger,skip_action);
                    active=true;
                } else {
                    if (active) {
                        event.DeActivateEvent(ev_trigger);
                        active=false;
                    }
                }
            } else {
                /* store value for possible later use in the activated event */
                event.SetValue(_value);
                event.ActivateEvent(ev_trigger,false);
            }
        }
        void DeActivateBind(boolean ev_trigger) {
            if (event.IsTrigger()) {
                if (!active) return;
                active=false;
                if ((flags & BFLG_Hold)!=0) {
                    if (!holding) {
                        holdlist.add(this);
                        holding=true;
                        return;
                    } else {
                        holdlist.remove(this);
                        holding=false;
                    }
                }
                event.DeActivateEvent(ev_trigger);
            } else {
                /* store value for possible later use in the activated event */
                event.SetValue(0);
                event.DeActivateEvent(ev_trigger);
            }
        }
        public abstract String ConfigName();
        public abstract String BindName();
        public boolean isLeft() {
            return (flags & BFLG_Left)!=0;
        }
        public boolean isRight() {
            return (flags & BFLG_Right)!=0;
        }
        public boolean isNumpad() {
            return (flags & BFLG_Numpad)!=0;
        }
        /*Bitu*/int mods,flags;
        /*Bit16s*/int value;
        CEvent event;
        CBindList list;
        boolean active,holding;
    }

    /* class for events which can be ON/OFF only: key presses, joystick buttons, joystick hat */
    static abstract private class CTriggeredEvent extends CEvent {
        public CTriggeredEvent(String _entry) {
            super(_entry);
        }
        public boolean IsTrigger() {
            return true;
        }
        public void ActivateEvent(boolean ev_trigger,boolean skip_action) {
            if (current_value>25000) {
                /* value exceeds boundary, trigger event if not active */
                if (activity==0 && !skip_action) Active(true);
                if (activity<32767) activity++;
            } else {
                if (activity>0) {
                    /* untrigger event if it is fully inactive */
                    DeActivateEvent(ev_trigger);
                    activity=0;
                }
            }
        }
        public void DeActivateEvent(boolean ev_trigger) {
            activity--;
            if (activity==0) Active(false);
        }
    }

    static public class CKeyEvent extends CTriggeredEvent {
        public CKeyEvent(String _entry ,int _key) {
            super(_entry);
            key=_key;
        }
        public void Active(boolean yesno) {
            Keyboard.KEYBOARD_AddKey(key, yesno);
        }
        int key;
    }

    static private class CBindButton extends CTextButton {
    public CBindButton(int _x,int _y,int _dx,int _dy,String _text,int _type) {
            super(_x,_y,_dx,_dy,_text);
            type=_type;
        }

        public void Click() {
            switch (type) {
            case BB_Add:
                mapper.addbind=true;
                SetActiveBind(null);
                change_action_text("Press a key/joystick button or move the joystick.",CLR_RED);
                break;
            case BB_Del:
                if (mapper.abindit!=mapper.aevent.bindlist.size())  {
                    mapper.aevent.bindlist.remove(mapper.abindit).destroy();
                    if (mapper.abindit==mapper.aevent.bindlist.size())
                        mapper.abindit=0;
                }
                if (mapper.abindit!=mapper.aevent.bindlist.size()) SetActiveBind(mapper.aevent.bindlist.elementAt(mapper.abindit));
                else SetActiveBind(null);
                break;
            case BB_Next:
                if (mapper.abindit!=mapper.aevent.bindlist.size())
                    mapper.abindit++;
                if (mapper.abindit==mapper.aevent.bindlist.size())
                    mapper.abindit=0;
                SetActiveBind(mapper.aevent.bindlist.elementAt(mapper.abindit));
                break;
            case BB_Save:
                MAPPER_SaveBinds();
                break;
            case BB_Exit:
                mapper.exit=true;
                break;
            }
        }
        protected int type;
    }

    static private class CCheckButton extends CTextButton {
        public CCheckButton(int _x,int _y,int _dx,int _dy,String _text,int _type) {
            super(_x,_y,_dx,_dy,_text);
            type=_type;
        }
        public void Draw() {
            if (!enabled) return;
            boolean checked=false;
            switch (type) {
            case BC_Mod1:
                checked=(mapper.abind.mods & BMOD_Mod1)>0;
                break;
            case BC_Mod2:
                checked=(mapper.abind.mods & BMOD_Mod2)>0;
                break;
            case BC_Mod3:
                checked=(mapper.abind.mods & BMOD_Mod3)>0;
                break;
            case BC_Hold:
                checked=(mapper.abind.flags & BFLG_Hold)>0;
                break;
            }
            if (checked) {
                /*
                Bit8u * point=((Bit8u *)mapper.surface->pixels)+((y+2)*mapper.surface->pitch)+x+dx-dy+2;
                for (Bitu lines=0;lines<(dy-4);lines++)  {
                    memset(point,color,dy-4);
                    point+=mapper.surface->pitch;
                }
                */
            }
            super.Draw();
        }
        public void Click() {
            switch (type) {
            case BC_Mod1:
                mapper.abind.mods^=BMOD_Mod1;
                break;
            case BC_Mod2:
                mapper.abind.mods^=BMOD_Mod2;
                break;
            case BC_Mod3:
                mapper.abind.mods^=BMOD_Mod3;
                break;
            case BC_Hold:
                mapper.abind.flags^=BFLG_Hold;
                break;
            }
            mapper.redraw=true;
        }
        protected int type;
    }

    static private class CModEvent extends CTriggeredEvent {
        public CModEvent(String _entry,int _wmod) {
            super(_entry);
            wmod=_wmod;
        }
        public void Active(boolean yesno) {
            if (yesno) mapper.mods|=(1 << (wmod-1));
            else mapper.mods&=~(1 << (wmod-1));
        }
        protected int wmod;
    }

    public static class CHandlerEvent extends CTriggeredEvent {
        public CHandlerEvent(String _entry, Mapper.MAPPER_Handler _handler,int _key,/*Bitu*/int _mod, String _buttonname) {
            super(_entry);
            handler=_handler;
            defmod=_mod;
            defkey=_key;
            buttonname=_buttonname;
            handlergroup.add(this);
        }
        public void Active(boolean yesno) {
            handler.call(yesno);
        }
        public String ButtonName() {
            return buttonname;
        }
        void MakeDefaultBind(StringRef buf) {
            /*Bitu*/int key=KeyboardKey.translateMapKey(defkey);
            buf.value = entry+" \"key "+key+((defmod & 1)!=0 ? " mod1" : "")+((defmod & 2)!=0 ? " mod2" : "")+((defmod & 4)!=0 ? " mod3" : "")+"\"";
        }
        protected /*MapKeys*/int defkey;
        protected /*Bitu*/int defmod;
        protected Mapper.MAPPER_Handler handler;
        public String buttonname;
    }

    public static class CButton {
        public void destroy() {}
        public CButton(int _x,int _y,int _dx,int _dy) {
            x=_x;y=_y;dx=_dx;dy=_dy;
            buttons.add(this);
            color=CLR_WHITE;
            enabled=true;
        }
        public void Draw() {
            if (!enabled) return;
            /*
            Bit8u * point=((Bit8u *)mapper.surface->pixels)+(y*mapper.surface->pitch)+x;
            for (Bitu lines=0;lines<dy;lines++)  {
                if (lines==0 || lines==(dy-1)) {
                    for (Bitu cols=0;cols<dx;cols++) *(point+cols)=color;
                } else {
                    *point=color;*(point+dx-1)=color;
                }
                point+=mapper.surface->pitch;
            }
            */
        }
        public boolean OnTop(int _x,int _y) {
            return ( enabled && (_x>=x) && (_x<x+dx) && (_y>=y) && (_y<y+dy));
        }
        public void Click() {}
        public void Enable(boolean yes) {
            enabled=yes;
            //mapper.redraw=true;
        }
        public void SetColor(int _col) { color=_col; }

        protected int x,y,dx,dy;
        protected int color;
        protected boolean enabled;
    }

    static public class CCaptionButton extends CButton {
        public CCaptionButton(int _x,int _y,int _dx,int _dy) {
            super(_x,_y,_dx,_dy);
        }
        public void Change(String format, Object[] args) {
            caption = StringHelper.sprintf(format, args);
            mapper.redraw=true;
        }

        public void Draw() {
            if (!enabled) return;
            //DrawText(x+2,y+2,caption,color);
        }
        protected String caption;
    }

    public static class CTextButton extends CButton {
	    public CTextButton(int _x,int _y,int _dx,int _dy,String _text) {
            super(_x,_y,_dx,_dy);
            text=_text;
        }
	    public void Draw() {
		    if (!enabled) return;
		    super.Draw();
		    //DrawText(x+2,y+2,text,color);
	    }
	    protected String text;
    }

    public static class CEventButton extends CTextButton {
        public CEventButton(int _x,int _y,int _dx,int _dy,String _text,CEvent _event) {
            super(_x,_y,_dx,_dy,_text);
            event=_event;
        }

        public void Click() {
            if (last_clicked!=null) last_clicked.SetColor(CLR_WHITE);
            this.SetColor(CLR_GREEN);
            SetActiveEvent(event);
            last_clicked=this;
        }
        protected CEvent event;
    }

    static private void change_action_text(String text,int col) {
        bind_but.action.Change(text,null);
        bind_but.action.SetColor(col);
    }

    static private void SetActiveBind(CBind _bind) {
        mapper.abind=_bind;
        if (_bind != null) {
            bind_but.bind_title.Enable(true);
            bind_but.bind_title.Change("BIND:"+_bind.BindName(), null);
            bind_but.del.Enable(true);
            bind_but.next.Enable(true);
            bind_but.mod1.Enable(true);
            bind_but.mod2.Enable(true);
            bind_but.mod3.Enable(true);
            bind_but.hold.Enable(true);
        } else {
            bind_but.bind_title.Enable(false);
            bind_but.del.Enable(false);
            bind_but.next.Enable(false);
            bind_but.mod1.Enable(false);
            bind_but.mod2.Enable(false);
            bind_but.mod3.Enable(false);
            bind_but.hold.Enable(false);
        }
    }

    static void SetActiveEvent(CEvent event) {
        mapper.aevent=event;
        mapper.redraw=true;
        mapper.addbind=false;
        bind_but.event_title.Change("EVENT:"+((event!=null) ? event.GetName(): "none"), null);
        if (event==null) {
            change_action_text("Select an event to change.",CLR_WHITE);
            bind_but.add.Enable(false);
            SetActiveBind(null);
        } else {
            change_action_text("Select a different event or hit the Add/Del/Next buttons.",CLR_WHITE);
            mapper.abindit=0;
            if (mapper.abindit!=event.bindlist.size()) {
                SetActiveBind(event.bindlist.elementAt(mapper.abindit));
            } else SetActiveBind(null);
            bind_but.add.Enable(true);
        }
    }

    static private final int BW = 28;
    static private final int BH = 20;
    static private final int DX = 5;

    static private int PX(int x) {return ((x)*BW + DX);}
    static private int PY(int y) {return (10+(y)*BH);}

    static CKeyEvent AddKeyButtonEvent(int x,int y,int dx,int dy,String title,String entry,int key) {
        CKeyEvent event=new CKeyEvent("key_"+entry,key);
        new CEventButton(x,y,dx,dy,title,event);
        return event;
    }

    static private void AddModButton(int x,int y,int dx,int dy,String title,int _mod) {
        CModEvent event=new CModEvent("mod_"+_mod,_mod);
        new CEventButton(x,y,dx,dy,title,event);
    }

    static private class KeyBlock {
        public KeyBlock(String title, String entry, int key) {
            this.title = title;
            this.entry = entry;
            this.key = key;
        }
        String title;
        String entry;
        int key;
    }
    static private final KeyBlock[] combo_f = new KeyBlock [] {
        new KeyBlock("F1","f1",Keyboard.KBD_KEYS.KBD_f1),		new KeyBlock("F2","f2",Keyboard.KBD_KEYS.KBD_f2),		new KeyBlock("F3","f3",Keyboard.KBD_KEYS.KBD_f3),
        new KeyBlock("F4","f4",Keyboard.KBD_KEYS.KBD_f4),		new KeyBlock("F5","f5",Keyboard.KBD_KEYS.KBD_f5),		new KeyBlock("F6","f6",Keyboard.KBD_KEYS.KBD_f6),
        new KeyBlock("F7","f7",Keyboard.KBD_KEYS.KBD_f7),		new KeyBlock("F8","f8",Keyboard.KBD_KEYS.KBD_f8),		new KeyBlock("F9","f9",Keyboard.KBD_KEYS.KBD_f9),
        new KeyBlock("F10","f10",Keyboard.KBD_KEYS.KBD_f10),	new KeyBlock("F11","f11",Keyboard.KBD_KEYS.KBD_f11),	new KeyBlock("F12","f12",Keyboard.KBD_KEYS.KBD_f12),
    };

    static private final KeyBlock[] combo_1 = new KeyBlock[] {
        new KeyBlock("`~","grave",Keyboard.KBD_KEYS.KBD_grave),	new KeyBlock("1!","1",Keyboard.KBD_KEYS.KBD_1),	new KeyBlock("2@","2",Keyboard.KBD_KEYS.KBD_2),
        new KeyBlock("3#","3",Keyboard.KBD_KEYS.KBD_3),			new KeyBlock("4$","4",Keyboard.KBD_KEYS.KBD_4),	new KeyBlock("5%","5",Keyboard.KBD_KEYS.KBD_5),
        new KeyBlock("6^","6",Keyboard.KBD_KEYS.KBD_6),			new KeyBlock("7&","7",Keyboard.KBD_KEYS.KBD_7),	new KeyBlock("8*","8",Keyboard.KBD_KEYS.KBD_8),
        new KeyBlock("9(","9",Keyboard.KBD_KEYS.KBD_9),			new KeyBlock("0)","0",Keyboard.KBD_KEYS.KBD_0),	new KeyBlock("-_","minus",Keyboard.KBD_KEYS.KBD_minus),
        new KeyBlock("=+","equals",Keyboard.KBD_KEYS.KBD_equals),	new KeyBlock("\u001B","bspace",Keyboard.KBD_KEYS.KBD_backspace),
    };

    static private final KeyBlock[] combo_2 = new KeyBlock[] {
        new KeyBlock("q","q",Keyboard.KBD_KEYS.KBD_q),			new KeyBlock("w","w",Keyboard.KBD_KEYS.KBD_w),	new KeyBlock("e","e",Keyboard.KBD_KEYS.KBD_e),
        new KeyBlock("r","r",Keyboard.KBD_KEYS.KBD_r),			new KeyBlock("t","t",Keyboard.KBD_KEYS.KBD_t),	new KeyBlock("y","y",Keyboard.KBD_KEYS.KBD_y),
        new KeyBlock("u","u",Keyboard.KBD_KEYS.KBD_u),			new KeyBlock("i","i",Keyboard.KBD_KEYS.KBD_i),	new KeyBlock("o","o",Keyboard.KBD_KEYS.KBD_o),	
        new KeyBlock("p","p",Keyboard.KBD_KEYS.KBD_p),			new KeyBlock("[","lbracket",Keyboard.KBD_KEYS.KBD_leftbracket),	
        new KeyBlock("]","rbracket",Keyboard.KBD_KEYS.KBD_rightbracket),	
    };

    static private final KeyBlock[] combo_3 = new KeyBlock[] {
        new KeyBlock("a","a",Keyboard.KBD_KEYS.KBD_a),			new KeyBlock("s","s",Keyboard.KBD_KEYS.KBD_s),	new KeyBlock("d","d",Keyboard.KBD_KEYS.KBD_d),
        new KeyBlock("f","f",Keyboard.KBD_KEYS.KBD_f),			new KeyBlock("g","g",Keyboard.KBD_KEYS.KBD_g),	new KeyBlock("h","h",Keyboard.KBD_KEYS.KBD_h),
        new KeyBlock("j","j",Keyboard.KBD_KEYS.KBD_j),			new KeyBlock("k","k",Keyboard.KBD_KEYS.KBD_k),	new KeyBlock("l","l",Keyboard.KBD_KEYS.KBD_l),
        new KeyBlock(";","semicolon",Keyboard.KBD_KEYS.KBD_semicolon),				new KeyBlock("'","quote",Keyboard.KBD_KEYS.KBD_quote),
        new KeyBlock("\\","backslash",Keyboard.KBD_KEYS.KBD_backslash),	
    };

    static private final  KeyBlock[] combo_4 = new KeyBlock[] {
        new KeyBlock("<","lessthan",Keyboard.KBD_KEYS.KBD_extra_lt_gt),
        new KeyBlock("z","z",Keyboard.KBD_KEYS.KBD_z),			new KeyBlock("x","x",Keyboard.KBD_KEYS.KBD_x),	new KeyBlock("c","c",Keyboard.KBD_KEYS.KBD_c),
        new KeyBlock("v","v",Keyboard.KBD_KEYS.KBD_v),			new KeyBlock("b","b",Keyboard.KBD_KEYS.KBD_b),	new KeyBlock("n","n",Keyboard.KBD_KEYS.KBD_n),
        new KeyBlock("m","m",Keyboard.KBD_KEYS.KBD_m),			new KeyBlock(",","comma",Keyboard.KBD_KEYS.KBD_comma),
        new KeyBlock(".","period",Keyboard.KBD_KEYS.KBD_period),						new KeyBlock("/","slash",Keyboard.KBD_KEYS.KBD_slash),		
    };
    
    static void CreateLayout() {
        int i;
        /* Create the buttons for the Keyboard */

        AddKeyButtonEvent(PX(0),PY(0),BW,BH,"ESC","esc", Keyboard.KBD_KEYS.KBD_esc);
        for (i=0;i<12;i++) AddKeyButtonEvent(PX(2+i),PY(0),BW,BH,combo_f[i].title,combo_f[i].entry,combo_f[i].key);
        for (i=0;i<14;i++) AddKeyButtonEvent(PX(  i),PY(1),BW,BH,combo_1[i].title,combo_1[i].entry,combo_1[i].key);

        AddKeyButtonEvent(PX(0),PY(2),BW*2,BH,"TAB","tab",Keyboard.KBD_KEYS.KBD_tab);
        for (i=0;i<12;i++) AddKeyButtonEvent(PX(2+i),PY(2),BW,BH,combo_2[i].title,combo_2[i].entry,combo_2[i].key);

        AddKeyButtonEvent(PX(14),PY(2),BW*2,BH*2,"ENTER","enter",Keyboard.KBD_KEYS.KBD_enter);

        caps_lock_event=AddKeyButtonEvent(PX(0),PY(3),BW*2,BH,"CLCK","capslock",Keyboard.KBD_KEYS.KBD_capslock);
        for (i=0;i<12;i++) AddKeyButtonEvent(PX(2+i),PY(3),BW,BH,combo_3[i].title,combo_3[i].entry,combo_3[i].key);

        AddKeyButtonEvent(PX(0),PY(4),BW*2,BH,"SHIFT","lshift",Keyboard.KBD_KEYS.KBD_leftshift);
        for (i=0;i<11;i++) AddKeyButtonEvent(PX(2+i),PY(4),BW,BH,combo_4[i].title,combo_4[i].entry,combo_4[i].key);
        AddKeyButtonEvent(PX(13),PY(4),BW*3,BH,"SHIFT","rshift",Keyboard.KBD_KEYS.KBD_rightshift);

        /* Last Row */
        AddKeyButtonEvent(PX(0) ,PY(5),BW*2,BH,"CTRL","lctrl",Keyboard.KBD_KEYS.KBD_leftctrl);
        AddKeyButtonEvent(PX(3) ,PY(5),BW*2,BH,"ALT","lalt",Keyboard.KBD_KEYS.KBD_leftalt);
        AddKeyButtonEvent(PX(5) ,PY(5),BW*6,BH,"SPACE","space",Keyboard.KBD_KEYS.KBD_space);
        AddKeyButtonEvent(PX(11),PY(5),BW*2,BH,"ALT","ralt",Keyboard.KBD_KEYS.KBD_rightalt);
        AddKeyButtonEvent(PX(14),PY(5),BW*2,BH,"CTRL","rctrl",Keyboard.KBD_KEYS.KBD_rightctrl);

        /* Arrow Keys */
        int XO = 17;
        int YO = 0;

        AddKeyButtonEvent(PX(XO+0),PY(YO),BW,BH,"PRT","printscreen",Keyboard.KBD_KEYS.KBD_printscreen);
        AddKeyButtonEvent(PX(XO+1),PY(YO),BW,BH,"SCL","scrolllock",Keyboard.KBD_KEYS.KBD_scrolllock);
        AddKeyButtonEvent(PX(XO+2),PY(YO),BW,BH,"PAU","pause",Keyboard.KBD_KEYS.KBD_pause);
        AddKeyButtonEvent(PX(XO+0),PY(YO+1),BW,BH,"INS","insert",Keyboard.KBD_KEYS.KBD_insert);
        AddKeyButtonEvent(PX(XO+1),PY(YO+1),BW,BH,"HOM","home",Keyboard.KBD_KEYS.KBD_home);
        AddKeyButtonEvent(PX(XO+2),PY(YO+1),BW,BH,"PUP","pageup",Keyboard.KBD_KEYS.KBD_pageup);
        AddKeyButtonEvent(PX(XO+0),PY(YO+2),BW,BH,"DEL","delete",Keyboard.KBD_KEYS.KBD_delete);
        AddKeyButtonEvent(PX(XO+1),PY(YO+2),BW,BH,"END","end",Keyboard.KBD_KEYS.KBD_end);
        AddKeyButtonEvent(PX(XO+2),PY(YO+2),BW,BH,"PDN","pagedown",Keyboard.KBD_KEYS.KBD_pagedown);
        AddKeyButtonEvent(PX(XO+1),PY(YO+4),BW,BH,"\u0018","up",Keyboard.KBD_KEYS.KBD_up);
        AddKeyButtonEvent(PX(XO+0),PY(YO+5),BW,BH,"\u001B","left",Keyboard.KBD_KEYS.KBD_left);
        AddKeyButtonEvent(PX(XO+1),PY(YO+5),BW,BH,"\u0019","down",Keyboard.KBD_KEYS.KBD_down);
        AddKeyButtonEvent(PX(XO+2),PY(YO+5),BW,BH,"\u001A","right",Keyboard.KBD_KEYS.KBD_right);

        XO = 0;
        YO = 7;
        /* Numeric KeyPad */
        num_lock_event=AddKeyButtonEvent(PX(XO),PY(YO),BW,BH,"NUM","numlock",Keyboard.KBD_KEYS.KBD_numlock);
        AddKeyButtonEvent(PX(XO+1),PY(YO),BW,BH,"/","kp_divide",Keyboard.KBD_KEYS.KBD_kpdivide);
        AddKeyButtonEvent(PX(XO+2),PY(YO),BW,BH,"*","kp_multiply",Keyboard.KBD_KEYS.KBD_kpmultiply);
        AddKeyButtonEvent(PX(XO+3),PY(YO),BW,BH,"-","kp_minus",Keyboard.KBD_KEYS.KBD_kpminus);
        AddKeyButtonEvent(PX(XO+0),PY(YO+1),BW,BH,"7","kp_7",Keyboard.KBD_KEYS.KBD_kp7);
        AddKeyButtonEvent(PX(XO+1),PY(YO+1),BW,BH,"8","kp_8",Keyboard.KBD_KEYS.KBD_kp8);
        AddKeyButtonEvent(PX(XO+2),PY(YO+1),BW,BH,"9","kp_9",Keyboard.KBD_KEYS.KBD_kp9);
        AddKeyButtonEvent(PX(XO+3),PY(YO+1),BW,BH*2,"+","kp_plus",Keyboard.KBD_KEYS.KBD_kpplus);
        AddKeyButtonEvent(PX(XO),PY(YO+2),BW,BH,"4","kp_4",Keyboard.KBD_KEYS.KBD_kp4);
        AddKeyButtonEvent(PX(XO+1),PY(YO+2),BW,BH,"5","kp_5",Keyboard.KBD_KEYS.KBD_kp5);
        AddKeyButtonEvent(PX(XO+2),PY(YO+2),BW,BH,"6","kp_6",Keyboard.KBD_KEYS.KBD_kp6);
        AddKeyButtonEvent(PX(XO+0),PY(YO+3),BW,BH,"1","kp_1",Keyboard.KBD_KEYS.KBD_kp1);
        AddKeyButtonEvent(PX(XO+1),PY(YO+3),BW,BH,"2","kp_2",Keyboard.KBD_KEYS.KBD_kp2);
        AddKeyButtonEvent(PX(XO+2),PY(YO+3),BW,BH,"3","kp_3",Keyboard.KBD_KEYS.KBD_kp3);
        AddKeyButtonEvent(PX(XO+3),PY(YO+3),BW,BH*2,"ENT","kp_enter",Keyboard.KBD_KEYS.KBD_kpenter);
        AddKeyButtonEvent(PX(XO),PY(YO+4),BW*2,BH,"0","kp_0",Keyboard.KBD_KEYS.KBD_kp0);
        AddKeyButtonEvent(PX(XO+2),PY(YO+4),BW,BH,".","kp_period",Keyboard.KBD_KEYS.KBD_kpperiod);

        XO = 10;
        YO = 8;

//        /* Joystick Buttons/Texts */
//        /* Buttons 1+2 of 1st Joystick */
//        AddJButtonButton(PX(XO),PY(YO),BW,BH,"1" ,0,0);
//        AddJButtonButton(PX(XO+2),PY(YO),BW,BH,"2" ,0,1);
//        /* Axes 1+2 (X+Y) of 1st Joystick */
//        CJAxisEvent * cjaxis=AddJAxisButton(PX(XO+1),PY(YO),BW,BH,"Y-",0,1,false,NULL);
//        AddJAxisButton  (PX(XO+1),PY(YO+1),BW,BH,"Y+",0,1,true,cjaxis);
//        cjaxis=AddJAxisButton  (PX(XO),PY(YO+1),BW,BH,"X-",0,0,false,NULL);
//        AddJAxisButton  (PX(XO+2),PY(YO+1),BW,BH,"X+",0,0,true,cjaxis);
//
//        if (joytype==JOY_2AXIS) {
//            /* Buttons 1+2 of 2nd Joystick */
//            AddJButtonButton(PX(XO+4),PY(YO),BW,BH,"1" ,1,0);
//            AddJButtonButton(PX(XO+4+2),PY(YO),BW,BH,"2" ,1,1);
//            /* Buttons 3+4 of 1st Joystick, not accessible */
//            AddJButtonButton_hidden(0,2);
//            AddJButtonButton_hidden(0,3);
//
//            /* Axes 1+2 (X+Y) of 2nd Joystick */
//            cjaxis=	AddJAxisButton(PX(XO+4),PY(YO+1),BW,BH,"X-",1,0,false,NULL);
//                    AddJAxisButton(PX(XO+4+2),PY(YO+1),BW,BH,"X+",1,0,true,cjaxis);
//            cjaxis=	AddJAxisButton(PX(XO+4+1),PY(YO+0),BW,BH,"Y-",1,1,false,NULL);
//                    AddJAxisButton(PX(XO+4+1),PY(YO+1),BW,BH,"Y+",1,1,true,cjaxis);
//            /* Axes 3+4 (X+Y) of 1st Joystick, not accessible */
//            cjaxis=	AddJAxisButton_hidden(0,2,false,NULL);
//                    AddJAxisButton_hidden(0,2,true,cjaxis);
//            cjaxis=	AddJAxisButton_hidden(0,3,false,NULL);
//                    AddJAxisButton_hidden(0,3,true,cjaxis);
//        } else {
//            /* Buttons 3+4 of 1st Joystick */
//            AddJButtonButton(PX(XO+4),PY(YO),BW,BH,"3" ,0,2);
//            AddJButtonButton(PX(XO+4+2),PY(YO),BW,BH,"4" ,0,3);
//            /* Buttons 1+2 of 2nd Joystick, not accessible */
//            AddJButtonButton_hidden(1,0);
//            AddJButtonButton_hidden(1,1);
//
//            /* Axes 3+4 (X+Y) of 1st Joystick */
//            cjaxis=	AddJAxisButton(PX(XO+4),PY(YO+1),BW,BH,"X-",0,2,false,NULL);
//                    AddJAxisButton(PX(XO+4+2),PY(YO+1),BW,BH,"X+",0,2,true,cjaxis);
//            cjaxis=	AddJAxisButton(PX(XO+4+1),PY(YO+0),BW,BH,"Y-",0,3,false,NULL);
//                    AddJAxisButton(PX(XO+4+1),PY(YO+1),BW,BH,"Y+",0,3,true,cjaxis);
//            /* Axes 1+2 (X+Y) of 2nd Joystick , not accessible*/
//            cjaxis=	AddJAxisButton_hidden(1,0,false,NULL);
//                    AddJAxisButton_hidden(1,0,true,cjaxis);
//            cjaxis=	AddJAxisButton_hidden(1,1,false,NULL);
//                    AddJAxisButton_hidden(1,1,true,cjaxis);
//        }
//
//        if (joytype==JOY_CH) {
//            /* Buttons 5+6 of 1st Joystick */
//            AddJButtonButton(PX(XO+8),PY(YO),BW,BH,"5" ,0,4);
//            AddJButtonButton(PX(XO+8+2),PY(YO),BW,BH,"6" ,0,5);
//        } else {
//            /* Buttons 5+6 of 1st Joystick, not accessible */
//            AddJButtonButton_hidden(0,4);
//            AddJButtonButton_hidden(0,5);
//        }
//
//        /* Hat directions up, left, down, right */
//        AddJHatButton(PX(XO+8+1),PY(YO),BW,BH,"UP",0,0,0);
//        AddJHatButton(PX(XO+8+0),PY(YO+1),BW,BH,"LFT",0,0,3);
//        AddJHatButton(PX(XO+8+1),PY(YO+1),BW,BH,"DWN",0,0,2);
//        AddJHatButton(PX(XO+8+2),PY(YO+1),BW,BH,"RGT",0,0,1);
//
//        /* Labels for the joystick */
//        if (joytype ==JOY_2AXIS) {
//            new CTextButton(PX(XO+0),PY(YO-1),3*BW,20,"Joystick 1");
//            new CTextButton(PX(XO+4),PY(YO-1),3*BW,20,"Joystick 2");
//            new CTextButton(PX(XO+8),PY(YO-1),3*BW,20,"Disabled");
//        } else if(joytype ==JOY_4AXIS || joytype == JOY_4AXIS_2) {
//            new CTextButton(PX(XO+0),PY(YO-1),3*BW,20,"Axis 1/2");
//            new CTextButton(PX(XO+4),PY(YO-1),3*BW,20,"Axis 3/4");
//            new CTextButton(PX(XO+8),PY(YO-1),3*BW,20,"Disabled");
//        } else if(joytype == JOY_CH) {
//            new CTextButton(PX(XO+0),PY(YO-1),3*BW,20,"Axis 1/2");
//            new CTextButton(PX(XO+4),PY(YO-1),3*BW,20,"Axis 3/4");
//            new CTextButton(PX(XO+8),PY(YO-1),3*BW,20,"Hat/D-pad");
//        } else if ( joytype==JOY_FCS) {
//            new CTextButton(PX(XO+0),PY(YO-1),3*BW,20,"Axis 1/2");
//            new CTextButton(PX(XO+4),PY(YO-1),3*BW,20,"Axis 3");
//            new CTextButton(PX(XO+8),PY(YO-1),3*BW,20,"Hat/D-pad");
//        } else if(joytype == JOY_NONE) {
//            new CTextButton(PX(XO+0),PY(YO-1),3*BW,20,"Disabled");
//            new CTextButton(PX(XO+4),PY(YO-1),3*BW,20,"Disabled");
//            new CTextButton(PX(XO+8),PY(YO-1),3*BW,20,"Disabled");
//        }



        /* The modifier buttons */
        AddModButton(PX(0),PY(14),50,20,"Mod1",1);
        AddModButton(PX(2),PY(14),50,20,"Mod2",2);
        AddModButton(PX(4),PY(14),50,20,"Mod3",3);
        /* Create Handler buttons */
        int xpos=3;int ypos=11;
        for (CHandlerEvent hit : handlergroup) {
            new CEventButton(PX(xpos*3),PY(ypos),BW*3,BH,hit.ButtonName(),hit);
            xpos++;
            if (xpos>6) {
                xpos=3;ypos++;
            }
        }
        /* Create some text buttons */
//	new CTextButton(PX(6),0,124,20,"Keyboard Layout");
//	new CTextButton(PX(17),0,124,20,"Joystick Layout");

        bind_but.action=new CCaptionButton(180,330,0,0);

        bind_but.event_title=new CCaptionButton(0,350,0,0);
        bind_but.bind_title=new CCaptionButton(0,365,0,0);

        /* Create binding support buttons */

        bind_but.mod1=new CCheckButton(20,410,60,20, "mod1",BC_Mod1);
        bind_but.mod2=new CCheckButton(20,432,60,20, "mod2",BC_Mod2);
        bind_but.mod3=new CCheckButton(20,454,60,20, "mod3",BC_Mod3);
        bind_but.hold=new CCheckButton(100,410,60,20,"hold",BC_Hold);

        bind_but.next=new CBindButton(250,400,50,20,"Next",BB_Next);

        bind_but.add=new CBindButton(250,380,50,20,"Add",BB_Add);
        bind_but.del=new CBindButton(300,380,50,20,"Del",BB_Del);

        bind_but.save=new CBindButton(400,450,50,20,"Save",BB_Save);
        bind_but.exit=new CBindButton(450,450,50,20,"Exit",BB_Exit);

        bind_but.bind_title.Change("Bind Title", null);
    }

    public static void CreateStringBind(String in) {
        StringRef line = new StringRef(in.trim());
        String eventname=StringHelper.StripWord(line);
        CEvent event = null;
        for (CEvent it: events) {
            if (it.GetName().equals(eventname)) {
                event = it;
                break;
            }
        }
        if (event == null) {
            Log.log_msg("Can't find matching event for " + eventname);
            return ;
        }
        CBind bind;
        while (true) {
            if (line.value.length()==0)
                break;
            StringRef bindline = new StringRef(StringHelper.StripWord(line));
            for (CBindGroup it : bindgroups) {
                bind = it.CreateConfigBind(bindline);
                if (bind != null) {
                    event.AddBind(bind);
                    bind.SetFlags(bindline.value);
                    break;
                }
            }
        }
    }

    static public class DefaultKey {
        public DefaultKey(String s, int i) {
            eventend = s;
            key = i;
        }
        public DefaultKey(String s, int i, boolean isLeft, boolean isRight, boolean isNumPad) {
            eventend = s;
            key = i;
            this.isRight = isRight;
            this.isLeft = isLeft;
            this.isNumPad = isNumPad;
        }
        String eventend;
        /*Bitu*/int key;
        boolean isRight;
        boolean isLeft;
        boolean isNumPad;
    }


    static void CreateDefaultBinds() {
        for (DefaultKey key : KeyboardKey.DefaultKeys) {
            CreateStringBind("key_"+key.eventend+" \"key "+key.key+((key.isRight?" right":""))+((key.isLeft?" left":""))+((key.isNumPad?" numpad":""))+"\"");
        }
        KeyboardKey.CreateDefaultBinds();
        for (CHandlerEvent it: handlergroup) {
            StringRef buffer = new StringRef();
            it.MakeDefaultBind(buffer);
            CreateStringBind(buffer.value);
        }

//        /* joystick1, buttons 1-6 */
//        CreateStringBind("jbutton_0_0 \"stick_0 button 0\" ");
//        CreateStringBind("jbutton_0_1 \"stick_0 button 1\" ");
//        CreateStringBind("jbutton_0_2 \"stick_0 button 2\" ");
//        CreateStringBind("jbutton_0_3 \"stick_0 button 3\" ");
//        CreateStringBind("jbutton_0_4 \"stick_0 button 4\" ");
//        CreateStringBind("jbutton_0_5 \"stick_0 button 5\" ");
//        /* joystick2, buttons 1-2 */
//        CreateStringBind("jbutton_1_0 \"stick_1 button 0\" ");
//        CreateStringBind("jbutton_1_1 \"stick_1 button 1\" ");
//
//        /* joystick1, axes 1-4 */
//        CreateStringBind("jaxis_0_0- \"stick_0 axis 0 0\" ");
//        CreateStringBind("jaxis_0_0+ \"stick_0 axis 0 1\" ");
//        CreateStringBind("jaxis_0_1- \"stick_0 axis 1 0\" ");
//        CreateStringBind("jaxis_0_1+ \"stick_0 axis 1 1\" ");
//        CreateStringBind("jaxis_0_2- \"stick_0 axis 2 0\" ");
//        CreateStringBind("jaxis_0_2+ \"stick_0 axis 2 1\" ");
//        CreateStringBind("jaxis_0_3- \"stick_0 axis 3 0\" ");
//        CreateStringBind("jaxis_0_3+ \"stick_0 axis 3 1\" ");
//        /* joystick2, axes 1-2 */
//        CreateStringBind("jaxis_1_0- \"stick_1 axis 0 0\" ");
//        CreateStringBind("jaxis_1_0+ \"stick_1 axis 0 1\" ");
//        CreateStringBind("jaxis_1_1- \"stick_1 axis 1 0\" ");
//        CreateStringBind("jaxis_1_1+ \"stick_1 axis 1 1\" ");
//
//        /* joystick1, hat */
//        CreateStringBind("jhat_0_0_0 \"stick_0 hat 0 1\" ");
//        CreateStringBind("jhat_0_0_1 \"stick_0 hat 0 2\" ");
//        CreateStringBind("jhat_0_0_2 \"stick_0 hat 0 4\" ");
//        CreateStringBind("jhat_0_0_3 \"stick_0 hat 0 8\" ");
    }

    public static void MAPPER_AddHandler(Mapper.MAPPER_Handler handler,int key,/*Bitu*/int mods,String eventname,String buttonname) {
        //Check if it already exists=> if so return.
        for(CHandlerEvent it : handlergroup)
            if(it.buttonname.equals(buttonname)) return;

        new CHandlerEvent("hand_"+eventname,handler,key,mods,buttonname);
    }

    static private void MAPPER_SaveBinds() {
        String fileName = JavaMapper.mapperfile;
        try {
            RandomAccessFile saveFile = new RandomAccessFile(fileName, "rw");
            for (CEvent event : events) {
                saveFile.write(event.GetName().getBytes());
                for (CBind bind : event.bindlist) {
                    String buf = " \""+bind.ConfigName()+bind.AddFlags()+"\"";
                    saveFile.write(buf.getBytes());
                }
                saveFile.writeByte((byte)'\n');
            }
            saveFile.close();
            //change_action_text("Mapper file saved.",CLR_WHITE);
        } catch (Exception e) {
            Log.log_msg("Can't open "+fileName+" for saving the mappings");
        }
    }


    static public boolean MAPPER_LoadBinds(String fileName) {
        BufferedReader loadfile = null;
        try {
            loadfile = new BufferedReader(new InputStreamReader(FileIOFactory.openStream(fileName)));
            String line;
            // :TODO: test if readLine will work, fgets was the c code used
            while ((line=loadfile.readLine())!=null) {
                CreateStringBind(line);
            }
            loadfile.close();
        } catch (Exception e) {
            return false;
        } finally {
            if (loadfile != null) {
                try {loadfile.close();} catch (Exception e){}
            }
        }
        Log.log_msg("MAPPER: Loading mapper settings from "+fileName);
        return true;
    }

    static public void MAPPER_CheckEvent(Object event) {
        for (CBindGroup it : bindgroups) {
            if (it.CheckEvent(event)) return;
        }
    }

    static private void CreateBindGroups() {
        bindgroups.clear();
        new CKeyBindGroup(1024);
    }

    public static void MAPPER_Init() {
        // InitializeJoysticks();
        CreateLayout();
        CreateBindGroups();
        if (!MAPPER_LoadBinds(JavaMapper.mapperfile))
            CreateDefaultBinds();
        //MAPPER_SaveBinds();
//        if (SDL_GetModState()& KMOD_CAPS) {
//            for (CBindList_it bit=caps_lock_event->bindlist.begin();bit!=caps_lock_event->bindlist.end();bit++) {
//    #if SDL_VERSION_ATLEAST(1, 2, 14)
//                (*bit)->ActivateBind(32767,true,false);
//                (*bit)->DeActivateBind(false);
//    #else
//                (*bit)->ActivateBind(32767,true,true); //Skip the action itself as bios_keyboard.cpp handles the startup state.
//    #endif
//            }
//        }
//        if (SDL_GetModState()&KMOD_NUM) {
//            for (CBindList_it bit=num_lock_event->bindlist.begin();bit!=num_lock_event->bindlist.end();bit++) {
//    #if SDL_VERSION_ATLEAST(1, 2, 14)
//                (*bit)->ActivateBind(32767,true,false);
//                (*bit)->DeActivateBind(false);
//    #else
//                (*bit)->ActivateBind(32767,true,true);
//    #endif
//            }
//        }
    }

    public static Section.SectionFunction MAPPER_StartUp = new Section.SectionFunction() {
        public void call(Section sec) {
            Section_prop section=(Section_prop)sec;

            Prop_path pp = section.Get_path("mapperfile");
            mapperfile = pp.realpath;
            //MAPPER_AddHandler(&MAPPER_Run,MK_f1,MMOD1,"mapper","Mapper");
        }
    };
}
