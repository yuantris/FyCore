package io.core.constant

import android.os.Build

const val ANDROID_15 = Build.VERSION_CODES.VANILLA_ICE_CREAM
const val ANDROID_14 = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
const val ANDROID_13 = Build.VERSION_CODES.TIRAMISU
const val ANDROID_12_L = Build.VERSION_CODES.S_V2
const val ANDROID_12 = Build.VERSION_CODES.S
const val ANDROID_11 = Build.VERSION_CODES.R
const val ANDROID_10 = Build.VERSION_CODES.Q
const val ANDROID_9 = Build.VERSION_CODES.P
const val ANDROID_8_1 = Build.VERSION_CODES.O_MR1
const val ANDROID_8 = Build.VERSION_CODES.O
const val ANDROID_7_1 = Build.VERSION_CODES.N_MR1
const val ANDROID_7 = Build.VERSION_CODES.N
const val ANDROID_6 = Build.VERSION_CODES.M
const val ANDROID_5_1 = Build.VERSION_CODES.LOLLIPOP_MR1
const val ANDROID_5 = Build.VERSION_CODES.LOLLIPOP
const val ANDROID_4_4 = Build.VERSION_CODES.KITKAT
const val ANDROID_4_3 = Build.VERSION_CODES.JELLY_BEAN_MR2
const val ANDROID_4_2 = Build.VERSION_CODES.JELLY_BEAN_MR1
const val ANDROID_4_1 = Build.VERSION_CODES.JELLY_BEAN
const val ANDROID_4_0 = Build.VERSION_CODES.ICE_CREAM_SANDWICH

// Android版本枚举
enum class AndroidVer {
    A15,
    A14,
    A13,
    A12_L,
    A12,
    A11,
    A10,
    A9,
    A8_1,
    A8,
    A7_1,
    A7,
    A6,
    A5_1,
    A5,
    A4_4,
    A4_3,
    A4_2,
    A4_1,
    A4_0,
    UNKNOWN;

    val versionCode: Int
        get() = when (this) {
            A15 -> ANDROID_15
            A14 -> ANDROID_14
            A13 -> ANDROID_13
            A12_L -> ANDROID_12_L
            A12 -> ANDROID_12
            A11 -> ANDROID_11
            A10 -> ANDROID_10
            A9 -> ANDROID_9
            A8_1 -> ANDROID_8_1
            A8 -> ANDROID_8
            A7_1 -> ANDROID_7_1
            A7 -> ANDROID_7
            A6 -> ANDROID_6
            A5_1 -> ANDROID_5_1
            A5 -> ANDROID_5
            A4_4 -> ANDROID_4_4
            A4_3 -> ANDROID_4_3
            A4_2 -> ANDROID_4_2
            A4_1 -> ANDROID_4_1
            A4_0 -> ANDROID_4_0
            // 默认为当前版本
            UNKNOWN -> Build.VERSION.SDK_INT
        }
}