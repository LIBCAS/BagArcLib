package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;


/**
 *
 * @author Gabriela Melingerová
 */
public class ZIPUtils {
    
    private static final int BUFFER_SIZE = 4096;
    private List <String> fileList;
    
    public ZIPUtils() {
        fileList = new ArrayList <String>();
    }
    
    /**
     * Zips the folder
     * @param srcFolder - destination of the folder which we want to zip
     * @param destZipFile - destination of the desire zip file
     */
    public static void zipIt(File srcFolder, File destZipFile) throws ZipException {
        ZipFile zipfile = new ZipFile(destZipFile);
        ZipParameters  parameters = new ZipParameters();
        zipfile.addFolder(srcFolder, parameters);
    }
    
    /**
     * Unzips the file
     * @param srcZipFile - zip file which we want to unzip
     * @param destFolder - destination where we want to unzip
     */
    public static void unzip(String srcZipFile, String destFolder) {
        try {
             ZipFile zipFile = new ZipFile(srcZipFile);
             zipFile.extractAll(destFolder);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }
}
