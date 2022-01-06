package bagarclib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import utils.ZIPUtils;

/**
 *
 * @author Gabriela Melingerová
 */
public class UnbagArcLib {
    
    /**
     *  Creates the archive folder from the bagArcLib file - archive_xx.zip
     *
     * @param pathToFolder - path to the main folder which contains the bagArcLib files ex: /home/gabriela/Plocha/26.2.2020/bagit_d7a85ed3-5131-4298-a0fa-b061a6b20d46
     */
    public static void unbagIt(String pathToFolder) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, FileNotFoundException, Exception {
        File folder = new File(pathToFolder);
        File[] files = folder.listFiles();
        
        for (File file : files) {
            if (file.isFile() && !file.isHidden()) {
                String extension = utils.Utils.getExtension(file.getAbsolutePath());
                if (extension.equals("zip")) {
                    ZIPUtils.unzip(file.getAbsolutePath(), pathToFolder);
                    
                    //08b27a0-ed2b-4b83-b2ea-20e4a3bb5b80
                    String uuidArchive = utils.Utils.getUuidInFile(file.getName(), "zip", 0);
                    
                    // archive_408b27a0-ed2b-4b83-b2ea-20e4a3bb5b80
                    String archive = file.getName().replace(".zip", "");

                    // delete archive_xxxxx.zip
                    file.delete();
                    
                    String archivePath = pathToFolder + "/" + archive;
                    File archiveFolder = new File(archivePath);
                    File[] archiveFiles = archiveFolder.listFiles();
        
                    for (File archiveFile : archiveFiles) {
                        if (archiveFile.isDirectory()) {
                            File uuidArchivePath = new File(archivePath + "/" + uuidArchive);
                            archiveFile.renameTo(uuidArchivePath);
                        }
                        if (archiveFile.isFile()) {
                            archiveFile.delete();
                        }
                    }
                }
                if (extension.equals("sums")) {
                    file.delete();
                }
            }
        }
    }   
}
