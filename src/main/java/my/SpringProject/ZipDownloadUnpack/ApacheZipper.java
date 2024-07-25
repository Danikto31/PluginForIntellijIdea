package my.SpringProject.ZipDownloadUnpack;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
public class ApacheZipper {

    public static File DownloadZip(String DpathToZip, VirtualFile[] files) throws IOException {
        String PathToDirectory = files[0].getPath();

       URL url = new URL(DpathToZip);
       File dFile = new File(PathToDirectory,"project.zip");

        FileUtils.forceMkdir(dFile);

        FileUtils.copyURLToFile(url,dFile);
        return dFile;
    }


    public static void unzipFile(File zipFile, VirtualFile[] files) throws IOException {
        String destinationDir = files[0].getPath();
        try (ZipFile zip = new ZipFile.Builder().setFile(zipFile).get()) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                File outFile = new File(destinationDir, entry.getName());

                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(outFile);
                } else {
                    FileUtils.forceMkdirParent(outFile);
                    try (InputStream is = zip.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(outFile)) {
                        IOUtils.copy(is, fos);
                    }
                }
            }
        }
        Files.delete(Paths.get(zipFile.getPath()));
    }
}
