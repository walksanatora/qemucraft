package net.walksanator.qemucraft.util.math;

import org.joml.Matrix4f;

public interface Matrix4fExt {

    void setData(float[] values);

    float[] getData();

    static Matrix4fExt from(Matrix4f self) {
        return (Matrix4fExt) (Object) self;
    }

}
