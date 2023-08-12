package jdos.misc.setup;

public class Prop_multival_remain extends Prop_multival {
    public Prop_multival_remain(String _propname, int when, String sep) {
        super(_propname, when, sep);
    }

    public void SetValue(String input) {
        SetVal(new Value(input, Value.Etype.V_STRING), false, true);
        //No properties in this section. do nothing
        if (section.Get_prop(0) == null) return;
        int i=0;
        int number_of_properties = 0;
        while (section.Get_prop(number_of_properties) != null) {
            number_of_properties++;
        }
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
            /* when i == number_of_properties add the total line. (makes more then
		     * one string argument possible for parameters of cpu) */
            if (pos >= 0 && i < number_of_properties) {
                in = input.substring(0, pos); //seperator found
                input = input.substring(pos+separator.length());
            } else if (input.length() > 0) { //last argument or last property
                in = input;
                input = "";
            }
            //Test Value. If it fails set default
            Value valtest = new Value(in, p.Get_type());
            if (!p.CheckValue(valtest, true)) {
                make_default_value();
                return;
            }
            p.SetValue(in);
        }
    }
}
