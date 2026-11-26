import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Rebuilds the pinned local AAR with only the loopback HTTP status mapping corrected. */
public final class PatchLocalhostHttpStatus {
    private static final String ORIGINAL = "44c07852a524e43bc978f8e816c9b2680a0e3a999bec09d8049509a3db898d0e";
    private static final String TARGET = "androidx/media3/datasource/LocalhostZeroCopyDataSource";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected original AAR and new AAR paths");
        byte[] original = Files.readAllBytes(Path.of(args[0]));
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original));
        if (!ORIGINAL.equals(digest)) throw new IllegalArgumentException("Unexpected upstream AAR SHA-256");
        Files.write(Path.of(args[1]), change(original, "classes.jar",
                jar -> change(jar, TARGET + ".class", PatchLocalhostHttpStatus::patch)));
    }

    private static byte[] change(byte[] archive, String member, UnaryOperator<byte[]> transform) {
        int matches = 0;
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive));
             ZipOutputStream output = new ZipOutputStream(result)) {
            for (ZipEntry entry; (entry = input.getNextEntry()) != null; ) {
                ZipEntry copy = new ZipEntry(entry.getName());
                if (entry.getTime() >= 0) copy.setTime(entry.getTime());
                output.putNextEntry(copy);
                byte[] contents = input.readAllBytes();
                if (entry.getName().equals(member)) {
                    matches++;
                    contents = transform.apply(contents);
                }
                output.write(contents);
                output.closeEntry();
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read archive member " + member, error);
        }
        if (matches != 1) throw new IllegalStateException("Unexpected count for " + member);
        return result.toByteArray();
    }

    private static byte[] patch(byte[] bytes) {
        ClassNode type = new ClassNode();
        new ClassReader(bytes).accept(type, 0);
        if (!TARGET.equals(type.name)) throw new IllegalStateException("Wrong class");
        var open = type.methods.stream().filter(method -> method.name.equals("open")
                && method.desc.equals("(Landroidx/media3/datasource/DataSpec;)J")).findFirst().orElseThrow();
        var instructions = open.instructions.toArray();
        int statusLineSlot = -1;
        int replacements = 0;
        for (int i = 1; i < instructions.length; i++) {
            if (instructions[i] instanceof LdcInsnNode literal
                    && literal.cst instanceof String value && value.trim().equals("416")) {
                var previous = instructions[i].getPrevious();
                while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
                if (previous instanceof VarInsnNode load && load.getOpcode() == Opcodes.ALOAD) {
                    statusLineSlot = load.var;
                }
            }
            if (statusLineSlot >= 0 && instructions[i] instanceof IntInsnNode push
                    && push.getOpcode() == Opcodes.SIPUSH && (push.operand == 416 || push.operand == 400)) {
                InsnList parsed = new InsnList();
                parsed.add(new VarInsnNode(Opcodes.ALOAD, statusLineSlot));
                parsed.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TARGET, "parseErrorStatusCode",
                        "(Ljava/lang/String;)I", false));
                open.instructions.insertBefore(push, parsed);
                open.instructions.remove(push);
                replacements++;
            }
        }
        if (replacements != 2) throw new IllegalStateException("Unexpected status mapping");

        ClassNode tool = new ClassNode();
        try (InputStream compiled = PatchLocalhostHttpStatus.class.getResourceAsStream(
                "/PatchLocalhostHttpStatus.class")) {
            new ClassReader(compiled).accept(tool, ClassReader.SKIP_DEBUG);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read patch helper", error);
        }
        type.methods.add(tool.methods.stream()
                .filter(method -> method.name.equals("parseErrorStatusCode")).findFirst().orElseThrow());
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static int parseErrorStatusCode(String statusLine) {
        String[] parts = statusLine.split(" +", 3);
        if (parts.length >= 2 && parts[0].startsWith("HTTP/") && parts[1].matches("[1-5][0-9]{2}")) {
            return Integer.parseInt(parts[1]);
        }
        return 400;
    }
}
