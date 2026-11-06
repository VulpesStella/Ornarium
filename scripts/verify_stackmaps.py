"""Controlled verifier experiment, not a claim that stock Forge strips frames.

Remove StackMapTable with ASM4 from a branch-containing fixture, then verify the
expected Java 6 fallback / Java 7 rejection on a real Java 7 or 8 VM.
"""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

SOURCE = '''import java.io.*;
import java.util.jar.*;
import org.objectweb.asm.*;
public final class StackMapProbe {
    static class Loader extends ClassLoader {
        Class<?> loadBytes(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
    }
    public static void main(String[] args) throws Exception {
        byte[] original;
        JarFile jar = new JarFile(args[0]);
        try {
            InputStream input = jar.getInputStream(jar.getJarEntry("example/probe/FrameProbe.class"));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            input.close(); original = bytes.toByteArray();
        } finally { jar.close(); }
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(original).accept(writer, ClassReader.SKIP_FRAMES);
        boolean rejected = false;
        try {
            Class<?> probe = new Loader().loadBytes(writer.toByteArray());
            Object instance = probe.newInstance();
            Object result = probe.getMethod("select", boolean.class).invoke(instance, true);
            if (!Integer.valueOf(7).equals(result)) throw new AssertionError("Wrong branch result");
        } catch (VerifyError expected) {
            rejected = true;
        }
        boolean expectRejection = Integer.parseInt(args[1]) >= 7;
        if (rejected != expectRejection)
            throw new AssertionError("Unexpected frame verifier outcome: rejected=" + rejected);
        System.out.println("FRAME_STRIP_EXPECTED_RESULT target=" + args[1] + " rejected=" + rejected);
    }
}
'''


def verify(jar, asm, java, target, log):
    javac = Path(os.environ['JAVA_HOME']) / 'bin' / ('javac.exe' if os.name == 'nt' else 'javac')
    with tempfile.TemporaryDirectory(prefix='retrospectum-stackmaps-') as directory:
        source = Path(directory) / 'StackMapProbe.java'
        source.write_text(SOURCE, encoding='utf-8')
        subprocess.run([str(javac), '-source', '7', '-target', '7', '-cp', str(asm), str(source)],
                       stdout=log, stderr=subprocess.STDOUT, check=True, timeout=60)
        subprocess.run([str(java), '-Xverify:all', '-cp', directory + os.pathsep + str(asm),
                        'StackMapProbe', str(jar), str(target)],
                       stdout=log, stderr=subprocess.STDOUT, check=True, timeout=30)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--asm', type=Path, required=True)
    parser.add_argument('--java', type=Path, required=True)
    parser.add_argument('--target', type=int, choices=(6, 7), required=True)
    args = parser.parse_args()
    import sys
    verify(args.jar.resolve(), args.asm.resolve(), args.java.resolve(), args.target, sys.stdout)
