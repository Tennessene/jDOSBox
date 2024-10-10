package jdos.misc.setup;

public class Hex {
    int _hex;

    public Hex() {
        _hex = 0;
    }

    public Hex(int in) {
        _hex = in;
    }

    public Hex(Hex in) {
        _hex = in._hex;
    }
    
    public boolean equals(Object obj) {
        return (obj instanceof Hex && ((Hex)obj)._hex == _hex);
    }

    public int toInt() {
        return _hex;
    }
}
