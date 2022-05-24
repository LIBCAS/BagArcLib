package bagarclib;

import utils.XMLUtils;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import java.io.*;
import java.io.PrintWriter;
import java.io.FileReader;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList; 
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import utils.ZIPUtils;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Gabriela Melingerová
 */

public class BagArcLib {
    
    /**
     *  Creates the bagArcLib file - archive_xx.zip + archive_xx.sum from the archive folder (archive_xx), tar archive (archive_xx.tar), export folder or tar export
     *
     * @param pathToFolder - path to the main folder which contains the archives ex: /home/gabriela/Plocha/26.2.2020/
     */
    public static void bagIt(String pathToFolder) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, FileNotFoundException, Exception {
        File folder = new File(pathToFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile() && !file.isHidden()) {
                String extension = utils.Utils.getExtension(file.getAbsolutePath());
                if (extension.equals("tar")) {
                    bagTarFile(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());
                }
            }
            if (file.isDirectory() && !file.isHidden() && !utils.Utils.containsFolderData(file)) {
                bagFolder(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());
            }
        }
    }
    
    /**
     *  Creates the bagArcLib file - from the tar archive folder - archive_xx.tar or tar export
     *
     * @param pathToFile - path to the tar file ex: /home/gabriela/Plocha/26.2.2020/archive_xxxxx.tar
     * @param rootFolderPath - path to the main folder which contains the archives ex: /home/gabriela/Plocha/26.2.2020/
     */
    private static void bagTarFile(String pathToFile, String rootFolderPath) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, Exception {
        File tar = new File(pathToFile);
        String basePath = utils.Utils.getPathWithoutExtension(pathToFile);
        File dest = new File(rootFolderPath);
        
        // DELETING
        File extracted = new File(basePath);
        if (extracted.exists()) {
            utils.Utils.deleteFolder(extracted);
        }
        // UNTARING
        utils.Utils.unTarFile(tar, dest);
        
        // BAG FOLDER
        bagFolder(basePath, rootFolderPath);

        // DELETE UNTARFOLDER IF IT HAS ONLY ONE FOLDER 
        File untarFolder = new File(basePath);
        if (untarFolder.exists()) {
            utils.Utils.deleteFolder(untarFolder);
        }
    }
    
    /**
     *  Creates the bagArcLib file - archive_xx.zip + archive_xx.sum from the archive folder - archive_xx or export folder
     *
     * @param basePath - path to the archive folder ex: /home/gabriela/Plocha/26.2.2020/archive_xxxxx
     * @param rootFolderPath - path to the main folder which contains archive folders ex: /home/gabriela/Plocha/26.2.2020/
     */
    private static void bagFolder(String basePath, String rootFolderPath) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, Exception {
        Path folder = Paths.get(basePath);
        String folderName = folder.getFileName().toString();
        
        // SKIP BAGARCHIVE FOLDER
        if (folderName.matches("bagarchive")) {
            return;
        }
        
        String nameWithouArchive = utils.Utils.getNameWithoutPrefix(folderName);
        String srcPath = basePath;
        
        
        File archiveDir = new File(srcPath);
        File [] files = archiveDir.listFiles();
        String nameOfFolderUnderArchive = "";
        int amountOfDir = 0;

        for (File file : files) {
            if (file.isDirectory()) {
                nameOfFolderUnderArchive = file.getName();
                amountOfDir++;
            }
        }
        srcPath += "/" + nameOfFolderUnderArchive; 
        
        if (folderName.contains("archive") && amountOfDir > 1) {
            makeArchive(basePath, rootFolderPath);
            return;
        }

        basePath = rootFolderPath + "/" + "bagit_" + nameWithouArchive + "/" + folderName; 
        
        // CREATING FOLDER DATA
        String dataPath = basePath + "/" + "data";
        File destDir = new File(dataPath);
        destDir.mkdir();
        
        // COPYING ALL IN THE FOLDER: from /xx/xx/xx/archive_xxxxx/slozkapodarchivem to /xx/xx/xx/bagit_xxx/archive_xxx/data
        File srcDir = new File(srcPath);
        FileUtils.copyDirectory(srcDir, destDir);
        
        // for archive folder
        if (folderName.contains("archive")) {
            bagRestArchive(basePath, nameWithouArchive, rootFolderPath);
        }
        // for export folder
        else {
            bagRestExport(basePath, nameWithouArchive, rootFolderPath);
        }
    }
    
    /**
     *  Creates the temporary folder "archive" in the original archive folder because the archive contains more than one subfolder
     *
     * @param basePath - path to archive folder ex: /home/gabriela/Plocha/26.2.2020/archive_xxxxx
     * @param rootFolderPath - path to main folder which contains archive folders ex: /home/gabriela/Plocha/26.2.2020/
     */
    private static void makeArchive(String basePath, String rootFolderPath) throws IOException, ParserConfigurationException, SAXException, FileNotFoundException, Exception {
        File rootDir = new File(basePath);
        File [] files = rootDir.listFiles();
        for (File file : files) {
            if (file.isDirectory() && !file.getName().contains("archive")) {
                String nameWithouArchive = file.getName();
                
                // CREATING DIRECTORY /archive_d7a85ed3-5131-4298-a0fa-b061a6b20d46
                String archivePath = basePath + "/" + "archive" + "/" + "archive_" + nameWithouArchive + "/" + nameWithouArchive; 
                File archiveDir = new File(archivePath);
                archiveDir.mkdir();
                File srcDir = new File(file.getPath());
                FileUtils.copyDirectory(srcDir, archiveDir);
            }
        }
        
        // BAGIT FOLDER
        String rootArchivePath = basePath + "/" + "archive";
        bagIt(rootArchivePath);
        
        // DELETE ALL NEW FOLDER ARCHIVE_XXX
        File rootArchiveDir = new File(rootArchivePath);
        files = rootArchiveDir.listFiles();
        for (File file : files) {
            if (file.isDirectory() && file.getName().contains("archive")) {
                utils.Utils.deleteFolder(file.getAbsoluteFile());
            }
        }
        
        // CREATING BAGARCHIVE FOLDER IN THE ROOT
        String bagarchivePath =  rootFolderPath + "/" + "bagarchive";
        File bagarchiveDir = new File(bagarchivePath);
        bagarchiveDir.mkdir();
        
        // COPY ALL FILES IN ARCHIVE FOLDER TO BAGARCHIVE FOLDER IN THE ROOT
        FileUtils.copyDirectory(rootArchiveDir, bagarchiveDir);
        
        // DELETE FOLDER ARCHIV
        utils.Utils.deleteFolder(rootArchiveDir);
    }
    
    /**
     *  Bags the folders in the archive
     *
     * @param basePath - path to the folder ex: /home/gabriela/Plocha/26.2.2020/bagit_xxxxx/archive_xxxxx
     * @param nameWithouArchive - archive_xxxxx -> xxxxx
     * @param rootFolderPath - path to the main folder which contains the archive folders ex: /home/gabriela/Plocha/26.2.2020/
     */
    private static void bagRestArchive(String basePath, String nameWithouArchive, String rootFolderPath) throws NoSuchAlgorithmException, IOException, ParserConfigurationException, SAXException, Exception {
        // CREATING MANIFEST-MD5.TXT
        String dataFolder = basePath + "/" + "data";
        File baseDir = new File(dataFolder);
        File [] files = baseDir.listFiles();
        
        int size = 0;
        int amount = 0;
                  
        String dataFolderWithChildFolder = "data";
        String metsPath = basePath + "/" + dataFolderWithChildFolder + "/" + "mets.xml";
        String nameOfNDKSubfolder = getNameOfSubfolder(basePath + "/" + dataFolderWithChildFolder + "/" + "NDK" + "/");
        String ndkPath = basePath + "/" + dataFolderWithChildFolder + "/" + "NDK" + "/" + nameOfNDKSubfolder + "/" + "mets_" + nameOfNDKSubfolder + ".xml";
                
        String nameOriginalTif = getNameOfFileOfCertainExtension(basePath + "/" + dataFolderWithChildFolder, "md5");
        String originalTifPath = basePath + "/" + dataFolderWithChildFolder + "/" + nameOriginalTif;
        createManifest(basePath, metsPath, ndkPath, nameOfNDKSubfolder, dataFolderWithChildFolder, nameWithouArchive);
        
        String originalTifFolderPath = utils.Utils.getPathWithoutExtension(originalTifPath);
                
        // GETTING PAYLOAD AND AMOUNT
        List<Integer> payloadAmount = new ArrayList<Integer>();
        payloadAmount = getPayloadAndAmount(metsPath, ndkPath, originalTifFolderPath, basePath + "/" + dataFolderWithChildFolder, "", basePath + "/");
        size += payloadAmount.get(0);
        amount += payloadAmount.get(1);
        
        // CREATING BAG-INFO.TXT
        createBagInfo(basePath, size, amount);
        
        // CREATING BAGIT.TXT
        createBagIt(basePath);
                
        // CREATING TAGMANIFETS-MD5.TXT
        createTagmanifest(basePath);
        
        String bagitDir = rootFolderPath + "/" + "bagit_" + nameWithouArchive;
        
        // CREATING ZIP
        String zipname = "archive_" + nameWithouArchive;
        zipIt(bagitDir + "/" + zipname, rootFolderPath + "/" + zipname + ".zip");

        // CREATING CHECKSUM FILE FOR ZIP
        createChecksum(rootFolderPath, zipname);
        
        // DELETING FOLDER BAGIT_
        File bagitFolder = new File(bagitDir);
        if (bagitFolder.exists()) {
            utils.Utils.deleteFolder(bagitFolder);
        }
    }
    
    /**
     *  Bags the folders in the export
     *
     * @param basePath - path to the folder ex: /home/gabriela/Plocha/26.2.2020/bagit_xxxxx/archive_xxxxx
     * @param nameWithouArchive - archive_xxxxx -> xxxxx
     * @param rootFolderPath - path to the main folder which contains the archive folders ex: /home/gabriela/Plocha/26.2.2020/
     */
    private static void bagRestExport(String basePath, String nameOfExportFolder, String rootFolderPath) throws NoSuchAlgorithmException, IOException, ParserConfigurationException, SAXException, Exception {
        String dataFolder = basePath + "/" + "data";
        File baseDir = new File(dataFolder);
        File [] files = baseDir.listFiles();
        
        int size = 0;
        int amount = 0;
        
        // CREATING MANIFEST-MD5.TXT
        createManifest(basePath, dataFolder, nameOfExportFolder);
        
        // GETTING PAYLOAD AND AMOUNT
        List<Integer> payloadAmount = new ArrayList<Integer>();
        payloadAmount = getPayloadAndAmountForFiles(dataFolder);
        size += payloadAmount.get(0);
        amount += payloadAmount.get(1);
        
        // CREATING BAG-INFO.TXT
        createBagInfo(basePath, size, amount);
        
        // CREATING BAGIT.TXT
        createBagIt(basePath);
                
        // CREATING TAGMANIFETS-MD5.TXT
        createTagmanifest(basePath);
        
        String bagitDir = rootFolderPath + "/" + "bagit_" + nameOfExportFolder;
         
        // CREATING ZIP
        String zipname = nameOfExportFolder;
        zipIt(bagitDir, rootFolderPath + "/" + zipname + ".zip");
        
        // CREATING CHECKSUM FILE FOR ZIP
        createChecksum(rootFolderPath, zipname);
        
        // DELETING FOLDER BAGIT_
        File bagitFolder = new File(bagitDir);
        if (bagitFolder.exists()) {
            utils.Utils.deleteFolder(bagitFolder);
        }
    }
    
    /**
     *  Creates manifest-md5.txt -  md5 and the path to the file
     *
     * @param basePath - path to the folder ex: /home/gabriela/Plocha/26.2.2020/bagit_xxxxx/archive_xxxxx
     * @param metsPath - path to the mets file ex: path to temporary folder ex: /home/gabriela/Plocha/26.2.2020/bagit_xxxxx/archive_xxxxx/data/mets.xml
     * @param ndkPath - path to the NDK mets ex: /home/gabriela/Plocha/26.2.2020/bagit_xxxxx/archive_xxxxx/data/NDK/aba007-zzzzzz/mets-aba007-zzzzzz.xml
     * @param nameOfNDKSubfolder - name of the NDK subfolder ex: aba007-zzzzzz
     * @param dataFolderWithChildFolder - path to the data folder ex: data
     * @param nameWithouArchive - archive_xxxxx -> xxxxx
     */
    private static void createManifest(String basePath, String metsPath, String ndkPath, String nameOfNDKSubfolder, String dataFolderWithChildFolder, String nameWithouArchive) throws ParserConfigurationException, SAXException, IOException, Exception {        
        String manifestPath = basePath + "/" + "manifest-md5.txt";

        if (!Files.exists(Paths.get(metsPath))) {
            throw new Exception("Soubor " + metsPath + " neexistuje");
        }
        getChecksumsFromXML(metsPath, manifestPath, dataFolderWithChildFolder);
        
        if (!Files.exists(Paths.get(ndkPath))) {
            throw new Exception("Soubor " + ndkPath + " neexistuje");
        }
        getChecksumsFromXML(ndkPath, manifestPath, dataFolderWithChildFolder + "/" + "NDK" + "/" + nameOfNDKSubfolder);
        
        String nameOriginal = getNameOfFileOfCertainExtension(basePath + "/" + dataFolderWithChildFolder, "md5");
        String originalPath = basePath + "/" + dataFolderWithChildFolder + "/" + nameOriginal;
        if (nameOriginal.equals("")) {
            throw new Exception("Soubor Original_xxx.md5 neexistuje");
        }
        getChecksumsFromText(originalPath, manifestPath, dataFolderWithChildFolder);
        getChecksumsForFiles(basePath + "/" + dataFolderWithChildFolder, nameWithouArchive, manifestPath, basePath + "/");
        
        // for ICC_profile
        File directory = new File(basePath + "/" + dataFolderWithChildFolder + "/" + "ICC_profil");
        if (directory.exists()) {
            getChecksumsForFiles(basePath + "/" + dataFolderWithChildFolder + "/" + "ICC_profil", nameWithouArchive, manifestPath, basePath + "/");
        }
    }
    
    /**
     *  Creates manifest-md5.txt -  md5 and the path to the file
     *
     * @param basePath - path to the folder ex: /home/gabriela/Plocha/26.2.2020/bagit_xxxxx/archive_xxxxx
     * @param dataFolder - path to the data folder ex: path to temporary folder ex: /home/gabriela/Plocha/26.2.2020/bagit_xxxxx/archive_xxxxx/data/
     * @param nameWithouArchive - archive_xxxxx -> xxxxx
     */
    private static void createManifest(String basePath, String dataFolder, String nameWithouArchive) throws IOException, Exception {
        String manifestPath = basePath + "/" + "manifest-md5.txt";
        getChecksumsForFiles(dataFolder, nameWithouArchive, manifestPath, basePath + "/");
    }
    
    /**
     *  Creates bag-info.txt
     *
     * @param basePath - path to the main folder of the archive ex: /home/gabriela/Plocha/26.2.2020/bagit_d7a85ed3-5131-4298-a0fa-b061a6b20d46
     * @param size - size of the files
     * @param amount - amount of the files
     */
    private static void createBagInfo(String basePath, int size, int amount) throws ParserConfigurationException, SAXException, IOException {
        String bagInfo = basePath + "/" + "bag-info.txt";
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String currentDate = dateFormat.format(date);
        
        OutputStream os = new FileOutputStream(bagInfo);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"))) {
            writer.println("Payload-Oxum: " + size + "." + amount);
            writer.println("Bagging-Date: " + currentDate);
            writer.close();
        }
    }
    
    
    /**
     *  Creates bagit.txt
     *
     * @param basePath - path to the main folder of the archive ex: /home/gabriela/Plocha/26.2.2020/bagit_d7a85ed3-5131-4298-a0fa-b061a6b20d46
     */
    private static void createBagIt(String basePath) throws IOException {
        String bagIt = basePath + "/" + "bagit.txt";
        OutputStream os = new FileOutputStream(bagIt);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"))) {
            writer.println("BagIt-Version: 1.0");
            writer.println("Tag-File-Character-Encoding: UTF-8");
            writer.close();
        }
    }
    
    /**
     *  Creates tagmanifest-md5.txt
     *
     * @param basePath - path to the main folder of the archive ex: /home/gabriela/Plocha/26.2.2020/bagit_d7a85ed3-5131-4298-a0fa-b061a6b20d46
     */
    private static void createTagmanifest(String basePath) throws IOException, Exception {
        String tagmanifest = basePath + "/" + "tagmanifest-md5.txt";
        OutputStream os = new FileOutputStream(tagmanifest);
        File folder = new File(basePath);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"))) {
            File[] files = folder.listFiles();
            for(File file : files) {
                if (file.isFile() && !file.getName().equals("tagmanifest-md5.txt")) {
                    String checksum = utils.Utils.getMD5(file.getPath());
                    String name = file.getName();
                    writer.println(checksum + "  " + name);
                }
            }
            writer.close();
        }
    }
    
    /**
     *  Creates name_of_zip.checksum
     *
     * @param rootFolderPath - path of the selected folder - root ex: /home/gabriela/Plocha/26.2.2020/
     * @param zipname - name of the zip file
     */
    private static void createChecksum(String rootFolderPath, String zipname) throws IOException, Exception {
        String checksumZip = rootFolderPath + "/" + zipname + ".sums";
        OutputStream os = new FileOutputStream(checksumZip);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {;
            String checksum = utils.Utils.getMD5(rootFolderPath + "/" + zipname + ".zip");
            writer.println("MD5 " + checksum);
            writer.close();
        }
    }
   
    /**
     *  Returns the name of the file with the certain extension
     *
     * @param path - path of the folder which contains the files ex: /home/  files: file.txt example.md5
     * @param extension - name of the extension ex: txt
     * @return String name of the file ex: file.txt
     */
    private static String getNameOfFileOfCertainExtension(String path, String extension) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile() && utils.Utils.getExtension(file.getAbsolutePath()).equals(extension)) {
                return file.getName();
            } 
        }
        return "";
    }
    
    
    /**
     *  Gets the checksums from xml
     *
     * @param path - path of the xml file which has the checksums and the paths and we want to get the checksums and the paths from it
     * @param manifest - path to the manifest file for saving the checksums and the paths of the files
     * @param basicHome - path which we want to start from ex: data/d7a85ed3-5131-4298-a0fa-b061a6b20d46
     */
    private static void getChecksumsFromXML(String path, String manifest, String basicHome) throws ParserConfigurationException, SAXException, IOException {
        Document document = XMLUtils.parseDocument(path);
        List<Element> files = XMLUtils.findElements(document.getDocumentElement(), "mets:file");
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(manifest, true), StandardCharsets.UTF_8)))) {
            for (Element file : files) {
                String checksum = file.getAttribute("CHECKSUM");
                Element flocat = XMLUtils.findElement(file, "mets:FLocat");
                if (flocat != null) {
                    String pathOfFile = flocat.getAttribute("xlink:href");
                    pathOfFile = getRelativePath(basicHome, pathOfFile);
                    writer.println(checksum + "  " + pathOfFile);
                }
            }
            writer.close();
        }
    }
    
    /**
     *  Gets the checksums from txt
     *
     * @param path - path of the txt file which has the checksums and the paths and we want to get the checksums and the paths from it
     * @param manifest - path to the manifest file for the saving checksums and the paths of the files
     * @param basicHome - path which we want to start from ex: data/d7a85ed3-5131-4298-a0fa-b061a6b20d46
     */
    private static void getChecksumsFromText(String path, String manifest, String basicHome) throws FileNotFoundException, IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(manifest, true), StandardCharsets.UTF_8)))) {
                while ((line = br.readLine()) != null) {
                    List<String> checksumpath = getChecksumAndPath(line);
                    String checksum = checksumpath.get(0);
                    String pathToFile = checksumpath.get(1);
                    pathToFile = pathToFile.replace('\\', '/');
                    pathToFile = getRelativePath(basicHome, pathToFile);
                    line = checksum + "  " + pathToFile;
                    writer.println(line);
                }
                writer.close();
            }
        }
    }
    
    /**
     *  Gets the checksums for the files in /data folder, /data/name_of_archive_without_prefix, /data/NDK/ and /data/NDK/aba007-xxxxxx
     *
     * @param path - path of the folder which we want to get the checksums for the files
     * @param folderName - name of the folder
     * @param manifest - path to the manifest file for the saving checksums and the paths of files
     * @param basePath - absolute path
     */
    private static void getChecksumsForFiles(String path, String folderName, String manifest, String basePath) throws IOException, Exception {
        File folder = new File(path);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
               String pathFromFolder = getPathFromFolder(basePath, file.getPath());
               String checksum = utils.Utils.getMD5(file.getPath());
               writeChecksumAndPahToFile(manifest, checksum, pathFromFolder);
            }
            
            // d7a85ed3-5131-4298-a0fa-b061a6b20d46/
            if (file.isDirectory() && file.getName().equals(folderName)) {
                getChecksumsForFiles(file.getAbsolutePath(), folderName, manifest, basePath);
            }
            // d7a85ed3-5131-4298-a0fa-b061a6b20d46/NDK/
            if (file.isDirectory() && file.getName().equals("NDK")) {
                getChecksumsForFiles(file.getAbsolutePath(), "NDK", manifest, basePath);
            }
            // d7a85ed3-5131-4298-a0fa-b061a6b20d46/NDK/aba007-00008r/
            if (file.isDirectory() && folderName.equals("NDK")) {
                getChecksumsForFiles(file.getAbsolutePath(), "", manifest, basePath);
            }
        }
    }
    
    
    /**
     * Zips
     * 
     * @param filePath - file/folder to be zipped
     * @param fileName - name of the zipped file
     * @throws IOException
     */
    private static void zipIt(String filePath, String fileName) throws IOException, Exception {
        ZIPUtils.zipIt(new File(filePath), new File(fileName));
    }
    
    /**
     * Replaces '*' in the path with '/' or delete '.' from the path
     * 
     * @param basicHome - path we want to start from
     * @param path - path of the file
     * @return String path which start with basicHome
     */
    private static String getRelativePath(String basicHome, String path) {
        char [] arr = path.toCharArray();
        
        // we want path with / in manifest-md5 
        basicHome = basicHome.replace("\\", "/");
        if (arr[0] == '*') {
            return basicHome + "/" + path.substring(1, path.length());
        }
        if (arr[0] == '.') {
            return basicHome + path.substring(1, path.length());
        }
        
        return basicHome + "/" + path;
    }

    /**
     * Returns the path without the prefix
     * 
     * @param basePath - base path where is the untar folder ex: /home/archive
     * @param filePath - absolute path of the file ex: /home/archive/time/example.txt
     * @return String path ex: time/example.txt
     */
    private static String getPathFromFolder(String basePath, String filePath) {
        String newPath = filePath.substring(basePath.length());
        return newPath;
    }
    
    /**
     * Returns the checksum and the path of file
     * 
     * @param line - line to be parse ex: 12133232434 
     * @return List<String> which contains of the checksum and the path
     */
    private static List<String> getChecksumAndPath(String line) {
        List<String> list = new ArrayList<String>();
        char [] arr = line.toCharArray();
        String checksum = "";
        String path = "";
        Boolean isItChecksum = true;
        for (int i = 0; i < line.length(); i++) {
            if (isItChecksum && arr[i] != ' ') {
                checksum += arr[i];
            }
            if (arr[i] == ' ') {
                isItChecksum = false;
            }
            if (!isItChecksum && arr[i] != ' ') {
                path += arr[i];
            }
        }
        list.add(checksum);
        list.add(path);
        return list;
    }
    
    /**
     * Writes the checksum and the path to the file
     * 
     * @param manifest - path to the manifest file for saving the checksums and the paths of the files
     * @param checksum - checksum of the file
     * @param pathOfFile - path to the file
     */
    private static void writeChecksumAndPahToFile(String manifest, String checksum, String pathOfFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(manifest, true), StandardCharsets.UTF_8)))) {
            pathOfFile = pathOfFile.replace("\\","/");
            writer.println(checksum + "  " + pathOfFile);
            writer.close();
        }
    }
    
    /**
     *  Returns name of the subfolder
     *
     * @param pathToFolder - path to the folder
     * @return String name of /subfolder
     */
    private static String getNameOfSubfolder(String pathToFolder) {
        String fileName = "";
        File folder = new File(pathToFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                fileName = file.getName();
                return fileName;
            }
        }
        return fileName;
    }
    
    /**
     *  Returns the size/payload and the amount of the files from xml
     *
     * @param path - path of the xml file which has sizes and we want to get the size/payload and the amount from it
     * @return List<Integer> the sum of the payloads/sizes and the amount in the xml file
     */
    private static List<Integer> getPayloadAndAmountFromXML(String path) throws ParserConfigurationException, SAXException, IOException {
        List<Integer> payloadAmount = new ArrayList<Integer>();
        Document document = XMLUtils.parseDocument(path);
        List<Element> files = XMLUtils.findElements(document.getDocumentElement(), "mets:file");
        int size = 0;
        int amount = 0;
        for (Element file : files) {
            size += Integer.parseInt(file.getAttribute("SIZE"));
            amount++;
        }
        payloadAmount.add(size);
        payloadAmount.add(amount);
        return payloadAmount;
    }
    
    /**
     *  Returns the size/payload and the amount of the files from the folder
     *
     * @param path - path of the folder which contains the files and we want to get the size/payload and the amount from it
     * @return List<Integer> the sum of the payloads/sizes and the amount in the folder
     */
    private static List<Integer> getPayloadAndAmountForFiles(String path) throws ParserConfigurationException, SAXException, IOException {
        List<Integer> payloadAmount = new ArrayList<Integer>();
        File folder = new File(path);
        int size = 0;
        int amount = 0;
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                size += file.length();
                amount++;
            }
        }
        payloadAmount.add(size);
        payloadAmount.add(amount);
        return payloadAmount;
    }
    
    /**
     *  Returns the size/payload and the amount of the files in /data folder, /data/NDK/ and /data/NDK/aba007-xxxxxx
     *
     * @param path - path of the folder which we want to get the checksums for the files
     * @param folderName - name of the folder
     * @param manifest - path to the manifest file for saving the checksums and the paths of the files
     * @param basePath - absolute path
     * @return List<Integer> the sum of the payloads/sizes and the amount of the files
     */
    private static List<Integer> getPayloadsAndAmountOfFiles(String path, String folderName, String basePath) throws IOException {
        List<Integer> payloadAmount = new ArrayList<Integer>();
        File folder = new File(path);
        File[] files = folder.listFiles();
        int size = 0;
        int amount = 0;
        for (File file : files) {
            if (file.isFile()) {
               size += file.length();
               amount++;
            }
            
            // data/NDK/
            if (file.isDirectory() && file.getName().equals("NDK")) {
                List<Integer> payloadAmountTmp = getPayloadsAndAmountOfFiles(file.getAbsolutePath(), "NDK", basePath);
                size += payloadAmountTmp.get(0);
                amount += payloadAmountTmp.get(1);
            }
            // data/NDK/aba007-00008r/
            if (file.isDirectory() && folderName.equals("NDK")) {
                List<Integer> payloadAmountTmp = getPayloadsAndAmountOfFiles(file.getAbsolutePath(), "", basePath);
                size += payloadAmountTmp.get(0);
                amount += payloadAmountTmp.get(1);
            }
        }
        payloadAmount.add(size);
        payloadAmount.add(amount);
        return payloadAmount;
    }
    
    /**
     *  Returns the sums of the size/payload and the amount of the files
     *
     * @param metsPath - path to mets.xml
     * @param ndkPath - path to NDK/aba007-xxxxxx/mets_aba007-xxxxxx.xml
     * @param originalTifFolderPath - path to the folder ex: /Original_Tif_LZs
     * @param path - path to the folder data ex: /home/gabriela/Plocha/26.2.2020/bagit_d7a85ed3-5131-4298-a0fa-b061a6b20d46/data
     * @param folderName - name of the folder in the folder /data
     * @param basePath - root folder ex: /home/gabriela/Plocha/26.2.2020/bagit_d7a85ed3-5131-4298-a0fa-b061a6b20d46/
     */
    private static List<Integer> getPayloadAndAmount(String metsPath, String ndkPath, String originalTifFolderPath, String path, String folderName, String basePath) throws ParserConfigurationException, SAXException, IOException {
        List<Integer> payloadAmount = new ArrayList<Integer>();
        List<Integer> payloadAmountTmp = new ArrayList<Integer>();
        int size = 0;
        int amount = 0;
        
        payloadAmountTmp = getPayloadAndAmountFromXML(metsPath);
        size += payloadAmountTmp.get(0);
        amount += payloadAmountTmp.get(1);
        
        payloadAmountTmp = getPayloadAndAmountFromXML(ndkPath);
        size += payloadAmountTmp.get(0);
        amount += payloadAmountTmp.get(1);
        
        payloadAmountTmp = getPayloadAndAmountForFiles(originalTifFolderPath);
        size += payloadAmountTmp.get(0);
        amount += payloadAmountTmp.get(1);
        
        payloadAmountTmp = getPayloadsAndAmountOfFiles(path, folderName, basePath);
        size += payloadAmountTmp.get(0);
        amount += payloadAmountTmp.get(1);
        
        payloadAmount.add(size);
        payloadAmount.add(amount);
        return payloadAmount;
    }
}
