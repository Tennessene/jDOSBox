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
    private Vector<Function_wrapper> initfunctions = new Vector<Function_wrapper>();
    private Vector<Function_wrapper> destroyfunction = new Vector<Function_wrapper>();
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
        for (Function_wrapper f: initfunctions) {
            if (initall || f.canchange) f.function.call(this);
        }
    }
    public void ExecuteDestroy() {
        ExecuteDestroy(true);
    }
    public void ExecuteDestroy(boolean destroyall) {
        for (Function_wrapper f: destroyfunction) {
            if (destroyall || f.canchange) f.function.call(this);
        }
    }
    public String GetName() {return sectionname;}

    public abstract String GetPropValue(String _property);
    public abstract void HandleInputline(String _line);
    public abstract void PrintData(OutputStream os) throws IOException;
}
