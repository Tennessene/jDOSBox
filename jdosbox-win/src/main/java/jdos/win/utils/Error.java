package jdos.win.utils;

public class Error {
    public static final int ERROR_SUCCESS = 0;
    public static final int ERROR_FILE_NOT_FOUND = 2;
    public static final int ERROR_PATH_NOT_FOUND = 3;
    public static final int ERROR_ACCESS_DENIED = 5;
    public static final int ERROR_INVALID_HANDLE = 6;
    public static final int ERROR_NO_MORE_FILES = 18;
    public static final int ERROR_FILE_EXISTS = 80;
    public static final int ERROR_INVALID_PARAMETER = 87;
    public static final int ERROR_INSUFFICIENT_BUFFER = 122;
    public static final int ERROR_MOD_NOT_FOUND = 126;
    public static final int ERROR_BAD_PATHNAME = 161;
    public static final int ERROR_ALREADY_EXISTS = 183;
    public static final int ERROR_MR_MID_NOT_FOUND = 317;
    public static final int ERROR_INVALID_ADDRESS = 487;
    public static final int ERROR_INVALID_FLAGS = 1004;
    public static final int ERROR_INVALID_WINDOW_HANDLE = 1400;
    public static final int ERROR_TLW_WITH_WSCHILD = 1406;
    public static final int ERROR_CANNOT_FIND_WND_CLASS = 1407;
    public static final int ERROR_CLASS_ALREADY_EXISTS = 1410;
    public static final int ERROR_CLASS_DOES_NOT_EXIST = 1411;
    public static final int ERROR_INVALID_INDEX = 1413;
    public static final int ERROR_INVALID_FILTER_PROC = 1427;
    public static final int ERROR_HOOK_NEEDS_HMOD = 1428;
    public static final int ERROR_RESOURCE_DATA_NOT_FOUND = 1812;
    public static final int ERROR_RESOURCE_TYPE_NOT_FOUND = 1813;

    public static final int S_OK = 0x00000000;
    public static final int E_NOINTERFACE = 0x80004002;
    public static final int E_POINTER = 0x80004003;
    public static final int E_INVALIDARG = 0x80070057;
    public static final int E_OUTOFMEMORY = 0x8007000E;
    public static final int DDERR_INVALIDPARAMS = E_INVALIDARG;

    public static final int INVALID_FILE_ATTRIBUTES = -1;

    public static String getError(int e) {
        switch (e) {
            case ERROR_SUCCESS:
                return "The operation completed successfully.";
            case ERROR_FILE_NOT_FOUND:
                return "The system cannot find the file specified.";
            case ERROR_PATH_NOT_FOUND:
                return "The system cannot find the path specified.";
            case ERROR_ACCESS_DENIED:
                return "Access is denied.";
            case ERROR_INVALID_HANDLE:
                return "The handle is invalid.";
            case ERROR_FILE_EXISTS:
                return "The file exists.";
            case ERROR_INVALID_PARAMETER:
                return "The parameter is incorrect.";
            case ERROR_INSUFFICIENT_BUFFER:
                return "The data area passed to a system call is too small.";
            case ERROR_MOD_NOT_FOUND:
                return "The specified module could not be found.";
            case ERROR_ALREADY_EXISTS:
                return "Cannot create a file when that file already exists.";
            case ERROR_MR_MID_NOT_FOUND:
                return "The system cannot find message text for message number 0x%1 in the message file for %2.";
            case ERROR_INVALID_FLAGS:
                return "Invalid flags.";
        }
        return null;
    }
}
