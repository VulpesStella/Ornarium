package toolchain.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/** A real ASM4 coremod edit; preserves frames in the other probe methods. */
public final class ProbeTransformer implements IClassTransformer {
    public byte[] transform(String name, String transformedName, byte[] input) {
        if (!"example.probe.BranchProbe".equals(transformedName)) return input;
        ClassNode node = new ClassNode();
        new ClassReader(input).accept(node, 0);
        for (Object entry : node.methods) {
            MethodNode method = (MethodNode) entry;
            if (!"transformed".equals(method.name) || !"()Z".equals(method.desc)) continue;
            method.instructions.clear();
            method.instructions.add(new InsnNode(Opcodes.ICONST_1));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
            method.tryCatchBlocks.clear();
            method.localVariables = null;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
