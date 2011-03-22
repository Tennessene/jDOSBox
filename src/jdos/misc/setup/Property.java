package jdos.misc.setup;

import jdos.misc.Log;
import jdos.misc.Msg;

import java.util.Vector;

public abstract class Property {
    public enum Changeable {Always, WhenIdle,OnlyAtStart}
    public final String propname;

    public Property(String _propname, Changeable when) {
        propname = _propname;
        change = when;
    }

    public void Set_values(String[] in) {
        Value.Etype type = default_value.type;
        for (String i:in) {
            suggested_values.add(new Value(i, type));
        }
    }
    public void Set_help(String str) {
        String result = "CONFIG_"+propname;
        result = result.toUpperCase();
        Msg.add(result, str);
    }

    public String Get_help() {
        String result = "CONFIG_"+propname;
        result = result.toUpperCase();
        return Msg.get(result);
    }

    public abstract void SetValue(String str);

    public Value GetValue() { return value; }
    public Value Get_Default_Value() { return default_value; }
    //CheckValue returns true  if value is in suggested_values;
	//Type specific properties are encouraged to override this and check for type
	//specific features.
	public boolean CheckValue(Value in, boolean warn) {
        if (suggested_values.isEmpty()) return true;
        for (Value v: suggested_values) {
            if (in.equals(v))
                return true;
        }
        if (warn) Log.log_msg("\"%s\" is not a valid value for variable: %s.\nIt might now be reset to the default value: %s",in.toString(),propname,default_value.toString());
        return false;
    }

    //Set interval value to in or default if in is invalid. force always sets the value.
    public void SetVal(Value in, boolean forced) {SetVal(in, forced, true);}
	public void SetVal(Value in, boolean forced, boolean warn) {if(forced || CheckValue(in,warn)) value = in; else value = default_value;}
    public Vector<Value> GetValues() {
        return suggested_values;
    }

    public Value.Etype Get_type() {return default_value.type;}

    protected Value value = new Value();
    protected Vector<Value> suggested_values = new Vector<Value>();
    protected Value default_value = new Value();
    protected final Changeable change;
}
