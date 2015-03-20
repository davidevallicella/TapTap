package com.cellasoft.taptap.utils;

/**
 * Created by netsysco on 22/02/2015.
 */
public class Utils {
    public static final double GYRO_NOISE_LIMIT = 0.06;
    public static final int    IDX_X            = 0;
    public static final int    IDX_Y            = 1;
    public static final int    IDX_Z            = 2;

    public static double gyroNoiseLimiter(double gyroValue) {
        double v = gyroValue;
        if (Math.abs(v) < GYRO_NOISE_LIMIT)
            v = 0.0;
        return v;
    }

    public static double[] rotateToEarth(double diff[], double simGravity[]) {
        double rotatedDiff[] = new double[3];
        rotatedDiff[IDX_X] = diff[IDX_X];
        rotatedDiff[IDX_Y] = diff[IDX_Y];
        rotatedDiff[IDX_Z] = diff[IDX_Z];
        double gravity[] = new double[3];
        gravity[IDX_X] = simGravity[IDX_X];
        gravity[IDX_Y] = simGravity[IDX_Y];
        gravity[IDX_Z] = simGravity[IDX_Z];
        double dz = Math.atan2(gravity[IDX_Y], gravity[IDX_X]);
        dz = fixAtanDegree(dz, gravity[IDX_Y], gravity[IDX_X]);
        rotz(rotatedDiff, -dz);
        rotz(gravity, -dz);
        double dy = Math.atan2(gravity[IDX_X], gravity[IDX_Z]);
        dy = fixAtanDegree(dy, gravity[IDX_X], gravity[IDX_Z]);
        roty(rotatedDiff, -dy);
        return rotatedDiff;
    }

    public static void rotz(double vec[], double dz) {
        double x = vec[IDX_X];
        double y = vec[IDX_Y];
        vec[IDX_X] = x * Math.cos(dz) - y * Math.sin(dz);
        vec[IDX_Y] = x * Math.sin(dz) + y * Math.cos(dz);
    }

    public static void rotx(double vec[], double dx) {
        double y = vec[IDX_Y];
        double z = vec[IDX_Z];
        vec[IDX_Y] = (y * Math.cos(dx) - z * Math.sin(dx));
        vec[IDX_Z] = (y * Math.sin(dx) + z * Math.cos(dx));
    }

    public static void roty(double vec[], double dy) {
        double x = vec[IDX_X];
        double z = vec[IDX_Z];
        vec[IDX_Z] = z * Math.cos(dy) - x * Math.sin(dy);
        vec[IDX_X] = z * Math.sin(dy) + x * Math.cos(dy);
    }

    public static double fixAtanDegree(double deg, double y, double x) {
        double rdeg = deg;
        if ((x < 0.0) && (y > 0.0))
            rdeg = Math.PI - deg;
        if ((x < 0.0) && (y < 0.0))
            rdeg = Math.PI + deg;
        return rdeg;
    }

    public static double[] vecdiff(double v1[], double v2[]) {
        double diff[] = new double[3];
        diff[IDX_X] = v1[IDX_X] - v2[IDX_X];
        diff[IDX_Y] = v1[IDX_Y] - v2[IDX_Y];
        diff[IDX_Z] = v1[IDX_Z] - v2[IDX_Z];
        return diff;
    }

    public static boolean isBetween(double x, long lower, long upper) {
        return lower <= x && x <= upper;
    }
}
