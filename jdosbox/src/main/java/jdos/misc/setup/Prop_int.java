package jdos.misc.setup;

import jdos.misc.Log;

public class Prop_int extends Property{
    public Prop_int(String _propname, int when, int _value) {
        super(_propname, when);
        default_value.set(_value);
        value.set(_value);
        min.set(-1);
        max.set(-1);
    }
    public Prop_int(String _propname, int when, int _min, int _max, int _value) {
        super(_propname, when);
        default_value.set(_value);
        value.set(_value);
        min.set(_min);
        max.set(_max);
    }
    public void SetMinMax(int min, int max) {this.min.set(min); this.max.set(max);}
    public void SetMinMax(Value min, Value max) {min.set(min); max.set(max);}
    public void SetValue(String str) {
        SetVal(new Value(str, Value.Etype.V_INT), false, true);
    }

    public boolean CheckValue(Value in, boolean warn) {
        if (suggested_values.isEmpty() && super.CheckValue(in, warn)) return true;
        int mi = min.getInt();
        int ma = max.getInt();
        int va = in.getInt();
        if (mi == -1 && ma == -1) return true;
        if (va >= mi && va <= ma) return true;
        if (warn) Log.log_msg(in.toString()+" lies outside the range "+min.toString()+"-"+max.toString()+" for variable: "+propname+".\nIt might now be reset to the default value: "+default_value.toString());
        return false;
    }
    private Value min = new Value();
    private Value max = new Value();
}
