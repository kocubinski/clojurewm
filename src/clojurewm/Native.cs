using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace clojurewm
{
    using DWORD = UInt32;
    using HANDLE = IntPtr;
    using HDWP = IntPtr;
    using HHOOK = IntPtr;
    using HICON = IntPtr;
    using HINSTANCE = IntPtr;
    using HMENU = IntPtr;
    using HMODULE = IntPtr;
    using HMONITOR = IntPtr;
    using HSID = IntPtr;
    using HWINEVENTHOOK = IntPtr;
    using HWND = IntPtr;
    using LONG = Int32;
    using LPARAM = IntPtr; // INT_PTR
    using LPVOID = IntPtr;
    using LRESULT = IntPtr; // LONG_PTR
    using PVOID = IntPtr;
    using SIZE_T = IntPtr;
    using UINT = UInt32;
    using UINT_PTR = UIntPtr;
    using WPARAM = UIntPtr; // UINT_PTR
    using LCID = UInt32; // DWORD

    public class Native
    {
        public static readonly IntPtr IntPtrOne = (IntPtr) 1;

        private IntPtr hook;

        public const int WH_KEYBOARD_LL = 13;

		public delegate IntPtr HookProc(int code, UIntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll")]
        public static extern IntPtr SetWindowsHookEx(int hookType, 
            [MarshalAs(UnmanagedType.FunctionPtr)] HookProc lpfn, IntPtr hMod, UInt32 dwThreadId);

        [DllImport("user32.dll")]
        public static extern IntPtr CallNextHookEx([Optional] IntPtr hhk, int nCode, 
            UIntPtr wParam, IntPtr lParam);

        public void Init()
        {
            hook = SetWindowsHookEx(WH_KEYBOARD_LL, HookProcedure,
                                    Process.GetCurrentProcess().MainModule.BaseAddress,
                                    0);
            Console.WriteLine("Initializing...");
            Application.Run();
        }

        private IntPtr HookProcedure(int code, UIntPtr wparam, IntPtr lparam)
        {
            Console.WriteLine("{0} - {1} - {2}", code, wparam, lparam);
            //Console.WriteLine("OK");
            CallNextHookEx(hook, code, wparam, lparam);
            return IntPtr.Zero;
        }
    }
}