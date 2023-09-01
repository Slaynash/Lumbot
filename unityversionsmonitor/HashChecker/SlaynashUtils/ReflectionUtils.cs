using System;
using System.Reflection;

namespace SlaynashUtils
{
    public static class ReflectionUtils
    {
        internal static IntPtr GetFunctionPointerFromMethod<T>(string methodName) =>
            typeof(T).GetMethod(methodName, BindingFlags.NonPublic | BindingFlags.Static).MethodHandle.GetFunctionPointer();
    }
}
