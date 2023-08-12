package jdos.misc.setup;

public class Prop_bool extends Property {
    public Prop_bool(String _propname, int when, boolean _value) {
        super(_propname, when);
        default_value.set(_value);
        value.set(_value);
    }
    public void SetValue(String str) {
        SetVal(new Value(str, Value.Etype.V_BOOL), false, true);
    }
}
