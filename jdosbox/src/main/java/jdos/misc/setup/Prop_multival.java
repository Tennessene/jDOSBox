package jdos.misc.setup;

import java.util.Vector;

public class Prop_multival extends Property {
    protected Section_prop section;
    protected String separator;
    protected void make_default_value() {
        Property p = section.Get_prop(0);
        if (p == null) return;
        String result = p.Get_Default_Value().toString();
        int i=1;
        while (true) {
            p = section.Get_prop(i++);
            if (p == null)
                break;
            String props = p.Get_Default_Value().toString();
            if (props.length()!=0) {
                result+=separator;
                result+=props;
            }
        }
    }
    public Prop_multival(String _propname, int when, String sep) {
        super(_propname, when);
        section = new Section_prop("");
        separator = sep;
    }
    public Section_prop GetSection() {return section;}
    public void SetValue(String input) {
        SetVal(new Value(input, Value.Etype.V_STRING), false, true);
        //No properties in this section. do nothing
        if (section.Get_prop(0) == null) return;
        int i=0;
        while (true) {
            Property p = section.Get_prop(i++);
            if (p == null)
                break;
            //trim leading seperators
            while (input.startsWith(separator)) {
                input = input.substring(separator.length());
            }
            int pos = input.indexOf(separator);
            String in = ""; //default value
            if (pos >= 0) {
                in = input.substring(0, pos); //seperator found
                input = input.substring(pos+separator.length());
            } else if (input.length() > 0) { //last argument
                in = input;
                input = "";
            }
            //Test Value. If it fails set default
            Value valtest = new Value(in, p.Get_type());
            if (p.CheckValue(valtest, true)) {
                make_default_value();
                return;
            }
            p.SetValue(in);
        }
    }
    public Vector GetValues() {
        if (section.Get_prop(0) == null)
            return suggested_values;
        Vector result = new Vector();
        int i=0;
        while (true) {
            Property p = section.Get_prop(i++);
            if (p == null)
                return suggested_values;
            Vector v = p.GetValues();
            if (!v.isEmpty()) return v;
        }
    }
}
