﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace restogram.NET
{
    static class StringUtils
    {
        public static string EncodeTo64(this string toEncode)
        {
            byte[] toEncodeAsBytes = System.Text.Encoding.UTF8.GetBytes(toEncode);
            string returnValue = System.Convert.ToBase64String(toEncodeAsBytes);
            return returnValue;
        }

        public static string DecodeFrom64(this string encodedData)
        {
            byte[] encodedDataAsBytes = System.Convert.FromBase64String(encodedData);
            string returnValue = System.Text.Encoding.UTF8.GetString(encodedDataAsBytes);
            return returnValue;

        }
    }
}
