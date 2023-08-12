package jdos.hardware.pci;

abstract public class PCI_BAR {
    abstract int getBAR(int currentValue);
    abstract void setBAR(int newValue);
}
