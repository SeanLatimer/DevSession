package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.win32.W32APITypeMapper;

import java.nio.charset.StandardCharsets;

public class WindowsCredStore implements NativeCreds {

    private static final int CRED_TYPE_GENERIC = 1;
    private static final int CRED_PERSIST_LOCAL_MACHINE = 2;
    private static final int ERROR_NOT_FOUND = 1168;
    private static final String TARGET_PREFIX = "devsession|";
    private static final String PROBE_KEY = "__probe__";

    private interface Advapi32 extends StdCallLibrary {

        Advapi32 INSTANCE = Native.load("Advapi32", Advapi32.class, W32APIOptions.UNICODE_OPTIONS);

        boolean CredWriteW(CREDENTIAL credential, DWORD flags);

        boolean CredReadW(String targetName, DWORD type, DWORD flags, PointerByReference credential);

        boolean CredDeleteW(String targetName, DWORD type, DWORD flags);

        void CredFree(Pointer credential);

    }

    @Structure.FieldOrder({"Flags", "Type", "TargetName", "Comment", "LastWritten", "CredentialBlobSize",
        "CredentialBlob", "Persist", "AttributeCount", "Attributes", "TargetAlias", "UserName"})
    public static class CREDENTIAL extends Structure {

        public DWORD Flags;
        public DWORD Type;
        public String TargetName;
        public String Comment;
        public FILETIME LastWritten;
        public DWORD CredentialBlobSize;
        public Pointer CredentialBlob;
        public DWORD Persist;
        public DWORD AttributeCount;
        public Pointer Attributes;
        public String TargetAlias;
        public String UserName;

        public CREDENTIAL() {
            super(W32APITypeMapper.UNICODE);
        }

        public CREDENTIAL(Pointer pointer) {
            super(pointer, 0, W32APITypeMapper.UNICODE);
            read();
        }
    }

    static WindowsCredStore open() {
        PointerByReference probe = new PointerByReference();
        if (Advapi32.INSTANCE.CredReadW(TARGET_PREFIX + PROBE_KEY, new DWORD(CRED_TYPE_GENERIC), new DWORD(0), probe)) {
            Advapi32.INSTANCE.CredFree(probe.getValue());
        } else {
            int error = Native.getLastError();
            if (error != ERROR_NOT_FOUND) {
                throw new IllegalStateException("Windows Credential Manager is not usable (error " + error + ")");
            }
        }
        return new WindowsCredStore();
    }

    @Override
    public String read(String key) {
        PointerByReference reference = new PointerByReference();
        if (!Advapi32.INSTANCE.CredReadW(TARGET_PREFIX + key, new DWORD(CRED_TYPE_GENERIC), new DWORD(0), reference)) {
            int error = Native.getLastError();
            if (error == ERROR_NOT_FOUND) return null;
            throw new IllegalStateException("CredRead failed for '" + key + "' (error " + error + ")");
        }

        try {
            CREDENTIAL credential = new CREDENTIAL(reference.getValue());
            if (credential.CredentialBlob == null || credential.CredentialBlobSize.intValue() <= 0) return null;
            byte[] blob = credential.CredentialBlob.getByteArray(0, credential.CredentialBlobSize.intValue());
            return new String(blob, StandardCharsets.UTF_8);
        } finally {
            Advapi32.INSTANCE.CredFree(reference.getValue());
        }
    }

    @Override
    public void write(String key, String value) {
        byte[] blob = value.getBytes(StandardCharsets.UTF_8);

        Memory buffer = new Memory(Math.max(1, blob.length));
        try {
            buffer.write(0, blob, 0, blob.length);

            CREDENTIAL credential = new CREDENTIAL();
            credential.Flags = new DWORD(0);
            credential.Type = new DWORD(CRED_TYPE_GENERIC);
            credential.TargetName = TARGET_PREFIX + key;
            credential.Comment = "DevSession authentication token";
            credential.LastWritten = new FILETIME();
            credential.CredentialBlobSize = new DWORD(blob.length);
            credential.CredentialBlob = buffer;
            credential.Persist = new DWORD(CRED_PERSIST_LOCAL_MACHINE);
            credential.AttributeCount = new DWORD(0);
            credential.Attributes = Pointer.NULL;
            credential.TargetAlias = null;
            credential.UserName = key;
            credential.write();

            if (!Advapi32.INSTANCE.CredWriteW(credential, new DWORD(0))) {
                throw new IllegalStateException("CredWrite failed for '" + key + "' (error " + Native.getLastError() + ")");
            }
        } finally {
            buffer.clear();
        }
    }

    @Override
    public void delete(String key) {
        if (!Advapi32.INSTANCE.CredDeleteW(TARGET_PREFIX + key, new DWORD(CRED_TYPE_GENERIC), new DWORD(0))) {
            int error = Native.getLastError();
            if (error != ERROR_NOT_FOUND) {
                throw new IllegalStateException("CredDelete failed for '" + key + "' (error " + error + ")");
            }
        }
    }

    @Override
    public String describe() {
        return "Windows Credential Manager";
    }

}
