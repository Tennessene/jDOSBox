package jdos.misc.setup;

public class Prop_hex extends Property {
    public Prop_hex(String _propname, int when, Hex _value) {
        super(_propname, when);
        default_value.set(_value);
        value.set(_value);
    }
    public void SetValue(String str) {
        SetVal(new Value(str, Value.Etype.V_HEX), false, true);
    }
}
