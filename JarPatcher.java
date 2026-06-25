import java.io.*;
import java.util.jar.*;
import java.util.zip.*;

public class JarPatcher {
    public static void main(String[] args) throws Exception {
        String inputJar = args[0];
        String updatedCommonJar = args[1];
        String outputJar = args[2];
        
        byte[] updatedCommon = readAllBytes(new File(updatedCommonJar));
        
        try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {
            
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String name = entry.getName();
                
                if (name.endsWith("/")) {
                    jos.putNextEntry(new JarEntry(name));
                    jos.closeEntry();
                    continue;
                }
                
                if (name.equals("BOOT-INF/lib/share-common-1.0.0.jar")) {
                    // Replace with updated version - stored (no compression)
                    JarEntry newEntry = new JarEntry(name);
                    newEntry.setMethod(ZipEntry.STORED);
                    newEntry.setSize(updatedCommon.length);
                    newEntry.setCrc(computeCrc(updatedCommon));
                    jos.putNextEntry(newEntry);
                    jos.write(updatedCommon);
                    jos.closeEntry();
                    System.out.println("Replaced: " + name);
                } else {
                    boolean isNestedJar = name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar");
                    byte[] data = readAllBytes(jis);
                    
                    JarEntry newEntry = new JarEntry(name);
                    if (isNestedJar) {
                        newEntry.setMethod(ZipEntry.STORED);
                        newEntry.setSize(data.length);
                        newEntry.setCrc(computeCrc(data));
                    }
                    jos.putNextEntry(newEntry);
                    jos.write(data);
                    jos.closeEntry();
                }
            }
        }
        System.out.println("Patched JAR created: " + outputJar);
    }
    
    static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }
    
    static byte[] readAllBytes(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return readAllBytes(fis);
        }
    }
    
    static long computeCrc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
}