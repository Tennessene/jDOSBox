package jdos.misc.setup;

import jdos.util.StringHelper;

/*
 * Multitype storage container that is aware of the currently stored type in it.
 * Value st = "hello";
 * Value in = 1;
 * st = 12 //Exception
 * in = 12 //works
 */
public class Value {
    private Hex _hex = new Hex();
    private boolean _bool;
    private int _int;
    private String _string;
    private double _double;

    public class WrongType extends RuntimeException {}
    public static final class Etype {
        static final int V_NONE = 0;
        static final int V_HEX = 1;
        static final int V_BOOL = 2;
        static final int V_INT = 3;
        static final int V_STRING = 4;
        static final int V_DOUBLE = 5;
        static final int V_CURRENT = 6;
    }
    public int type;

    public Value() { type = Etype.V_NONE;}
    public Value(Hex in) { _hex = new Hex(in); type = Etype.V_HEX; }
    public Value(boolean in) {_bool = in; type = Etype.V_BOOL; }
    public Value(int in) {_int = in; type = Etype.V_INT; }
    public Value(double in) {_double = in; type = Etype.V_DOUBLE; }
    public Value(String in) {_string = in; type = Etype.V_STRING; }
    public Value(Value in) {plaincopy(in);}
    public Value(String in, int _t) { type = Etype.V_NONE; try {SetValue(in, _t);} catch (WrongType e){}}

    public void set(Hex in) throws WrongType {copy(new Value(in));}
    public void set(int in) throws WrongType {copy(new Value(in));}
    public void set(boolean in) throws WrongType {copy(new Value(in));}
    public void set(double in) throws WrongType {copy(new Value(in));}
    public void set(String in) throws WrongType {copy(new Value(in));}
    public void set(Value in) throws WrongType {copy(new Value(in));}

    public Hex getHex() throws WrongType {
        if (type != Etype.V_HEX)
            throw new WrongType();
        return _hex;
    }

    public int getInt() throws WrongType {
        if (type != Etype.V_INT)
            throw new WrongType();
        return _int;
    }

    public boolean getBool() throws WrongType {
        if (type != Etype.V_BOOL)
            throw new WrongType();
        return _bool;
    }

    public double getDouble() throws WrongType {
        if (type != Etype.V_DOUBLE)
            throw new WrongType();
        return _double;
    }

    public String getString() throws WrongType {
        if (type != Etype.V_STRING)
            throw new WrongType();
        return _string;
    }

    public void SetValue(String in, int _type) throws WrongType {
        if (_type == Etype.V_CURRENT && type == Etype.V_NONE) throw new WrongType();
        if (_type != Etype.V_CURRENT) {
            if (type != Etype.V_NONE && type != _type) throw new WrongType();
            type = _type;
        }
        if (type == Etype.V_HEX)
            _hex = new Hex(Integer.parseInt(in, 16));
        else if (type == Etype.V_INT)
            _int = Integer.parseInt(in);
        else if (type == Etype.V_BOOL) {
            char c = in.toLowerCase().charAt(0);
            _bool = !(c == '0' || c == 'd' || c == 'f' || in.equalsIgnoreCase("off"));
        } else if (type == Etype.V_STRING)
            _string = in;
        else if (type == Etype.V_DOUBLE)
            _double = Double.parseDouble(in);
        else
            /* Shouldn't happen!/Unhandled */
            throw new WrongType();
    }

    public void SetValue(String in) throws WrongType {
        SetValue(in, Etype.V_CURRENT);
    }

    public String toString() {
        if (type == Etype.V_HEX)
            return Integer.toString(_hex._hex, 16);
        else if (type == Etype.V_INT)
            return Integer.toString(_int);
        else if (type == Etype.V_BOOL)
            return Boolean.toString(_bool);
        else if (type == Etype.V_STRING)
            return _string;
        else if (type == Etype.V_DOUBLE)
            return StringHelper.format(_double, 2);
        else
            throw new RuntimeException("ToString messed up ?");
    }

    private void copy(Value in) throws WrongType {
        if (in != this) {
            if (type != Etype.V_NONE && type != in.type)
                throw new WrongType();
            plaincopy(in);
        }
    }

    private void plaincopy(Value in) {
        type = in.type;
        _int = in._int;
        _double = in._double;
        _bool = in._bool;
        _hex = new Hex(in._hex);
        _string = in._string;
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof Value) {
            Value other = (Value)obj;
            if (type != other.type)
                return false;
            if (type == Etype.V_BOOL)
                return _bool == other._bool;
            if (type == Etype.V_INT)
                return _int == other._int;
            if (type == Etype.V_HEX)
                return _hex.equals(other._hex);
            if (type == Etype.V_DOUBLE)
                return _double == other._double;
            if (type == Etype.V_STRING)
                return _string.equals(other._string);
            throw new RuntimeException("comparing stuff that doesn't make sense");
        }
        return false;
    }
}
