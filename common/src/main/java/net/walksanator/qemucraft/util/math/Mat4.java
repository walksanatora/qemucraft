package net.walksanator.qemucraft.util.math;//code copied from https://raw.githubusercontent.com/qcommon/croco/master/src/main/java/net/dblsaiko/qcommon/croco/Mat4.java

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.util.Arrays;

import static java.lang.Math.*;

public final class Mat4 {

    public static final Mat4 IDENTITY = new Mat4(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    );

    public final float c00, c01, c02, c03;
    public final float c10, c11, c12, c13;
    public final float c20, c21, c22, c23;
    public final float c30, c31, c32, c33;

    private Vec4 r0, r1, r2, r3;
    private Vec4 c0, c1, c2, c3;

    private Mat4 inverse;
    private boolean hasInverse = true;

    public Mat4(
            float c00, float c01, float c02, float c03,
            float c10, float c11, float c12, float c13,
            float c20, float c21, float c22, float c23,
            float c30, float c31, float c32, float c33
    ) {
        this.c00 = c00;
        this.c01 = c01;
        this.c02 = c02;
        this.c03 = c03;
        this.c10 = c10;
        this.c11 = c11;
        this.c12 = c12;
        this.c13 = c13;
        this.c20 = c20;
        this.c21 = c21;
        this.c22 = c22;
        this.c23 = c23;
        this.c30 = c30;
        this.c31 = c31;
        this.c32 = c32;
        this.c33 = c33;
    }

    public Mat4 translate(float x, float y, float z) {
        return mul(new Mat4(
                1, 0, 0, x,
                0, 1, 0, y,
                0, 0, 1, z,
                0, 0, 0, 1
        ));
    }

    public Mat4 scale(float x, float y, float z) {
        return mul(new Mat4(
                x, 0, 0, 0,
                0, y, 0, 0,
                0, 0, z, 0,
                0, 0, 0, 1
        ));
    }

    public Mat4 rotate(float x, float y, float z, float angle) {
        float c = cosd(-angle);
        float s = sind(-angle);
        float t = 1 - c;

        return mul(new Mat4(
                t * x * x + c, t * x * y - s * z, t * x * z + s * y, 0f,
                t * x * y + s * z, t * y * y + c, t * y * z - s * x, 0f,
                t * x * z - s * y, t * y * z + s * x, t * z * z + c, 0f,
                0f, 0f, 0f, 1f
        ));
    }

    public Mat4 translate(Vec3 xyz) {
        return translate(xyz.x, xyz.y, xyz.z);
    }

    public Mat4 mul(Mat4 other) {
        return new Mat4(
                getR0().dot(other.getC0()), getR0().dot(other.getC1()), getR0().dot(other.getC2()), getR0().dot(other.getC3()),
                getR1().dot(other.getC0()), getR1().dot(other.getC1()), getR1().dot(other.getC2()), getR1().dot(other.getC3()),
                getR2().dot(other.getC0()), getR2().dot(other.getC1()), getR2().dot(other.getC2()), getR2().dot(other.getC3()),
                getR3().dot(other.getC0()), getR3().dot(other.getC1()), getR3().dot(other.getC2()), getR3().dot(other.getC3())
        );
    }

    public Vec4 mul(Vec4 other) {
        return new Vec4(getR0().dot(other), getR1().dot(other), getR2().dot(other), getR3().dot(other));
    }

    public Vec3 mul(Vec3 other) {
        return mul(other.toVec4()).toVec3();
    }

    public static Mat4 perspective(float fovY, float aspect, float zNear, float zFar) {
        float halfFovyRadians = (fovY / 2f) / 180 * (float) PI;
        float range = (float) Math.tan(halfFovyRadians) * zNear;
        float left = -range * aspect;
        float right = range * aspect;
        float bottom = -range;

        return new Mat4(
                2f * zNear / (right - left), 0f, 0f, 0f,
                0f, 2f * zNear / (range - bottom), 0f, 0f,
                0f, 0f, (-(zFar + zNear) / (zFar - zNear)), -(2f * zFar * zNear) / (zFar - zNear),
                0f, 0f, -1f, 0f
        );
    }

    public static Mat4 frustum(float left, float right, float bottom, float top, float zNear, float zFar) {
        float m00 = 2f * zNear / (right - left);
        float m11 = 2f * zNear / (top - bottom);
        float m02 = (right + left) / (right - left);
        float m12 = (top + bottom) / (top - bottom);
        float m22 = -(zFar + zNear) / (zFar - zNear);
        float m23 = -(2f * zFar * zNear) / (zFar - zNear);

        return new Mat4(
                m00, 0f, m02, 0f,
                0f, m11, m12, 0f,
                0f, 0f, m22, m23,
                0f, 0f, -1f, 0f
        );
    }

    public static Mat4 lookAt(Vec3 eye, Vec3 center, Vec3 up) {
        Vec3 f = center.sub(eye).getNormalized();
        Vec3 s = (f.cross(up.getNormalized())).getNormalized();
        Vec3 u = s.cross(f);

        return new Mat4(
                s.x, s.y, s.z, -s.dot(eye),
                u.x, u.y, u.z, -u.dot(eye),
                -f.x, -f.y, -f.z, f.dot(eye),
                0f, 0f, 0f, 1f
        );
    }

    public static Mat4 ortho(float left, float right, float bottom, float top, float zNear, float zFar) {
        float m00 = 2f / (right - left);
        float m11 = 2f / (top - bottom);
        float m22 = -2f / (zFar - zNear);
        float m03 = -(right + left) / (right - left);
        float m13 = -(top + bottom) / (top - bottom);
        float m23 = -(zFar + zNear) / (zFar - zNear);

        return new Mat4(
                m00, 0f, 0f, m03,
                0f, m11, 0f, m13,
                0f, 0f, m22, m23,
                0f, 0f, 0f, 1f
        );
    }

    public void intoBuffer(FloatBuffer fb) {
        // @formatter:off
        fb.put(c00); fb.put(c10); fb.put(c20); fb.put(c30);
        fb.put(c01); fb.put(c11); fb.put(c21); fb.put(c31);
        fb.put(c02); fb.put(c12); fb.put(c22); fb.put(c32);
        fb.put(c03); fb.put(c13); fb.put(c23); fb.put(c33);
        // @formatter:on
    }

    public static Mat4 fromBuffer(FloatBuffer fb) {
        float[] data = new float[16];
        fb.get(data);
        return new Mat4(
                data[0], data[4], data[8], data[12],
                data[1], data[5], data[9], data[13],
                data[2], data[6], data[10], data[14],
                data[3], data[7], data[11], data[15]
        );
    }

    public Mat3 getRotation() {
        return new Mat3(
                c00, c01, c02,
                c10, c11, c12,
                c20, c21, c22
        );
    }

    @Environment(EnvType.CLIENT)
    public Matrix4f toMatrix4f() {
        Matrix4f mat = new Matrix4f();
        intoMatrix4f(mat);
        return mat;
    }

    @Environment(EnvType.CLIENT)
    public void intoMatrix4f(Matrix4f target) {
        Matrix4fExt.from(target).setData(toArray());
    }

    @Environment(EnvType.CLIENT)
    public static Mat4 fromMatrix4f(Matrix4f mat) {
        return Mat4.fromArray(Matrix4fExt.from(mat).getData());
    }

    public float[] toArray() {
        return new float[]{
                c00, c01, c02, c03,
                c10, c11, c12, c13,
                c20, c21, c22, c23,
                c30, c31, c32, c33
        };
    }

    public static Mat4 fromArray(float[] array) {
        return new Mat4(
                array[0], array[1], array[2], array[3],
                array[4], array[5], array[6], array[7],
                array[8], array[9], array[10], array[11],
                array[12], array[13], array[14], array[15]
        );
    }

    // @formatter:off
    public Vec4 getR0() { if (r0 == null) r0 = new Vec4(c00, c01, c02, c03); return r0; }
    public Vec4 getR1() { if (r1 == null) r1 = new Vec4(c10, c11, c12, c13); return r1; }
    public Vec4 getR2() { if (r2 == null) r2 = new Vec4(c20, c21, c22, c23); return r2; }
    public Vec4 getR3() { if (r3 == null) r3 = new Vec4(c30, c31, c32, c33); return r3; }
    public Vec4 getC0() { if (c0 == null) c0 = new Vec4(c00, c10, c20, c30); return c0; }
    public Vec4 getC1() { if (c1 == null) c1 = new Vec4(c01, c11, c21, c31); return c1; }
    public Vec4 getC2() { if (c2 == null) c2 = new Vec4(c02, c12, c22, c32); return c2; }
    public Vec4 getC3() { if (c3 == null) c3 = new Vec4(c03, c13, c23, c33); return c3; }
    // @formatter:on

    public Mat4 invert() {
        if (hasInverse && inverse == null) {
            Mat4 m = invert0();
            if (m == null) hasInverse = false;
            else inverse = m;
        }

        return inverse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mat4 mat4 = (Mat4) o;
        return Float.compare(mat4.c00, c00) == 0 &&
                Float.compare(mat4.c01, c01) == 0 &&
                Float.compare(mat4.c02, c02) == 0 &&
                Float.compare(mat4.c03, c03) == 0 &&
                Float.compare(mat4.c10, c10) == 0 &&
                Float.compare(mat4.c11, c11) == 0 &&
                Float.compare(mat4.c12, c12) == 0 &&
                Float.compare(mat4.c13, c13) == 0 &&
                Float.compare(mat4.c20, c20) == 0 &&
                Float.compare(mat4.c21, c21) == 0 &&
                Float.compare(mat4.c22, c22) == 0 &&
                Float.compare(mat4.c23, c23) == 0 &&
                Float.compare(mat4.c30, c30) == 0 &&
                Float.compare(mat4.c31, c31) == 0 &&
                Float.compare(mat4.c32, c32) == 0 &&
                Float.compare(mat4.c33, c33) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.toArray());
    }

    @Override
    public String toString() {
        return String.format("mat4(%f, %f, %f, %f; %f, %f, %f, %f; %f, %f, %f, %f; %f, %f, %f, %f)", c00, c01, c02, c03, c10, c11, c12, c13, c20, c21, c22, c23, c30, c31, c32, c33);
    }

    private static float cosd(float angle) {
        return (float) cos(angle * (2 * PI) / 360.0);
    }

    private static float sind(float angle) {
        return (float) sin(angle * (2 * PI) / 360.0);
    }

    @Nullable
    private Mat4 invert0() {
        float[] inv = new float[16];
        float[] m = {c00, c01, c02, c03, c10, c11, c12, c13, c20, c21, c22, c23, c30, c31, c32, c33};

        inv[0] = m[5] * m[10] * m[15] -
                m[5] * m[11] * m[14] -
                m[9] * m[6] * m[15] +
                m[9] * m[7] * m[14] +
                m[13] * m[6] * m[11] -
                m[13] * m[7] * m[10];

        inv[4] = -m[4] * m[10] * m[15] +
                m[4] * m[11] * m[14] +
                m[8] * m[6] * m[15] -
                m[8] * m[7] * m[14] -
                m[12] * m[6] * m[11] +
                m[12] * m[7] * m[10];

        inv[8] = m[4] * m[9] * m[15] -
                m[4] * m[11] * m[13] -
                m[8] * m[5] * m[15] +
                m[8] * m[7] * m[13] +
                m[12] * m[5] * m[11] -
                m[12] * m[7] * m[9];

        inv[12] = -m[4] * m[9] * m[14] +
                m[4] * m[10] * m[13] +
                m[8] * m[5] * m[14] -
                m[8] * m[6] * m[13] -
                m[12] * m[5] * m[10] +
                m[12] * m[6] * m[9];

        float det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (det == 0) return null;
        det = 1 / det;

        inv[1] = -m[1] * m[10] * m[15] +
                m[1] * m[11] * m[14] +
                m[9] * m[2] * m[15] -
                m[9] * m[3] * m[14] -
                m[13] * m[2] * m[11] +
                m[13] * m[3] * m[10];

        inv[5] = m[0] * m[10] * m[15] -
                m[0] * m[11] * m[14] -
                m[8] * m[2] * m[15] +
                m[8] * m[3] * m[14] +
                m[12] * m[2] * m[11] -
                m[12] * m[3] * m[10];

        inv[9] = -m[0] * m[9] * m[15] +
                m[0] * m[11] * m[13] +
                m[8] * m[1] * m[15] -
                m[8] * m[3] * m[13] -
                m[12] * m[1] * m[11] +
                m[12] * m[3] * m[9];

        inv[13] = m[0] * m[9] * m[14] -
                m[0] * m[10] * m[13] -
                m[8] * m[1] * m[14] +
                m[8] * m[2] * m[13] +
                m[12] * m[1] * m[10] -
                m[12] * m[2] * m[9];

        inv[2] = m[1] * m[6] * m[15] -
                m[1] * m[7] * m[14] -
                m[5] * m[2] * m[15] +
                m[5] * m[3] * m[14] +
                m[13] * m[2] * m[7] -
                m[13] * m[3] * m[6];

        inv[6] = -m[0] * m[6] * m[15] +
                m[0] * m[7] * m[14] +
                m[4] * m[2] * m[15] -
                m[4] * m[3] * m[14] -
                m[12] * m[2] * m[7] +
                m[12] * m[3] * m[6];

        inv[10] = m[0] * m[5] * m[15] -
                m[0] * m[7] * m[13] -
                m[4] * m[1] * m[15] +
                m[4] * m[3] * m[13] +
                m[12] * m[1] * m[7] -
                m[12] * m[3] * m[5];

        inv[14] = -m[0] * m[5] * m[14] +
                m[0] * m[6] * m[13] +
                m[4] * m[1] * m[14] -
                m[4] * m[2] * m[13] -
                m[12] * m[1] * m[6] +
                m[12] * m[2] * m[5];

        inv[3] = -m[1] * m[6] * m[11] +
                m[1] * m[7] * m[10] +
                m[5] * m[2] * m[11] -
                m[5] * m[3] * m[10] -
                m[9] * m[2] * m[7] +
                m[9] * m[3] * m[6];

        inv[7] = m[0] * m[6] * m[11] -
                m[0] * m[7] * m[10] -
                m[4] * m[2] * m[11] +
                m[4] * m[3] * m[10] +
                m[8] * m[2] * m[7] -
                m[8] * m[3] * m[6];

        inv[11] = -m[0] * m[5] * m[11] +
                m[0] * m[7] * m[9] +
                m[4] * m[1] * m[11] -
                m[4] * m[3] * m[9] -
                m[8] * m[1] * m[7] +
                m[8] * m[3] * m[5];

        inv[15] = m[0] * m[5] * m[10] -
                m[0] * m[6] * m[9] -
                m[4] * m[1] * m[10] +
                m[4] * m[2] * m[9] +
                m[8] * m[1] * m[6] -
                m[8] * m[2] * m[5];

        for (int i = 0; i < inv.length; i++) {
            inv[i] *= det;
        }

        return Mat4.fromArray(inv);
    }

}
