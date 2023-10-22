package com.cxapp.gcmodel;

import android.os.Build;

public class g {
    public static String dd() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer);
        }
    }

    public static void hhh() {
//        String deviceDetails = dd();
//        if (!deviceDetails.equals("Motorola")) {
//            System.exit(0);
//            throw new RuntimeException("api low");
//
//        }
    }

    private static String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        char firstChar = str.charAt(0);
        if (Character.isUpperCase(firstChar)) {
            return str;
        } else {
            return Character.toUpperCase(firstChar) + str.substring(1);
        }
    }

    public static void simulateArrayIndexOutOfBounds() {
        int[] sampleArray = new int[5];
        int value = sampleArray[10];
    }

    // 模拟空指针异常
    public static void simulateNullPointerException() {
        String nullString = null;
        int length = nullString.length();
    }
}