package jdos.misc.setup;

public class Prop_double extends Property {
    public Prop_double(String _propname, int when, double _value) {
        super(_propname, when);
        default_value.set(_value);
        value.set(_value);
    }
    public void SetValue(String str) {
        SetVal(new Value(str, Value.Etype.V_DOUBLE), false, true);
    }
}
