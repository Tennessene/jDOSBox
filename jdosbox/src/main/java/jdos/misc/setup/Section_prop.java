package jdos.misc.setup;

import jdos.util.StringHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

public class Section_prop extends Section {
    private Vector properties = new Vector();
    public Section_prop(String _sectionname) {
        super(_sectionname);
    }
    public Prop_int Add_int(String _propname, int when) {
        return Add_int(_propname, when, 0);
    }
    public Prop_int Add_int(String _propname, int when, int _value) {
        Prop_int test = new Prop_int(_propname, when, _value);
        properties.add(test);
        return test;
    }
    public Prop_string Add_string(String _propname, int when) {
        return Add_string(_propname, when, null);
    }
	public Prop_string Add_string(String _propname, int when, String _value) {
        Prop_string test = new Prop_string(_propname, when, _value);
        properties.add(test);
        return test;
    }
    public Prop_path Add_path(String _propname, int when) {
        return Add_path(_propname, when);
    }
	public Prop_path Add_path(String _propname, int when, String _value) {
        Prop_path test = new Prop_path(_propname, when, _value);
        properties.add(test);
        return test;
    }
    public Prop_bool Add_bool(String _propname, int when) {
        return Add_bool(_propname, when, false);
    }
	public Prop_bool Add_bool(String _propname, int when, boolean _value) {
        Prop_bool test = new Prop_bool(_propname, when, _value);
        properties.add(test);
        return test;
    }
    public Prop_hex Add_hex(String _propname, int when) {
        return Add_hex(_propname, when, new Hex(0));
    }
	public Prop_hex Add_hex(String _propname, int when, Hex _value) {
        Prop_hex test = new Prop_hex(_propname, when, _value);
        properties.add(test);
        return test;
    }
	public Prop_multival Add_multi(String _propname, int when,String sep) {
        Prop_multival test = new Prop_multival(_propname, when, sep);
        properties.add(test);
        return test;
    }
	public Prop_multival_remain Add_multiremain(String _propname, int when,String sep) {
        Prop_multival_remain test = new Prop_multival_remain(_propname, when, sep);
        properties.add(test);
        return test;
    }
	public Property Get_prop(int index) {
        if (index>=0 && index<properties.size())
            return (Property)properties.elementAt(index);
        return null;
    }
    public Property byname(String name) {
        for (int i=0;i<properties.size();i++) {
            Property p = (Property)properties.elementAt(i);
            if (p.propname.equals(name))
                return p;
        }
        return null;
    }
	public int Get_int(String _propname) {
        Property result = byname(_propname);
        if (result != null)
            return result.GetValue().getInt();
        return 0;
    }
	public String Get_string(String _propname) {
        Property result = byname(_propname);
        if (result != null)
            return result.GetValue().getString();
        return "";
    }
	public boolean Get_bool(String _propname) {
        Property result = byname(_propname);
        if (result != null)
            return result.GetValue().getBool();
        return false;
    }
	public Hex Get_hex(String _propname) {
        Property result = byname(_propname);
        if (result != null)
            return result.GetValue().getHex();
        return new Hex(0);
    }
	public double Get_double(String _propname) {
        Property result = byname(_propname);
        if (result != null)
            return result.GetValue().getDouble();
        return 0.0;
    }
	public Prop_path Get_path(String _propname) {
        Property result = byname(_propname);
        if (result != null) {
            if (result instanceof Prop_path)
                return (Prop_path)result;
        }
        return null;
    }
	public Prop_multival Get_multival(String _propname) {
        Property result = byname(_propname);
        if (result != null) {
            if (result instanceof Prop_multival)
                return (Prop_multival)result;
        }
        return null;
    }
	public Prop_multival_remain Get_multivalremain(String _propname) {
        Property result = byname(_propname);
        if (result != null) {
            if (result instanceof Prop_multival_remain)
                return (Prop_multival_remain)result;
        }
        return null;
    }
	public void HandleInputline(String aLine) {
        String[] parts = StringHelper.split(aLine, "=");
        if (parts.length==2) {
            Property prop = byname(parts[0]);
            if (prop != null)
                prop.SetValue(parts[1]);
        }        
    }
	public void PrintData(OutputStream os) throws IOException {
        for (int i=0;i<properties.size();i++) {
            Property p = (Property)properties.elementAt(i);
            String line = p.propname+"="+p.GetValue().toString()+"\n";
            Config.fputs(line, os);
        }
    }
	public String GetPropValue(String _property) {
        Property prop = byname(_property);
        if (prop != null)
            return prop.GetValue().toString();
        return NO_SUCH_PROPERTY;
    }
}
