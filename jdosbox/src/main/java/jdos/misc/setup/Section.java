package jdos.misc.setup;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

public abstract class Section {
    public static final String NO_SUCH_PROPERTY = "PROP_NOT_EXIST";
    static public interface SectionFunction {
        public void call(Section section);
    }
    private class Function_wrapper {
        SectionFunction function;
        boolean canchange;
        Function_wrapper(SectionFunction _fun, boolean _ch) {
            function = _fun;
            canchange = _ch;
        }
    }
    private Vector initfunctions = new Vector();
    private Vector destroyfunction = new Vector();
    private String sectionname;

    public Section(String _sectionname) {
        sectionname = _sectionname;
    }

    public void AddInitFunction(SectionFunction func) {
        AddInitFunction(func, false);
    }
    public void AddInitFunction(SectionFunction func, boolean canchange) {
        initfunctions.add(new Function_wrapper(func, canchange));

    }
    public void AddDestroyFunction(SectionFunction func) {
        AddDestroyFunction(func, false);
    }
    public void AddDestroyFunction(SectionFunction fun, boolean canchange) {
        destroyfunction.add(new Function_wrapper(fun, canchange));
    }
    public void ExecuteInit() {
        ExecuteInit(true);
    }
    public void ExecuteInit(boolean initall) {
        for (int i=0;i<initfunctions.size();i++) {
            Function_wrapper f = (Function_wrapper)initfunctions.elementAt(i);
            if (initall || f.canchange) f.function.call(this);
        }
    }
    public void ExecuteDestroy() {
        ExecuteDestroy(true);
    }
    public void ExecuteDestroy(boolean destroyall) {
        for (int i=0;i<destroyfunction.size();i++) {
            Function_wrapper f = (Function_wrapper)destroyfunction.elementAt(i);
            if (destroyall || f.canchange) f.function.call(this);
        }
    }
    public String GetName() {return sectionname;}

    public abstract String GetPropValue(String _property);
    public abstract void HandleInputline(String _line);
    public abstract void PrintData(OutputStream os) throws IOException;
}
