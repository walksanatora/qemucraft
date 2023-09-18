package net.walksanator.qemucraft;

import dev.architectury.injectables.annotations.ExpectPlatform;


public class ShaderExpectPlatform {
    @ExpectPlatform
    public static int getShader() {
        throw new AssertionError();
    }
}
