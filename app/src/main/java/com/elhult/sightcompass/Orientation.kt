package com.elhult.sightcompass

import android.hardware.SensorManager
import kotlin.math.PI

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Orientation(
    val azimuth: Float,
    val pitch: Float,
    val roll: Float
) {

    val compassVal: Double
        get() {
            val azimuthDeg = azimuth * 180 / PI
            return if (azimuthDeg < 0) azimuthDeg + 360 else azimuthDeg
        }

    companion object {
        /**
         * Computes a orientation based on androidSensorRotationVector
         *
         * The rotation vector is in the first 4 components a unit quaternion in the surrounding
         * coordinate system....
         *  X pointing eastwards, parallel to the ground
         *  Y pointing north, parallel to the ground
         *  Z points towards the sky
         * Defining the rotation quaternion representing the phone, which is oriented like...
         *  x axis is "to the right"
         *  y axis is "up"
         *  z axis points "out of the screen"
         *
         * So with the screen facing the sky, and the "up" on the scree to the north pole,
         * the phone is not oriented at all. :)
         * You get the expected azimuth/pitch/roll by combining  getRotationMatrixFromVector
         * and getOrientation. But We want to use the phone in portrait, so in OUR PREFERRED
         * coordinate system we have....
         *   x axis as before
         *   our z is what before was called y
         *   our y is what before was called -z
         *
         *  so we apply the remapCoordinateSystem to fix that! :) This is also explained in https://developer.android.com/reference/android/hardware/SensorManager#remapCoordinateSystem(float[],%20int,%20int,%20float[])
         *
         *
         * See also
         * https://developer.android.com/guide/topics/sensors/sensors_overview#sensors-coords
         * https://developer.android.com/guide/topics/sensors/sensors_motion#sensors-motion-rotate
         */
        fun fromArray(androidSensorRotationVector: FloatArray): Orientation {
            val outArr = FloatArray(3)
            val defaultR = FloatArray(9) // rotation matrix in default coordinates
            val customR = FloatArray(9) // rotation matrix in custom coordinates
            SensorManager.getRotationMatrixFromVector(defaultR, androidSensorRotationVector)

            SensorManager.remapCoordinateSystem(
                defaultR,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                customR
            )
            SensorManager.getOrientation(customR, outArr)
            return Orientation(outArr[0], outArr[1], outArr[2])
        }
    }
}