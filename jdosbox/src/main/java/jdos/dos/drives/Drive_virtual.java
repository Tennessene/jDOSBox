package jdos.dos.drives;

import jdos.dos.*;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.ShortRef;
import jdos.util.StringRef;

public class Drive_virtual extends Dos_Drive {
    static public void VFILE_Register(String name, byte[] data, int len) {
        VFILE_Block new_file=new VFILE_Block();
        new_file.name=name;
        new_file.data=data;
        new_file.size=len;
        new_file.date=Dos.DOS_PackDate(2002,10,1);
        new_file.time= Dos.DOS_PackTime(12,34,56);
        new_file.next=first_file;
        first_file=new_file;
    }
    static public void VFILE_Remove(String name) {
        VFILE_Block chan=first_file;
        VFILE_Block where=first_file;
        while (chan!=null) {
            if (name.equals(chan.name)) {
                where.next = chan.next;
                if(chan == first_file) first_file = chan.next;
                return;
            }
            where = chan;
            chan=chan.next;
        }
    }
    private static class VFILE_Block {
        String name;
        /*Bit8u*/byte[] data;
        /*Bit32u*/long size;
        /*Bit16u*/int date;
        /*Bit16u*/int time;
        VFILE_Block next;
    }
    static private VFILE_Block first_file;

    static private class Virtual_File extends DOS_File {
        public Virtual_File(byte[] in_data,/*Bit32u*/long in_size) {
            file_size=(int)in_size;
            file_data=in_data;
            file_pos=0;
            date=Dos.DOS_PackDate(2002,10,1);
            time=Dos.DOS_PackTime(12,34,56);
            open=true;
        }
        public boolean Read(byte[] data,/*Bit16u*/IntRef size) {
            /*Bit32u*/int left=file_size-file_pos;
            if (left<=size.value) {
                System.arraycopy(file_data, file_pos, data, 0, left);
                size.value=left;
            } else {
                System.arraycopy(file_data, file_pos, data, 0, size.value);
            }
            file_pos+=size.value;
            return true;
        }
        public boolean Write(byte[] data,/*Bit16u*/IntRef size) {
            /* Not really writable */
	        return false;
        }
        public boolean Seek(/*Bit32u*/LongRef new_pos,/*Bit32u*/int type) {
            switch (type) {
            case Dos_files.DOS_SEEK_SET:
                if (new_pos.value<=file_size) file_pos=(int)new_pos.value;
                else return false;
                break;
            case Dos_files.DOS_SEEK_CUR:
                if ((new_pos.value+file_pos)<=file_size) file_pos=(int)(new_pos.value+file_pos);
                else return false;
                break;
            case Dos_files.DOS_SEEK_END:
                if (new_pos.value<=file_size) file_pos=(int)(file_size-new_pos.value);
                else return false;
                break;
            }
            new_pos.value=file_pos;
            return true;
        }
        public boolean Close() {
            return true;
        }        
        public /*Bit16u*/int GetInformation() {
            return 0x40;
        }

        private /*Bit32u*/int file_size;
        private /*Bit32u*/int file_pos;
        private byte[] file_data;
    }

	public Drive_virtual() {
        info="Internal Virtual Drive";
    }
	public DOS_File FileOpen(String name,/*Bit32u*/int flags) {
        /* Scan through the internal list of files */
        VFILE_Block cur_file=first_file;
        while (cur_file!=null) {
            if (name.compareToIgnoreCase(cur_file.name)==0) {
            /* We have a match */
                DOS_File file=new Virtual_File(cur_file.data,cur_file.size);
                file.flags=flags;
                return file;
            }
            cur_file=cur_file.next;
        }
        return null;
    }
	public DOS_File FileCreate(String name,/*Bit16u*/int attributes) {
        return null;
    }

	public boolean FileUnlink(String _name) {
        return false;
    }
    
	public boolean RemoveDir(String _dir) {
        return false;
    }
    
	public boolean MakeDir(String _dir) {
        return false;
    }
    
	public boolean TestDir(String _dir) {
        if (_dir.length()==0) return true;		//only valid dir is the empty dir
	    return false;
    }
    
	public boolean FindFirst(String _dir,Dos_DTA dta,boolean fcb_findfirst/*=false*/) {
        search_file=first_file;
        /*Bit8u*/ShortRef attr=new ShortRef();StringRef pattern=new StringRef();
        dta.GetSearchParams(attr,pattern);
        if (attr.value == Dos_system.DOS_ATTR_VOLUME) {
            dta.SetResult("DOSBOX",0,0,0,(short)Dos_system.DOS_ATTR_VOLUME);
            return true;
        } else if ((attr.value & Dos_system.DOS_ATTR_VOLUME)!=0 && !fcb_findfirst) {
            if (Drives.WildFileCmp("DOSBOX",pattern.value)) {
                dta.SetResult("DOSBOX",0,0,0,(short)Dos_system.DOS_ATTR_VOLUME);
                return true;
            }
        }
        return FindNext(dta);
    }
	public boolean FindNext(Dos_DTA dta) {
        /*Bit8u*/ShortRef attr=new ShortRef();StringRef pattern=new StringRef();
        dta.GetSearchParams(attr,pattern);
        while (search_file!=null) {
            if (Drives.WildFileCmp(search_file.name,pattern.value)) {
                dta.SetResult(search_file.name,search_file.size,search_file.date,search_file.time,(short)Dos_system.DOS_ATTR_ARCHIVE);
                search_file=search_file.next;
                return true;
            }
            search_file=search_file.next;
        }
        Dos.DOS_SetError(Dos.DOSERR_NO_MORE_FILES);
        return false;
    }
	public boolean GetFileAttr(String name,/*Bit16u*/IntRef attr) {
        VFILE_Block cur_file=first_file;
        while (cur_file!=null) {
            if (name.compareToIgnoreCase(cur_file.name)==0) {
                attr.value = Dos_system.DOS_ATTR_ARCHIVE;	//Maybe readonly ?
                return true;
            }
            cur_file=cur_file.next;
        }
        return false;
    }
	public boolean Rename(String oldname,String newname) {
        return false;
    }    
	public boolean AllocationInfo(/*Bit16u*/IntRef _bytes_sector,/*Bit8u*/ShortRef _sectors_cluster,/*Bit16u*/IntRef _total_clusters,/*Bit16u*/IntRef _free_clusters) {
        _bytes_sector.value=512;
        _sectors_cluster.value=32;
        _total_clusters.value=32765; // total size is always 500 MB
        _free_clusters.value=0; // nothing free here
        return true;
    }
	public boolean FileExists(String name) {
        VFILE_Block cur_file=first_file;
        while (cur_file!=null) {
            if (name.compareToIgnoreCase(cur_file.name)==0) return true;
            cur_file=cur_file.next;
        }
        return false;
    }
	public boolean FileStat(String name, FileStat_Block stat_block) {
        VFILE_Block cur_file=first_file;
        while (cur_file!=null) {
            if (name.compareToIgnoreCase(cur_file.name)==0) {
                stat_block.attr=Dos_system.DOS_ATTR_ARCHIVE;
                stat_block.size=cur_file.size;
                stat_block.date=Dos.DOS_PackDate(2002,10,1);
                stat_block.time=Dos.DOS_PackTime(12,34,56);
                return true;
            }
            cur_file=cur_file.next;
        }
        return false;
    }
	public /*Bit8u*/short GetMediaByte() {
        return 0xF8;
    }
	public void EmptyCache() {}
	public boolean isRemote() {
        return false;
    }
	public boolean isRemovable() {
        return false;
    }
	public /*Bits*/int UnMount() {
        first_file = null;
        return 1;
    }
	private VFILE_Block search_file;
}
