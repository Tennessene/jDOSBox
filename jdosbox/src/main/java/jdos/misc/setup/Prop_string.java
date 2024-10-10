package jdos.misc.setup;

import jdos.misc.Log;

public class Prop_string extends Property {
    public Prop_string(String _propname, int when, String _value) {
        super(_propname, when);
        default_value.set(_value);
        value.set(_value);
    }
    public void SetValue(String str) {
        //suggested values always case insensitive.
    	//If there are none then it can be paths and such which are case sensitive
        if (!suggested_values.isEmpty()) str = str.toLowerCase();
        SetVal(new Value(str, Value.Etype.V_STRING), false, true);
    }
    public boolean CheckValue(Value in, boolean warn) {
        if (suggested_values.isEmpty()) return true;
        for (int i=0;i<suggested_values.size();i++) {
            Value v = (Value)suggested_values.elementAt(i);
            if (v.equals(in)) { //Match!
                return true;
            }
            if (v.toString().equals("%u")) {
                try {
                    if (Integer.parseInt(in.toString())>=0)
                        return true;
                } catch (Exception e) {
                }
            }
        }
        if (warn) Log.log_msg("\""+in.toString()+"\" is not a valid value for variable: "+propname+".\nIt might now be reset it to default value: "+default_value.toString());
        return false;
    }
}
