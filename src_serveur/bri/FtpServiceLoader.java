package bri;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/** One loader per version. Programmer classes never fall back to the server classpath. */
public final class FtpServiceLoader extends ClassLoader {
    private static final int MAX_CLASS_BYTES = 4 * 1024 * 1024;
    private final URL root;
    private final String owner;

    public FtpServiceLoader(String ftpUrl, String owner) throws IOException {
        super(Service.class.getClassLoader());
        this.owner = owner;
        root = URI.create(ftpUrl.endsWith("/") ? ftpUrl : ftpUrl + "/").toURL();
        if (!root.getProtocol().equals("ftp")) throw new IOException("Une URL ftp:// est requise.");
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> type = findLoadedClass(name);
            if (type == null) {
                type = name.startsWith(owner + ".") ? findClass(name) : super.loadClass(name, false);
            }
            if (resolve) resolveClass(type);
            return type;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (!name.substring(0, Math.max(0, name.lastIndexOf('.'))).equals(owner)) {
            throw new ClassNotFoundException("Les classes FTP doivent appartenir au package " + owner);
        }
        try {
            URLConnection connection = new URL(root, name.replace('.', '/') + ".class").openConnection();
            connection.setUseCaches(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            try (InputStream input = connection.getInputStream()) {
                byte[] bytes = input.readNBytes(MAX_CLASS_BYTES + 1);
                if (bytes.length > MAX_CLASS_BYTES) throw new IOException("Classe trop volumineuse.");
                return defineClass(name, bytes, 0, bytes.length);
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ClassNotFoundException("Classe FTP inaccessible: " + name, e);
        }
    }
}
