package net.walksanator.qemucraft.util.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.FloatBuffer;
import java.util.Objects;

import org.joml.Matrix3f;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public final class Mat3 {

    public static final Mat3 IDENTITY = new Mat3(
        1, 0, 0,
        0, 1, 0,
        0, 0, 1
    );

    public final float c00, c01, c02;
    public final float c10, c11, c12;
    public final float c20, c21, c22;

    private Vec3 r0, r1, r2;
    private Vec3 c0, c1, c2;

    private Mat3 inverse;
    private boolean hasInverse = true;

    public Mat3(
        float c00, float c01, float c02,
        float c10, float c11, float c12,
        float c20, float c21, float c22
    ) {
        this.c00 = c00;
        this.c01 = c01;
        this.c02 = c02;
        this.c10 = c10;
        this.c11 = c11;
        this.c12 = c12;
        this.c20 = c20;
        this.c21 = c21;
        this.c22 = c22;
    }

    public Mat3 scale(float x, float y, float z) {
        return mul(new Mat3(
            x, 0, 0,
            0, y, 0,
            0, 0, z
        ));
    }

    public Mat3 rotate(float x, float y, float z, float angle) {
        float c = cosd(-angle);
        float s = sind(-angle);
        float t = 1 - c;

        return mul(new Mat3(
            t * x * x + c, t * x * y - s * z, t * x * z + s * y,
            t * x * y + s * z, t * y * y + c, t * y * z - s * x,
            t * x * z - s * y, t * y * z + s * x, t * z * z + c
        ));
    }

    public Mat3 mul(Mat3 other) {
        return new Mat3(
            getR0().dot(other.getC0()), getR0().dot(other.getC1()), getR0().dot(other.getC2()),
            getR1().dot(other.getC0()), getR1().dot(other.getC1()), getR1().dot(other.getC2()),
            getR2().dot(other.getC0()), getR2().dot(other.getC1()), getR2().dot(other.getC2())
        );
    }

    public Vec3 mul(Vec3 other) {
        return new Vec3(getR0().dot(other), getR1().dot(other), getR2().dot(other));
    }

    public void intoBuffer(FloatBuffer fb) {
        // @formatter:off
        fb.put(c00); fb.put(c10); fb.put(c20);
        fb.put(c01); fb.put(c11); fb.put(c21);
        fb.put(c02); fb.put(c12); fb.put(c22);
        // @formatter:on
    }

    public static Mat3 fromBuffer(FloatBuffer fb) {
        float[] data = new float[9];
        fb.get(data);
        return new Mat3(
            data[0], data[3], data[6],
            data[1], data[4], data[7],
                data[2], data[5], data[8]
        );
    }

    @Environment(EnvType.CLIENT)
    public Matrix3f toMatrix3f() {
        Matrix3f mat = new Matrix3f();
        intoMatrix3f(mat);
        return mat;
    }

    @Environment(EnvType.CLIENT)
    public void intoMatrix3f(Matrix3f target) {
        Matrix3fExt.from(target).setData(toArray());
    }

    @Environment(EnvType.CLIENT)
    public static Mat3 fromMatrix3f(Matrix3f mat) {
        return Mat3.fromArray(Matrix3fExt.from(mat).getData());
    }

    public float[] toArray() {
        return new float[]{
                c00, c01, c02,
                c10, c11, c12,
            c20, c21, c22
        };
    }

    public static Mat3 fromArray(float[] array) {
        return new Mat3(
            array[0], array[1], array[2],
            array[3], array[4], array[5],
            array[6], array[7], array[8]
        );
    }

    // @formatter:off
    public Vec3 getR0() { if (r0 == null) r0 = new Vec3(c00, c01, c02); return r0; }
    public Vec3 getR1() { if (r1 == null) r1 = new Vec3(c10, c11, c12); return r1; }
    public Vec3 getR2() { if (r2 == null) r2 = new Vec3(c20, c21, c22); return r2; }
    public Vec3 getC0() { if (c0 == null) c0 = new Vec3(c00, c10, c20); return c0; }
    public Vec3 getC1() { if (c1 == null) c1 = new Vec3(c01, c11, c21); return c1; }
    public Vec3 getC2() { if (c2 == null) c2 = new Vec3(c02, c12, c22); return c2; }
    // @formatter:on

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mat3 mat3 = (Mat3) o;
        return Float.compare(mat3.c00, c00) == 0 &&
            Float.compare(mat3.c01, c01) == 0 &&
            Float.compare(mat3.c02, c02) == 0 &&
            Float.compare(mat3.c10, c10) == 0 &&
            Float.compare(mat3.c11, c11) == 0 &&
            Float.compare(mat3.c12, c12) == 0 &&
            Float.compare(mat3.c20, c20) == 0 &&
            Float.compare(mat3.c21, c21) == 0 &&
            Float.compare(mat3.c22, c22) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(c00, c01, c02, c10, c11, c12, c20, c21, c22, r0, r1, r2, c0, c1, c2, inverse, hasInverse);
    }

    @Override
    public String toString() {
        return String.format("mat4(%f, %f, %f; %f, %f, %f; %f, %f, %f;)", c00, c01, c02, c10, c11, c12, c20, c21, c22);
    }

    private static float cosd(float angle) {
        return (float) cos(angle * (2 * PI) / 360.0);
    }

    private static float sind(float angle) {
        return (float) sin(angle * (2 * PI) / 360.0);
    }

}
