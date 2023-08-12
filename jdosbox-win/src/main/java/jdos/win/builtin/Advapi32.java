package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.system.StaticData;
import jdos.win.system.WinSystem;

public class Advapi32 extends BuiltinModule {
    public class Sid {
        public static final int SIZE = 8;
        int psid;
        int Attributes;
    }

    public Advapi32(Loader loader, int handle) {
        super(loader, "advapi32.dll", handle);
        add(AddAccessAllowedAce);
        add(AddAccessDeniedAce);
        add(AllocateAndInitializeSid);
        add(FreeSid);
        add(GetTokenInformation);
        add(InitializeAcl);
        add(OpenProcessToken);
        add(Advapi32.class, "RegCloseKey", new String[] {"hKey"});
        add(RegCreateKeyExA);
        add(RegOpenKeyExA);
        add(RegQueryValueExA);
        add(RegSetValueExA);
    }

    // BOOL WINAPI AddAccessAllowedAce(PACL pAcl, DWORD dwAceRevision, DWORD AccessMask, PSID pSid)
    private Callback.Handler AddAccessAllowedAce = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.AddAccessAllowedAce";
        }
        public void onCall() {
            int pAcl = CPU.CPU_Pop32();
            int dwAceRevision = CPU.CPU_Pop32();
            int AccessMask = CPU.CPU_Pop32();
            int pSid = CPU.CPU_Pop32();
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // BOOL WINAPI AddAccessDeniedAce(PACL pAcl, DWORD dwAceRevision, DWORD AccessMask, PSID pSid)
    private Callback.Handler AddAccessDeniedAce = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.AddAccessDeniedAce";
        }
        public void onCall() {
            int pAcl = CPU.CPU_Pop32();
            int dwAceRevision = CPU.CPU_Pop32();
            int AccessMask = CPU.CPU_Pop32();
            int pSid = CPU.CPU_Pop32();
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // PVOID WINAPI FreeSid(PSID pSid)
    private Callback.Handler FreeSid = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.FreeSid";
        }
        public void onCall() {
            int pSid = CPU.CPU_Pop32();
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // BOOL WINAPI AllocateAndInitializeSid(PSID_IDENTIFIER_AUTHORITY pIdentifierAuthority, BYTE nSubAuthorityCount, DWORD dwSubAuthority0, DWORD dwSubAuthority1, DWORD dwSubAuthority2, DWORD dwSubAuthority3, DWORD dwSubAuthority4, DWORD dwSubAuthority5, DWORD dwSubAuthority6, DWORD dwSubAuthority7, PSID *pSid)
    private Callback.Handler AllocateAndInitializeSid = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.AllocateAndInitializeSid";
        }
        public void onCall() {
            int pIdentifierAuthority = CPU.CPU_Pop32();
            int nSubAuthorityCount = CPU.CPU_Pop32();
            int dwSubAuthority0 = CPU.CPU_Pop32();
            int dwSubAuthority1 = CPU.CPU_Pop32();
            int dwSubAuthority2 = CPU.CPU_Pop32();
            int dwSubAuthority3 = CPU.CPU_Pop32();
            int dwSubAuthority4 = CPU.CPU_Pop32();
            int dwSubAuthority5 = CPU.CPU_Pop32();
            int dwSubAuthority6 = CPU.CPU_Pop32();
            int dwSubAuthority7 = CPU.CPU_Pop32();
            int pSid = CPU.CPU_Pop32();
            Memory.mem_writed(pSid, 1);
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // BOOL WINAPI GetTokenInformation(HANDLE TokenHandle, TOKEN_INFORMATION_CLASS TokenInformationClass, LPVOID TokenInformation, DWORD TokenInformationLength, PDWORD ReturnLength)
    private Callback.Handler GetTokenInformation = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.GetTokenInformation";
        }
        public void onCall() {
            int TokenHandle = CPU.CPU_Pop32();
            int TokenInformationClass = CPU.CPU_Pop32();
            int TokenInformation = CPU.CPU_Pop32();
            int TokenInformationLength = CPU.CPU_Pop32();
            int ReturnLength = CPU.CPU_Pop32();
            if (TokenInformationClass == 1) { // TokenUser
                if (TokenInformationLength == 0) {
                    Memory.mem_writed(ReturnLength, Sid.SIZE);
                } else {
                    Memory.mem_writed(TokenInformation, StaticData.user.getHandle());
                    Memory.mem_writed(TokenInformation+4, 0); // Attributes
                    Memory.mem_writed(ReturnLength, Sid.SIZE);
                }
            } else {
                System.out.println(getName()+" TokenInformationClass "+TokenInformationClass+" not implemented yet");
                notImplemented();
            }
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // BOOL WINAPI InitializeAcl(PACL pAcl, DWORD nAclLength, DWORD dwAclRevision)
    private Callback.Handler InitializeAcl = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.InitializeAcl";
        }
        public void onCall() {
            int pAcl = CPU.CPU_Pop32();
            int nAclLength = CPU.CPU_Pop32();
            int dwAclRevision = CPU.CPU_Pop32();
            Memory.mem_zero(pAcl, nAclLength);
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // BOOL WINAPI OpenProcessToken(HANDLE ProcessHandle, DWORD DesiredAccess, PHANDLE TokenHandle)
    private Callback.Handler OpenProcessToken = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.OpenProcessToken";
        }
        public void onCall() {
            int ProcessHandle = CPU.CPU_Pop32();
            int DesiredAccess = CPU.CPU_Pop32();
            int TokenHandle = CPU.CPU_Pop32();
            Memory.mem_writed(TokenHandle, 1);
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // LONG WINAPI RegCloseKey(HKEY hKey)
    public static int RegCloseKey(int hKey) {
        return ERROR_SUCCESS;
    }

    // LONG WINAPI RegCreateKeyEx(HKEY hKey, LPCTSTR lpSubKey, DWORD Reserved, LPTSTR lpClass, DWORD dwOptions, REGSAM samDesired, LPSECURITY_ATTRIBUTES lpSecurityAttributes, PHKEY phkResult, LPDWORD lpdwDisposition)
    private Callback.Handler RegCreateKeyExA = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.RegCreateKeyExA";
        }
        public void onCall() {
            int hKey = CPU.CPU_Pop32();
            int lpSubKey = CPU.CPU_Pop32();
            int Reserved = CPU.CPU_Pop32();
            int lpClass = CPU.CPU_Pop32();
            int dwOptions = CPU.CPU_Pop32();
            int samDesired = CPU.CPU_Pop32();
            int lpSecurityAttributes = CPU.CPU_Pop32();
            int phkResult = CPU.CPU_Pop32();
            int lpdwDisposition = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.registry.createKey(hKey, lpSubKey, phkResult, lpdwDisposition);
        }
    };

    // LONG WINAPI RegOpenKeyEx(HKEY hKey, LPCTSTR lpSubKey, DWORD ulOptions, REGSAM samDesired, PHKEY phkResult)
    private Callback.Handler RegOpenKeyExA = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.RegOpenKeyExA";
        }
        public void onCall() {
            int hKey = CPU.CPU_Pop32();
            int lpSubKey = CPU.CPU_Pop32();
            int ulOptions = CPU.CPU_Pop32();
            int samDesired = CPU.CPU_Pop32();
            int phkResult = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.registry.openKey(hKey, lpSubKey, phkResult);
        }
    };

    // LONG WINAPI RegQueryValueEx(HKEY hKey, LPCTSTR lpValueName, LPDWORD lpReserved, LPDWORD lpType, LPBYTE lpData, LPDWORD lpcbData)
    private Callback.Handler RegQueryValueExA = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.RegQueryValueExA";
        }
        public void onCall() {
            int hKey = CPU.CPU_Pop32();
            int lpValueName = CPU.CPU_Pop32();
            int lpReserved = CPU.CPU_Pop32();
            int lpType = CPU.CPU_Pop32();
            int lpData = CPU.CPU_Pop32();
            int lpcbData = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.registry.getValue(hKey, lpValueName, lpType, lpData, lpcbData);
        }
    };

    // LONG WINAPI RegSetValueEx(HKEY hKey, LPCTSTR lpValueName, DWORD Reserved, DWORD dwType, const BYTE *lpData, DWORD cbData)
    private Callback.Handler RegSetValueExA = new HandlerBase() {
        public java.lang.String getName() {
            return "Advapi32.RegSetValueExA";
        }
        public void onCall() {
            int hKey = CPU.CPU_Pop32();
            int lpValueName = CPU.CPU_Pop32();
            int lpReserved = CPU.CPU_Pop32();
            int dwType  = CPU.CPU_Pop32();
            int lpData = CPU.CPU_Pop32();
            int cbData = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.registry.setValue(hKey, lpValueName, dwType , lpData, cbData);
        }
    };
}
