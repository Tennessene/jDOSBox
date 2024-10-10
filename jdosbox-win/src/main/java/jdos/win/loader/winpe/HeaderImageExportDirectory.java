package jdos.win.loader.winpe;

import java.io.IOException;

public class HeaderImageExportDirectory {
    public long	Characteristics;
	public long	TimeDateStamp;
	public int	MajorVersion;
	public int	MinorVersion;
	public long	Name;
	public long	Base;
	public long	NumberOfFunctions;
	public long	NumberOfNames;
	public long	AddressOfFunctions;
	public long	AddressOfNames;
	public long	AddressOfNameOrdinals;

    public void load(LittleEndianFile is) throws IOException {
        Characteristics = is.readUnsignedInt();
        TimeDateStamp = is.readUnsignedInt();
        MajorVersion = is.readUnsignedShort();
        MinorVersion = is.readUnsignedShort();
        Name = is.readUnsignedInt();
        Base = is.readUnsignedInt();
        NumberOfFunctions = is.readUnsignedInt();
        NumberOfNames = is.readUnsignedInt();
        AddressOfFunctions = is.readUnsignedInt();
        AddressOfNames = is.readUnsignedInt();
        AddressOfNameOrdinals = is.readUnsignedInt();
    }
}
