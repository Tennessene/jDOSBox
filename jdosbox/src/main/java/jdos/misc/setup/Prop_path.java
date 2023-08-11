package jdos.misc.setup;

import jdos.misc.Cross;

import java.io.File;

public class Prop_path extends Prop_string {
    public String realpath;
    public Prop_path(String _propname, int when, String _value) {
        super(_propname, when, _value);
        default_value.set(_value);
        value.set(_value);
        realpath = _value;
    }
    public void SetValue(String str) {
        SetVal(new Value(str, Value.Etype.V_STRING), false, true);
        if (str.length()==0) {
            realpath = "";
            return;
        }
        String workcopy = Cross.ResolveHomedir(str); //Parse ~ and friends
        //Prepend config directory in it exists. Check for absolute paths later
        if (Config.current_config_dir.length()==0) realpath = workcopy;
        else realpath = Config.current_config_dir + File.separator + workcopy;
        try {
            if (new File(workcopy).getAbsolutePath().charAt(0) == workcopy.charAt(0))
                realpath = workcopy;
        } catch (Exception e) {
            // This will throw an exception for unsigned applets
        }
    }
}
