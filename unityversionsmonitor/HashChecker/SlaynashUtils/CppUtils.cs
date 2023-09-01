using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;

namespace SlaynashUtils
{
    public static class CppUtils
    {
        public static unsafe byte GetByte(this IntPtr value) => *(byte*)value;
        public static unsafe bool GetBool(this IntPtr value) => *(bool*)value;
        public static unsafe ushort GetUShort(IntPtr value) => *(ushort*)value;
        public static unsafe int GetInt(this IntPtr value) => *(int*)value;
        public static unsafe uint GetUInt(this IntPtr value) => *(uint*)value;
        public static unsafe IntPtr GetValue(this IntPtr pointer) => *(IntPtr*)pointer;

        public static unsafe void SetValue(this IntPtr pointer, bool value) => *(bool*)pointer = value;

        public static unsafe string CharArrayPtrToString(this IntPtr ptr)
        {
            byte* text = *(byte**)ptr;
            int length = 0;
            while (text[length] != 0)
                ++length;

            return Encoding.UTF8.GetString(text, length);
        }

        internal static unsafe IntPtr Sigscan(IntPtr module, string signature)
        {
            int moduleSize = 0;
            foreach (ProcessModule module_ in Process.GetCurrentProcess().Modules)
            {
                if (module_.ModuleName == "UnityPlayer.dll")
                {
                    moduleSize = module_.ModuleMemorySize;
                    break;
                }
            }

            string signatureSpaceless = signature.Replace(" ", "");
            int signatureLength = signatureSpaceless.Length / 2;
            byte[] signatureBytes = new byte[signatureLength];
            bool[] signatureNullBytes = new bool[signatureLength];
            for (int i = 0; i < signatureLength; ++i)
            {
                if (signatureSpaceless[i * 2] == '?')
                    signatureNullBytes[i] = true;
                else
                    signatureBytes[i] = (byte)((GetHexVal(signatureSpaceless[i * 2]) << 4) + (GetHexVal(signatureSpaceless[(i * 2) + 1])));
            }

            long index = module.ToInt64();
            long maxIndex = index + moduleSize;
            long tmpAddress = 0;
            int processed = 0;

            while (index < maxIndex)
            {
                if (signatureNullBytes[processed] || *(byte*)index == signatureBytes[processed])
                {
                    if (processed == 0)
                        tmpAddress = index;

                    ++processed;

                    if (processed == signatureLength)
                        return (IntPtr)tmpAddress;
                }
                else
                {
                    processed = 0;
                }

                ++index;
            }

            return IntPtr.Zero;
        }

        public static IntPtr ResolveRelativeInstruction(IntPtr instruction)
        {
            byte opcode = instruction.GetByte();
            if (opcode != 0xE8 && opcode != 0xE9)
                return IntPtr.Zero;

            return ResolvePtrOffset(instruction + 1, instruction + 5); // CALL: E8 [rel32] / JMP: E9 [rel32]
        }

        public static IntPtr ResolvePtrOffset(IntPtr offset32Ptr, IntPtr nextInstructionPtr)
        {
            uint jmpOffset = offset32Ptr.GetUInt();
            uint valueUInt = new ConvertClass() { valueULong = (ulong)nextInstructionPtr.ToInt64() }.valueUInt;
            long delta = nextInstructionPtr.ToInt64() - valueUInt;
            uint newPtrInt = unchecked(valueUInt + jmpOffset);
            return new IntPtr(newPtrInt + delta);
        }



        // source: https://stackoverflow.com/a/9995303
        private static int GetHexVal(char hex)
        {
            int val = (int)hex;
            return val - (val < 58 ? 48 : (val < 97 ? 55 : 87));
        }



        [StructLayout(LayoutKind.Explicit)]
        private class ConvertClass
        {
            [FieldOffset(0)]
            public ulong valueULong;

            [FieldOffset(0)]
            public uint valueUInt;
        }
    }
}
