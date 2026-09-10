<#
  RawPrinterHelper.ps1
  Sends raw bytes to an already-installed Windows printer object via the
  Print Spooler API (OpenPrinter/StartDocPrinter/WritePrinter), bypassing GDI
  rendering. Used instead of raw SMB "copy /b \host\share" because the
  cashier PC's account has no direct file-share permission on the print
  share, but printing through the locally installed printer connection
  (Settings > Printers & scanners) works via the spooler's own session.
#>

$RawPrinterHelperSource = @"
using System;
using System.Runtime.InteropServices;
public class RawPrinterHelper {
    [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Ansi)]
    public class DOCINFOA {
        [MarshalAs(UnmanagedType.LPStr)] public string pDocName;
        [MarshalAs(UnmanagedType.LPStr)] public string pOutputFile;
        [MarshalAs(UnmanagedType.LPStr)] public string pDataType;
    }
    [DllImport("winspool.Drv", EntryPoint="OpenPrinterA", SetLastError=true, CharSet=CharSet.Ansi, ExactSpelling=true)]
    public static extern bool OpenPrinter(string szPrinter, out IntPtr hPrinter, IntPtr pd);
    [DllImport("winspool.Drv", EntryPoint="ClosePrinter", SetLastError=true, ExactSpelling=true)]
    public static extern bool ClosePrinter(IntPtr hPrinter);
    [DllImport("winspool.Drv", EntryPoint="StartDocPrinterA", SetLastError=true, CharSet=CharSet.Ansi, ExactSpelling=true)]
    public static extern bool StartDocPrinter(IntPtr hPrinter, Int32 level, DOCINFOA di);
    [DllImport("winspool.Drv", EntryPoint="EndDocPrinter", SetLastError=true, ExactSpelling=true)]
    public static extern bool EndDocPrinter(IntPtr hPrinter);
    [DllImport("winspool.Drv", EntryPoint="StartPagePrinter", SetLastError=true, ExactSpelling=true)]
    public static extern bool StartPagePrinter(IntPtr hPrinter);
    [DllImport("winspool.Drv", EntryPoint="EndPagePrinter", SetLastError=true, ExactSpelling=true)]
    public static extern bool EndPagePrinter(IntPtr hPrinter);
    [DllImport("winspool.Drv", EntryPoint="WritePrinter", SetLastError=true, ExactSpelling=true)]
    public static extern bool WritePrinter(IntPtr hPrinter, IntPtr pBytes, Int32 dwCount, out Int32 dwWritten);

    public static bool SendBytesToPrinter(string szPrinterName, byte[] bytes, out string error) {
        IntPtr hPrinter;
        DOCINFOA di = new DOCINFOA();
        Int32 dwWritten = 0;
        bool bSuccess = false;
        error = "";
        di.pDocName = "HMIS Raw Print Job";
        di.pDataType = "RAW";
        if (OpenPrinter(szPrinterName, out hPrinter, IntPtr.Zero)) {
            if (StartDocPrinter(hPrinter, 1, di)) {
                if (StartPagePrinter(hPrinter)) {
                    IntPtr pUnmanagedBytes = Marshal.AllocCoTaskMem(bytes.Length);
                    Marshal.Copy(bytes, 0, pUnmanagedBytes, bytes.Length);
                    bSuccess = WritePrinter(hPrinter, pUnmanagedBytes, bytes.Length, out dwWritten);
                    Marshal.FreeCoTaskMem(pUnmanagedBytes);
                    EndPagePrinter(hPrinter);
                } else { error = "StartPagePrinter failed: " + Marshal.GetLastWin32Error(); }
                EndDocPrinter(hPrinter);
            } else { error = "StartDocPrinter failed: " + Marshal.GetLastWin32Error(); }
            ClosePrinter(hPrinter);
        } else { error = "OpenPrinter failed: " + Marshal.GetLastWin32Error(); }
        return bSuccess;
    }
}
"@

if (-not ("RawPrinterHelper" -as [type])) {
    Add-Type -TypeDefinition $RawPrinterHelperSource -Language CSharp
}
