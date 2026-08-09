package com.erlavush.rtcolony.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SodiumMixinCompatibilityTest {
    @Test
    void pinnedSodiumContainsEveryWorldOnlyMixinTargetMethod() throws IOException {
        assertMethod(
                "net/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer",
                "renderModel",
                "(Lnet/minecraft/client/resources/model/BakedModel;"
                        + "Lnet/minecraft/world/level/block/state/BlockState;"
                        + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"
        );
        assertMethod(
                "net/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer",
                "getVisibleFaces",
                "(IIIIII)I"
        );
        assertMethod(
                "net/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager",
                "shouldUseOcclusionCulling",
                "(Lnet/minecraft/client/Camera;Z)Z"
        );
    }

    private static void assertMethod(String owner, String name, String descriptor) throws IOException {
        Set<MethodSignature> methods = readMethods(owner);
        assertTrue(
                methods.contains(new MethodSignature(name, descriptor)),
                () -> owner + " no longer provides " + name + descriptor
        );
    }

    private static Set<MethodSignature> readMethods(String owner) throws IOException {
        String resourceName = owner + ".class";
        InputStream stream = SodiumMixinCompatibilityTest.class
                .getClassLoader()
                .getResourceAsStream(resourceName);
        assertNotNull(stream, () -> "Pinned Sodium class is missing: " + resourceName);

        try (stream) {
            Set<MethodSignature> methods = new HashSet<>();
            ClassReader reader = new ClassReader(stream);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(
                        int access,
                        String name,
                        String descriptor,
                        String signature,
                        String[] exceptions
                ) {
                    methods.add(new MethodSignature(name, descriptor));
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return methods;
        }
    }

    private record MethodSignature(String name, String descriptor) {
    }
}
