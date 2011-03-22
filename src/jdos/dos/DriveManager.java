package jdos.dos;

import jdos.misc.Log;
import jdos.misc.setup.Section;

import java.util.Vector;

public class DriveManager {
    static public void AppendDisk(int drive, Dos_Drive disk) {
        driveInfos[drive].disks.add(disk);
    }

    static public void InitializeDrive(int drive) {
        currentDrive = drive;
        DriveInfo driveInfo = driveInfos[currentDrive];
        if (driveInfo.disks.size() > 0) {
            driveInfo.currentDisk = 0;
            Dos_Drive disk = driveInfo.disks.elementAt(driveInfo.currentDisk);
            Dos_files.Drives[currentDrive] = disk;
            disk.Activate();
        }
    }

    static public int UnmountDrive(int drive) {
        int result = 0;
        // unmanaged drive
        if (driveInfos[drive].disks.size() == 0) {
            result = Dos_files.Drives[drive].UnMount();
        } else {
            // managed drive
            int currentDisk = driveInfos[drive].currentDisk;
            result = driveInfos[drive].disks.elementAt(currentDisk).UnMount();
            // only delete on success, current disk set to NULL because of UnMount
            if (result == 0) {
                driveInfos[drive].disks.clear();
            }
        }
        return result;
    }

//	static void CycleDrive(bool pressed);
//	static void CycleDisk(bool pressed);
    static public void CycleAllDisks() {
        for (int idrive=0; idrive<Dos_files.DOS_DRIVES; idrive++) {
            int numDisks = (int)driveInfos[idrive].disks.size();
            if (numDisks > 1) {
                // cycle disk
                int currentDisk = driveInfos[idrive].currentDisk;
                Dos_Drive oldDisk = driveInfos[idrive].disks.elementAt(currentDisk);
                currentDisk = (currentDisk + 1) % numDisks;
                Dos_Drive newDisk = driveInfos[idrive].disks.elementAt(currentDisk);
                driveInfos[idrive].currentDisk = currentDisk;

                // copy working directory, acquire system resources and finally switch to next drive
                newDisk.curdir = oldDisk.curdir;
                newDisk.Activate();
                Dos_files.Drives[idrive] = newDisk;
                Log.log_msg("Drive %c: disk %d of %d now active", 'A'+idrive, currentDisk+1, numDisks);
            }
        }
    }
    static public void Init(Section sec) {
        // setup driveInfos structure
        currentDrive = 0;
        for(int i = 0; i < Dos_files.DOS_DRIVES; i++) {
            driveInfos[i] = new DriveInfo();
            driveInfos[i].currentDisk = 0;
    	}
    }

//	MAPPER_AddHandler(&CycleDisk, MK_f3, MMOD1, "cycledisk", "Cycle Disk");
//	MAPPER_AddHandler(&CycleDrive, MK_f3, MMOD2, "cycledrive", "Cycle Drv");

    static private class DriveInfo {
        Vector<Dos_Drive> disks = new Vector<Dos_Drive>();
        /*Bit32u*/int currentDisk;
    }
    static private DriveInfo[] driveInfos = new DriveInfo[Dos_files.DOS_DRIVES];
    static int currentDrive;
}
