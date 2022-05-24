package checkArchive;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.util.*;
import java.awt.Color;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyleConstants;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import org.w3c.dom.Document;
import utils.XMLUtils;
import org.w3c.dom.Element;

/**
 *
 * @author Gabriela Melingerová
 */

public class CheckIt {
    
    private static String log = "";
    // audit_ndkpage_0001_55d6ace2-4449-4fd8-9a3b-593c828053a5.xml -> <0001, 55d6ace2-4449-4fd8-9a3b-593c828053a5>
    private static Map<Integer, String> pagesMap = new HashMap<>();
    
    // audit_ndkmonographtitle_0001_64aac7a1-d9ea-4db8-8e7b-532b0ad7b85e.xml -> <ndkmonographtitle_0001, 4aac7a1-d9ea-4db8-8e7b-532b0ad7b85e>
    private static Map<String, String> ndkMap = new HashMap<>();
    
    // file name, md5
    private static Map<String, String> originalMap = new HashMap<>();
    
    // file name, md5
    private static Map<String, String> md5Map = new HashMap<>();
    
    //ndkpage or page or old pages
    private static boolean ndkPages = false;
    private static boolean pages = false;
    private static boolean oldPages = false;
    private static boolean occFolder = false;
    
    // counter of the files in the folder alto, amdsec etc.
    private static int altoCounter, amdsecCounter, mastercopyCounter, txtCounter, usercopyCounter;
    
    private static javax.swing.JTextPane jTextPane1;
    private static StyledDocument doc;
    private static Style styleError;
    //private static Style styleErrorOriginal;
    private static Style styleInfo;
    private static Style styleWarning;
    private static Style styleNeutral;
    private static Style styleFile;
    private static Style styleFolder;
    private static Style styleCheck;
    //private static Style styleOriginalMD5;
    private static Style styleInfoFile;
    
    private static String nameOfFolderUnderArchive;
    
    // position of the last text in the document
    private static int position = 0;
    
    // counter of the files in md5_xxxxx.xml or info_xxxxx.xml
    private static int altoCnt, amdsecCnt, mastercopyCnt, txtCnt, usercopyCnt;
    
    // if false - the file is missing in folder
    private static boolean altoExists, amdsecExists, mastercopyExists, txtExists, usercopyExists;
    
    /**
     * Checks the archive folder or the tar folder
     *
     * @param pathToFolder - path to the main folder which contains archives/tars ex: /home/gabriela/Plocha/26.2.2020/
     * @param panel - panel to add the info about the checked folder
     */
    public static void checkIt(String pathToFolder, JTextPane panel) throws Exception {
        jTextPane1 = panel;
        doc = jTextPane1.getStyledDocument();
        jTextPane1.setText("");
        
        setStyles();

        File folder = new File(pathToFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile() && !file.isHidden()) {
                String extension = utils.Utils.getExtension(file.getAbsolutePath());
                if (extension.equals("tar")) {
                    checkTarFile(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());
                }
            }
            if (file.isDirectory() && !file.isHidden() && !utils.Utils.containsFolderData(file)) {
                checkArchiveFolder(file.getAbsolutePath());
            }
        }
    }
    
    /**
     * Checks the archive folder
     *
     * @param basePath - path to the archive folder ex: /home/gabriela/Plocha/26.2.2020/archive_xxxxx
     */
    private static void checkArchiveFolder(String basePath) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, Exception {        
        Path folder = Paths.get(basePath);
        String folderName = folder.getFileName().toString();
        String nameWithouArchive = utils.Utils.getNameWithoutPrefix(folderName);
        String srcPath = basePath;
        int positionUUID = 0;
        
        if (!folderName.contains("archive")) {
            return;
        }
        
        File archiveDir = new File(srcPath);
        File [] files = archiveDir.listFiles();
        boolean sameName = false;
        int amountOfDir = 0;
        
        // check proarc_export_status.log
        String logPath = archiveDir.getAbsolutePath() + "/" + "proarc_export_status.log"; 
        checkLog(archiveDir.getAbsolutePath(), logPath);
        
        positionUUID = doc.getLength();
        
        for (File file : files) {
            if (file.isDirectory()) {
                pagesMap.clear();
                ndkMap.clear();
                originalMap.clear();
                md5Map.clear();
                altoCounter = amdsecCounter = mastercopyCounter = txtCounter = usercopyCounter = 0;
                position = 0;
                ndkPages = pages = oldPages = occFolder = false;
                nameOfFolderUnderArchive = "";
                
                doc.insertString(doc.getLength(), "###########################################################################################################\n\n", styleCheck);
                doc.insertString(doc.getLength(), "Kontroluji složku: ", styleCheck);
                doc.insertString(doc.getLength(), file.getAbsolutePath(), styleFolder);
                doc.insertString(doc.getLength(), ".\n", styleNeutral);
                
                nameOfFolderUnderArchive = file.getName();
                amountOfDir++;
                
                String subfolderPath = file.getAbsolutePath();
                
                boolean textWasAdded = false;
                
                // AUDIT
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "AUDIT", "audit", "xml", 0);
                // mix of ndkpages and pages in AUDIT - WRONG
                String text = "";
                if (ndkPages && pages && !oldPages) {
                    text = "Složka obsahuje NDK strany i normalní strany.\n";
                    doc.insertString(position, text, styleError);
                }
                else {
                    // it contains only ndkpages
                    if (ndkPages) {
                        text = "Složka obsahuje jen NDK strany.\n";
                    }
                    // it contains only pages
                    if (pages) {
                        text = "Složka obsahuje jen strany.\n";
                    }
                    if (oldPages) {
                        text = "Jedná se o staré tisky.\n";
                        File directory = new File(subfolderPath + "/" + "ICC_profil");
                         if (directory.exists()) {
                             text += "Obsahuje složku ICC_profil.\n";
                             occFolder = true;
                         }
                         else {
                             text += "Nebsahuje složku ICC_profil.\n";
                         }
                    }
                }
                
                // workflow_information.xml
                File workflow = new File(subfolderPath + "/" + "workflow_information.xml");
                if (workflow.exists()) {
                    text += "Složka obsahuje soubor workflow_information.xml\n\n";
                    doc.insertString(position, text, styleCheck); 
                }
                else {
                    text += "Složka neobsahuje soubor workflow_information.xml\n\n";
                    doc.insertString(position, text, styleCheck);
                }
                
                if (textWasAdded == true) {
                    doc.insertString(position + text.length(), "************************ AUDIT ************************\n\n", styleInfoFile);
                }
                else {
                    doc.insertString(doc.getLength(), "************************ AUDIT ************************\n\n", styleInfoFile);
                    infoMainFolder("AUDIT");
                }
                
                // ICC_profile
                if (oldPages) {
                    position = doc.getLength();
                    if (occFolder) {
                        textWasAdded = checkFolder(subfolderPath + "/" + "ICC_profil", "", "", 0);
                        if (textWasAdded == true) {
                            doc.insertString(position, "************************ ICC_profil ************************\n\n", styleInfoFile); 
                        }
                        else {
                            doc.insertString(doc.getLength(), "************************ ICC_profil ************************\n\n", styleInfoFile); 
                            infoMainFolder("ICC_profil");
                        }
                    }
                }
                
                // DESCRIPTION
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "DESCRIPTION", "description", "xml", 0);
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ DESCRIPTION ************************\n\n", styleInfoFile); 
                }
                else {
                    doc.insertString(doc.getLength(), "************************ DESCRIPTION ************************\n\n", styleInfoFile); 
                    infoMainFolder("DESCRIPTION");
                }
                
                // FOXML
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "FOXML", "foxml", "xml", 0);
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ FOXML ************************\n\n", styleInfoFile); 
                }
                else {
                    doc.insertString(doc.getLength(), "************************ FOXML ************************\n\n", styleInfoFile);  
                    infoMainFolder("FOXML");
                }
                
                // FULL
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "FULL", "full", "jpg", 0);
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ FULL ************************\n\n", styleInfoFile); 
                }
                else {
                    doc.insertString(doc.getLength(), "************************ FULL ************************\n\n", styleInfoFile); 
                    infoMainFolder("FULL");
                }
                
                // PREVIEW
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "PREVIEW", "preview", "jpg", 0);
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ PREVIEW ************************\n\n", styleInfoFile); 
                }
                else {
                    doc.insertString(doc.getLength(), "************************ PREVIEW ************************\n\n", styleInfoFile);  
                    infoMainFolder("PREVIEW");
                }
                
                // RAW
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "RAW", "raw", "tif", 0);
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ RAW ************************\n\n", styleInfoFile); 
                }
                else {
                    doc.insertString(doc.getLength(), "************************ RAW ************************\n\n", styleInfoFile);
                    infoMainFolder("RAW");
                }
                
                // RAW_MIX
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "RAW_MIX", "raw_mix", "xml", 1);
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ RAW_MIX ************************\n\n", styleInfoFile); 
                }
                else {
                    doc.insertString(doc.getLength(), "************************ RAW_MIX ************************\n\n", styleInfoFile); 
                    infoMainFolder("RAW_MIX");
                }
                
                // RELS-EXT
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "RELS-EXT", "rels-ext", "xml", 0);
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ RELS-EXT ************************\n\n", styleInfoFile); 
                }
                else {
                    doc.insertString(doc.getLength(), "************************ RELS-EXT ************************\n\n", styleInfoFile); 
                    infoMainFolder("RELS-EXT");
                }
                
                // THUMBNAIL
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "THUMBNAIL", "thumbnail", "jpg", 0);
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ THUMBNAIL ************************\n\n", styleInfoFile); 
                }
                else {
                    doc.insertString(doc.getLength(), "************************ THUMBNAIL ************************\n\n", styleInfoFile); 
                    infoMainFolder("THUMBNAIL");
                }
                
                // Original_Tif_LZW/Original_Tif/Original_JPG/Original_JP2/Original_PDF
                position = doc.getLength();
                textWasAdded = checkOriginalFolder(subfolderPath + "/");
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ ORIGINAL ************************\n\n", styleInfoFile); 
                }
                
                // METS
                position = doc.getLength();
                textWasAdded = checkMets(subfolderPath + "/" + "mets.xml");
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ METS ************************\n\n", styleInfoFile); 
                }
                
                // NDK
                checkNDKFolder(subfolderPath + "/" + "NDK");
                
                if (nameWithouArchive.equals(nameOfFolderUnderArchive)) {
                    sameName = true;
                }
            }
        }
        // ERROR
        if (sameName == true && amountOfDir > 1) {
            doc.insertString(positionUUID, "UUID SLOŽKY:\n", styleError);
            doc.insertString(positionUUID = positionUUID + "UUID SLOŽKY:\n".length(), "- jedna podsložka má stejné uuid jako archivní ", styleNeutral);
            doc.insertString(positionUUID = positionUUID + "- jedna podsložka má stejné uuid jako archivní ".length(), nameWithouArchive, styleFile);
            doc.insertString(positionUUID = positionUUID + nameWithouArchive.length(), "\n\n", styleNeutral);
        }
        
        // ERROR
        if (sameName == false && amountOfDir == 1) {
            doc.insertString(positionUUID, "UUID SLOŽKY:\n", styleError);
            doc.insertString(positionUUID = positionUUID + "UUID SLOŽKY:\n".length(), "- podsložka ", styleNeutral);
            doc.insertString(positionUUID = positionUUID + "- podsložka ".length(), nameOfFolderUnderArchive, styleFile);        
            doc.insertString(positionUUID = positionUUID + nameOfFolderUnderArchive.length(), "\nnemá stejné uuid jako ", styleNeutral);
            doc.insertString(positionUUID = positionUUID + "\nnemá stejné uuid jako ".length() , folderName, styleFile);
            doc.insertString(positionUUID = positionUUID + folderName.length() , ".\n\n", styleNeutral);
        }
        
    }
    
    /**
     * Checks the tar file
     *
     * @param pathToFile - path to the tar file ex: /home/gabriela/Plocha/26.2.2020/archive_d7a85ed3-5131-4298-a0fa-b061a6b20d46.tar
     * @param rootFolderPath - path to the main folder which contains archives/tars ex: /home/gabriela/Plocha/26.2.2020/
     */
    private static void checkTarFile(String pathToFile, String rootFolderPath) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, Exception {
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
        
        // CHECK FOLDER
        checkArchiveFolder(basePath);

        // DELETE UNTARFOLDER IF IT HAS ONLY ONE FOLDER 
        File untarFolder = new File(basePath);
        if (untarFolder.exists()) {
            utils.Utils.deleteFolder(untarFolder);
        }
    }
    
    /**
     * Checks the folder
     *
     * @param folderPath - path to the folder
     * @param prefix - prefix of the files in the folder ex: thumbnail, raw_mix, foxml...
     * @param extension - extension of the files in the folder ex: jpg, xml...
     * @param underscore - number of the undescores in the prefix name  thumbnail -> 0, raw_mix -> 1
     * @return boolean - true if the text was added
     */
    private static boolean checkFolder(String folderPath, String prefix, String extension, int underscore) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, Exception {
        File directory = new File(folderPath);
        boolean auditDeviceFile = false;
        
        int fileCount = 0;
        boolean textWasAdded = false;
        
        String folderName = utils.Utils.getNameOfFileFromPath(folderPath);
        
        // ERROR
        if (!directory.exists()) {
            // for old prints it's not an error if the folders "alto" or "txt" are missing
            if ((prefix.equals("alto") || prefix.equals("txt")) && oldPages) {
                doc.insertString(doc.getLength(), folderName.toUpperCase() + ":", styleWarning);
            }
            else {
                doc.insertString(doc.getLength(), folderName.toUpperCase() + ":", styleError);
            }
            doc.insertString(doc.getLength(), "\n- chybí složka s názvem ", styleNeutral);
            doc.insertString(doc.getLength(), folderName, styleFile);
            doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
            doc.insertString(doc.getLength(), folderPath.replace("/" + prefix.toUpperCase(), ""), styleFolder);
            doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
            
            //System.err.println("NEEXISTUJE: složka " + prefix.toUpperCase() + " neexistuje");
            log += "Složka: " + prefix.toUpperCase() + " neexistuje.\n\n";
            textWasAdded = true;
            return textWasAdded;
        }
        
        File [] files = directory.listFiles();
       
        // ICC_profil is empty - ERROR
        if (occFolder && files.length == 0) {
            doc.insertString(doc.getLength(), "ICC_profil:\n", styleError);
            doc.insertString(doc.getLength(), "- složka je prázdná.\n\n", styleNeutral);
            textWasAdded = true;
        } 
        
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            
            fileCount++;
            
            // checking ICC_profil for oldprints
            if (occFolder && extension.equals("")) {
                String fullFileName = file.getName();
                String extensionFile = utils.Utils.getExtension(fullFileName);
                extensionFile = extensionFile.toLowerCase();
                
                if (extensionFile.matches("icc|tif|jpg|txt")) {
                    continue;
                }
                else {
                    //error
                    doc.insertString(doc.getLength(), "KONCOVKA:", styleError);
                    doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
                    doc.insertString(doc.getLength(), fullFileName, styleFile);
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folderPath, styleFolder);
                    doc.insertString(doc.getLength(), "\nnemá koncovku '", styleNeutral);
                    doc.insertString(doc.getLength(), "icc|tif|jpg|txt", styleFile);
                    doc.insertString(doc.getLength(), "', má: '", styleNeutral);
                    doc.insertString(doc.getLength(), extensionFile, styleFile);
                    doc.insertString(doc.getLength(), "'.\n\n", styleNeutral);
                    textWasAdded = true;
                    continue;
                }
                
            }
            
            String fullFileName = file.getName();
            String prefixFile = utils.Utils.getPrefix(fullFileName, underscore);
            String extensionFile = utils.Utils.getExtension(fullFileName);
            
            if (prefix.equals("audit")) {
                String secondName = utils.Utils.getPrefix(fullFileName, 1);
                if (secondName.equals("audit_device")) {
                    auditDeviceFile = true;
                }
                if (secondName.equals("audit_ndkpage") || secondName.equals("audit_page") || secondName.equals("audit_oldprintpage")) {
                    if (secondName.contains("ndkpage")) {
                        ndkPages = true;
                    }
                    else {
                        if (secondName.contains("oldprintpage")) {
                            oldPages = true;
                        }
                        else {
                            pages = true;
                        }
                    }
                    int number = utils.Utils.getNumber(fullFileName, underscore);
                    String uuidFile = utils.Utils.getUuidInFile(fullFileName, extensionFile, underscore);
                    pagesMap.put(number, uuidFile);
                }
                else {
                    String subname = utils.Utils.getSubname(fullFileName, prefix, underscore);
                    String uuidFile = utils.Utils.getUuidInFile(fullFileName, extensionFile, underscore);
                    ndkMap.put(subname, uuidFile);
                }
            }
            
            if (prefix.matches("alto|amd_mets|mc|txt|uc")) {
                String md5 = utils.Utils.getMD5(file.getAbsolutePath());
                md5Map.put(fullFileName, md5);
            }
            
            if (!prefix.equals("audit") && !prefix.equals("description") && fullFileName.contains("page")) {
                int number = utils.Utils.getNumber(fullFileName, underscore);
                String uuidFile = utils.Utils.getUuidInFile(fullFileName, extensionFile, underscore);
                String page = pagesMap.get(number);
                
                // ERROR
                if (page == null || !uuidFile.equals(page)) {
                    doc.insertString(doc.getLength(), "NDKPAGE|PAGE:", styleError);
                    doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
                    doc.insertString(doc.getLength(), fullFileName, styleFile);
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folderPath, styleFolder);
                    doc.insertString(doc.getLength(), "\nse nenachází v AUDIT složce.\n\n", styleNeutral);
                    textWasAdded = true;
                }        
            }
            
            if ((prefix.equals("foxml") || prefix.equals("rels-ext")) && !fullFileName.contains("page")) {
                String subname = utils.Utils.getSubname(fullFileName, prefix, underscore);
                String uuidFile = utils.Utils.getUuidInFile(fullFileName, extensionFile, underscore);
                
                String ndkFileUuid = ndkMap.get(subname);
                
                // ERROR
                if (ndkFileUuid == null || !uuidFile.equals(ndkFileUuid)) {
                    doc.insertString(doc.getLength(), "NDK SOUBORY|DEVICE:", styleError);
                    doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
                    doc.insertString(doc.getLength(), fullFileName, styleFile);
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folderPath, styleFolder);
                    doc.insertString(doc.getLength(), "\nse nenachází v AUDIT složce.\n\n", styleNeutral);
                    textWasAdded = true;
                } 
            }

            // ERROR
            if (!prefixFile.equals(prefix)) {
                doc.insertString(doc.getLength(), "PREFIX:", styleError);
                doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
                doc.insertString(doc.getLength(), fullFileName, styleFile);
                doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                doc.insertString(doc.getLength(), folderPath, styleFolder);
                doc.insertString(doc.getLength(), "\nnemá prefix '", styleNeutral);
                doc.insertString(doc.getLength(), prefix, styleFile);
                doc.insertString(doc.getLength(), "', má: '", styleNeutral);
                doc.insertString(doc.getLength(), prefixFile, styleFile);
                doc.insertString(doc.getLength(), "'.\n\n", styleNeutral);
                textWasAdded = true;
            }
            
            // ERROR
            if (!extensionFile.equals(extension)) {
                doc.insertString(doc.getLength(), "KONCOVKA:", styleError);
                doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
                doc.insertString(doc.getLength(), fullFileName, styleFile);
                doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                doc.insertString(doc.getLength(), folderPath, styleFolder);
                doc.insertString(doc.getLength(), "\nnemá koncovku '", styleNeutral);
                doc.insertString(doc.getLength(), extension, styleFile);
                doc.insertString(doc.getLength(), "', má: '", styleNeutral);
                doc.insertString(doc.getLength(), extensionFile, styleFile);
                doc.insertString(doc.getLength(), "'.\n\n", styleNeutral);
                textWasAdded = true;
            }
        }
        
        
        int pageCount = pagesMap.size();
        int ndkCount = ndkMap.size();
        
        // ERROR
        if (((prefix.equals("foxml") || prefix.equals("rels-ext")) && ((pageCount + ndkCount) != fileCount)) || (prefix.matches("full|preview|raw|raw_mix|thumbnail") && (pageCount != fileCount))) {
            doc.insertString(doc.getLength(), "SOUBORY:", styleError);
            doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
            doc.insertString(doc.getLength(), prefix.toUpperCase(), styleFile);
            doc.insertString(doc.getLength(), " má počet souborů: ", styleNeutral);
            doc.insertString(doc.getLength(), Integer.toString(fileCount), styleFolder);
            doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
            doc.insertString(doc.getLength(), "AUDIT", styleFile);
            doc.insertString(doc.getLength(), " má počet souborů ", styleNeutral);
            
            if (prefix.matches("full|preview|raw|raw_mix|thumbnail")) {
                doc.insertString(doc.getLength(), "(jen ndkpages|pages): ", styleNeutral);
                doc.insertString(doc.getLength(), Integer.toString(pageCount), styleFolder);
            }
            else {
                doc.insertString(doc.getLength(), ": ", styleNeutral);
                doc.insertString(doc.getLength(), Integer.toString(pageCount + ndkCount), styleFolder);
            }
            doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
            doc.insertString(doc.getLength(), folderPath.replace(prefix.toUpperCase(), ""), styleFolder);
            doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
            textWasAdded = true;
        }
        

        // ERROR
        if (prefix.equals("audit") && auditDeviceFile == false) {
            doc.insertString(doc.getLength(), "DEVICE:", styleError);
            doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
            doc.insertString(doc.getLength(), "audit_device", styleFile);
            doc.insertString(doc.getLength(), " se nenachází", styleNeutral);
            doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
            doc.insertString(doc.getLength(), folderPath, styleFolder);
            doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
            textWasAdded = true;
        }
        return textWasAdded;
    }
    
    /**
     * Checks the Original folder
     *
     * @param folderPath - path to the original folder:  "Original_Tif_LZW", "Original_Tif", "Original_JPG", "Original_JP2", "Original_PDF"
     * @return boolean - true if the text was added
     */
    private static boolean checkOriginalFolder(String folderPath) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, Exception {
        String[] originalName = {"Original_Tif_LZW", "Original_Tif", "Original_JPG", "Original_JP2", "Original_PDF"};
        Boolean originalExists = false;
        String rawPath = folderPath + "RAW";
        File rawFolder = new File(rawPath);
        int rawFileCounter = 0;
        int fileCounter = 0;
        
        if (rawFolder.exists()) {
            rawFileCounter = rawFolder.listFiles().length;
        }
        
        for (String name : originalName) {
            String pathOriginal = folderPath + name;
            File folder = new File(pathOriginal);
            
            if (folder.exists()) {
                originalExists = true;
                String extension = utils.Utils.getSecondName(name).toLowerCase();
                boolean infoExists = false;
                
                File [] files = folder.listFiles();
                for (File file : files) {
                    
                    if (file.isDirectory()) {
                        continue;
                    }
                    
                    String fullFileName = file.getName();
                    String extensionFile = utils.Utils.getExtension(fullFileName);
                    extensionFile = extensionFile.toLowerCase();

                    fileCounter++;
                    
                    if (fullFileName.equals("Info.txt")) {
                        infoExists = true;
                    }
                    
                    String md5 = utils.Utils.getMD5(file.getAbsolutePath());
                    
                    originalMap.put(fullFileName, md5);
                    
                    // ERROR
                    if (!extensionFile.equals(extension) && !fullFileName.equals("Info.txt")) {
                        doc.insertString(doc.getLength(), "KONCOVKA:", styleError);
                        doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
                        doc.insertString(doc.getLength(), fullFileName, styleFile);
                        doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                        doc.insertString(doc.getLength(), pathOriginal, styleFolder);
                        doc.insertString(doc.getLength(), "\nnemá koncovku '", styleNeutral);
                        doc.insertString(doc.getLength(), extension, styleFile);
                        doc.insertString(doc.getLength(), "', má: '", styleNeutral);
                        doc.insertString(doc.getLength(), extensionFile, styleFile);
                        doc.insertString(doc.getLength(), "'.\n\n", styleNeutral);
                    }
                }
                
                // Original_Tif
                if (name.equals("Original_Tif")) {
                    if (fileCounter == 2 && infoExists) {
                        // INFO
                        doc.insertString(doc.getLength(), "ORIGINAL:", styleInfo);
                    }
                    else {
                      doc.insertString(doc.getLength(), "ORIGINAL:", styleError);  
                    }
                    
                    doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
                    doc.insertString(doc.getLength(), name, styleFile);
                    doc.insertString(doc.getLength(), " má počet souborů: ", styleNeutral);
                    doc.insertString(doc.getLength(), Integer.toString(fileCounter), styleFolder);
                    
                    if (infoExists == false) {
                        doc.insertString(doc.getLength(), "\n- chybí soubor ", styleNeutral);
                        doc.insertString(doc.getLength(), "Info.txt", styleFile);
                    }
                    
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folderPath, styleFolder);
                    doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
                    
                    continue;
                }
                
                // INFO
                if (Math.abs(fileCounter - rawFileCounter) <= 10 ) {
                    doc.insertString(doc.getLength(), "ORIGINAL:", styleInfo);
                    doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
                    doc.insertString(doc.getLength(), name, styleFile);
                    doc.insertString(doc.getLength(), " má počet souborů: ", styleNeutral);
                    doc.insertString(doc.getLength(), Integer.toString(fileCounter), styleFolder);
                    doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
                    doc.insertString(doc.getLength(), "RAW", styleFile);
                    doc.insertString(doc.getLength(), " má počet souborů: ", styleNeutral);
                    doc.insertString(doc.getLength(), Integer.toString(rawFileCounter), styleFolder);
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folderPath, styleFolder);
                    doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
                }
                // WARNING
                else {
                    doc.insertString(doc.getLength(), "ORIGINAL:", styleWarning);
                    doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
                    doc.insertString(doc.getLength(), name, styleFile);
                    doc.insertString(doc.getLength(), " má počet souborů: ", styleNeutral);
                    doc.insertString(doc.getLength(), Integer.toString(fileCounter), styleFolder);
                    doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
                    doc.insertString(doc.getLength(), "RAW", styleFile);
                    doc.insertString(doc.getLength(), " má počet souborů: ", styleNeutral);
                    doc.insertString(doc.getLength(), Integer.toString(rawFileCounter), styleFolder);
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folderPath, styleFolder);
                    doc.insertString(doc.getLength(), ".\n\n", styleNeutral); 
                }
                
                checkMD5Original(pathOriginal + ".md5");
            } 
        }
        
        if (originalExists == false) {
            // ERROR
            doc.insertString(doc.getLength(), "ORIGINAL:", styleError);
            doc.insertString(doc.getLength(), "\n- chybí složka s názvem ", styleNeutral);
            doc.insertString(doc.getLength(), "Original_Tif_LZW x Original_Tif x Original_JPG x Original_JP2 x Original_PDF", styleFile);
            doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
            doc.insertString(doc.getLength(), folderPath, styleFolder);
            doc.insertString(doc.getLength(), "\nnekontroluji soubor md5 ", styleNeutral);
            doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
        }
        
        return true;   
    }
    
    /**
     * Checks the NDK folder
     *
     * @param folderPath - path to the NDK folder
     */
    private static void checkNDKFolder(String folderPath) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, Exception {
        File folder = new File(folderPath);
        boolean subfolderExists = false;
        File subfolder;
        
        // ERROR
        if (!folder.exists()) {
            doc.insertString(doc.getLength(), "NDK:", styleError);
            doc.insertString(doc.getLength(), "\n- chybí složka s názvem ", styleNeutral);
            doc.insertString(doc.getLength(), "NDK", styleFile);
            doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
            doc.insertString(doc.getLength(), folderPath.replace("/" + "NDK", ""), styleFolder);
            doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
            return;
        }
        
        File [] files = folder.listFiles();
        for (File file : files) {
            // ab007- or uuid for old prints
            if (file.isDirectory()) {
                String folderName = file.getName();
                String subfolderPath = file.getAbsolutePath();                
                if (!folderName.matches("^aba007-.*") && !folderName.equals(nameOfFolderUnderArchive)) {
                    if (oldPages) {
                        doc.insertString(doc.getLength(), "NDK - " + nameOfFolderUnderArchive + ":", styleError);
                        doc.insertString(doc.getLength(), "\n- chybí podsložka ", styleNeutral);
                        doc.insertString(doc.getLength(), nameOfFolderUnderArchive, styleFile);
                    }
                    else {
                        doc.insertString(doc.getLength(), "NDK - aba007:", styleError);
                        doc.insertString(doc.getLength(), "\n- chybí podsložka s prefixem ", styleNeutral);
                        doc.insertString(doc.getLength(), "aba007", styleFile);
                    }
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folderPath, styleFolder);
                    doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
                }
                else {
                    subfolderExists = true;
                }
                boolean textWasAdded = false;
                
                // alto
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "alto", "alto", "xml", 0);
                subfolder = new File(subfolderPath + "/" + "alto");
                if (subfolder.exists()) {
                   altoCounter = subfolder.listFiles().length; 
                }
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ alto ************************\n\n", styleInfoFile); 
                }
                
                // amdsec
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "amdsec", "amd_mets", "xml", 1);
                subfolder = new File(subfolderPath + "/" + "amdsec");
                if (subfolder.exists()) {
                   amdsecCounter = subfolder.listFiles().length; 
                }
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ amdsec ************************\n\n", styleInfoFile); 
                }
                
                // mastercopy
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "mastercopy", "mc", "jp2", 0);
                subfolder = new File(subfolderPath + "/" + "mastercopy");
                if (subfolder.exists()) {
                    mastercopyCounter = subfolder.listFiles().length; 
                }
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ mastercopy ************************\n\n", styleInfoFile); 
                }
                
                // txt
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "txt", "txt", "txt", 0);
                subfolder = new File(subfolderPath + "/" + "txt");
                if (subfolder.exists()) {
                    txtCounter = subfolder.listFiles().length;
                }
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ txt ************************\n\n", styleInfoFile); 
                }
                
                // usercopy
                position = doc.getLength();
                textWasAdded = checkFolder(subfolderPath + "/" + "usercopy", "uc", "jp2", 0);
                subfolder = new File(subfolderPath + "/" + "usercopy");
                if (subfolder.exists()) {
                    usercopyCounter = subfolder.listFiles().length;
                }
                if (textWasAdded == true) {
                   doc.insertString(position, "************************ usercopy ************************\n\n", styleInfoFile); 
                }
                
                if (subfolderExists == true) {
                    // info
                    position = doc.getLength();
                    String infoFile = "info_" + folderName + ".xml";
                    textWasAdded = checkInfo(subfolderPath + "/" + infoFile);
                    if (textWasAdded == true) {
                        doc.insertString(position, "************************ info_xxxxx.xml ************************\n\n", styleInfoFile);
                    }
                    
                    // md5
                    position = doc.getLength();
                    String md5File = "md5_" + folderName + ".md5";
                    textWasAdded = checkMD5(subfolderPath + "/" + md5File);
                    if (textWasAdded == true) {
                        doc.insertString(position, "************************ md5_xxxxx.md5 ************************\n\n", styleInfoFile);
                    }
                    
                    // mets
                    position = doc.getLength();
                    String metsFile = "mets_" + folderName + ".xml";
                    textWasAdded = checkMets(subfolderPath + "/" + metsFile);
                    if (textWasAdded == true) {
                        doc.insertString(position, "************************ mets_xxxxx.xml ************************\n\n", styleInfoFile);
                    }
                }
                
                
            }
        }
        
        position = doc.getLength();
        String logPath = folderPath + "/" + "proarc_export_status.log"; 
        boolean textWasAdded = checkLog(folderPath, logPath);
        if (textWasAdded == true) {
            doc.insertString(position, "************************ proarc_export_status.log ************************\n\n", styleInfoFile);
        }
    }
    
    /**
     * Checks the log - proarc_export_status.log
     *
     * @param archivePath - path to the archive
     * @param filePath - path to the log
     * @return boolean - true if the text was added
     */
    private static boolean checkLog(String archivePath, String filePath) throws IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, Exception {
        File file = new File(filePath);
        
        if (file.exists()) {
            Document document = XMLUtils.parseDocument(filePath);
            Element topElement = document.getDocumentElement();
            List<Element> status = utils.XMLUtils.findElements(topElement, "status");
            
            List<Element> pids = utils.XMLUtils.findElements(topElement, "inputPid");
            
            for (int i = 0; i < status.size(); i++) {
                Element stat = status.get(i);
                String statusTxt = stat.getTextContent();
                
                // ERROR
                if (!statusTxt.equals("OK")) {
                    Element pid = pids.get(i);
                    String pidTxt = pid.getTextContent();
                    
                    doc.insertString(doc.getLength(), "LOG ERROR EXPORT:", styleError);
                    doc.insertString(doc.getLength(), "\n- ", styleNeutral);
                    doc.insertString(doc.getLength(), pidTxt, styleFile);
                    doc.insertString(doc.getLength(), "\nv logu ", styleNeutral);
                    doc.insertString(doc.getLength(), "proarc_export_status.log", styleFile);
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), archivePath, styleFolder);
                    doc.insertString(doc.getLength(), "\nmá status ", styleNeutral);
                    doc.insertString(doc.getLength(), statusTxt, styleFile);
                    doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
                    return true;
                }
            }
        }
       
        // ERROR
        else {
            doc.insertString(doc.getLength(), "LOG:", styleError);
            doc.insertString(doc.getLength(), "\n- chybí log s názvem ", styleNeutral);
            doc.insertString(doc.getLength(), "proarc_export_status.log", styleFile);
            doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
            doc.insertString(doc.getLength(), archivePath, styleFolder);
            doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
            return true;
        }
        return false;
    }
    
    /**
     * Checks the md5 of the original file
     *
     * @param file - md5 file:  "Original_Tif_LZW.md5", "Original_Tif.md5", "Original_JPG.md5", "Original_JP2.md5", "Original_PDF.md5"
     */
    private static void checkMD5Original(String file) throws IOException, BadLocationException {
        String originalFile = utils.Utils.getNameOfFileFromPath(file);
        String folder = file.replace(".md5", "");
        boolean fileExists = true;
        
        File md5File = new File(file);
        // Original_Tif_LZW.md5/Original_Tif.md5/Original_JPG.md5/Original_JP2.md5/Original_PDF.md5
        if (!md5File.exists()) {
            infoMissingMainFile(originalFile.toUpperCase(), originalFile, file.replace(originalFile, ""));
            return;  
        }
        
        int lineCounter = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<String> checksumpath = getChecksumAndPath(line);
                String checksum = checksumpath.get(0);
                String pathToFile = checksumpath.get(1);
                pathToFile = pathToFile.replace('\\', '/');
                lineCounter++;
                
                String fileName = utils.Utils.getNameOfFileFromPath(pathToFile);
                
                String md5 = originalMap.get(fileName);
                if (md5 == null) {
                    doc.insertString(doc.getLength(), "ORIGINAL MD5 - NENALEZENÝ SOUBOR:", styleError);
                    doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
                    doc.insertString(doc.getLength(), fileName, styleFile);
                    doc.insertString(doc.getLength(), "\nchecksum ", styleNeutral);
                    doc.insertString(doc.getLength(), checksum, styleFile);
                    doc.insertString(doc.getLength(), "\nze souboru ", styleNeutral);
                    doc.insertString(doc.getLength(), originalFile, styleFolder);
                    doc.insertString(doc.getLength(), "\nnebyl nenalezen", styleFile);
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folder, styleFolder);
                    doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
                    fileExists = false;
                }
                if (md5 != null && !md5.equals(checksum)) {
                    doc.insertString(doc.getLength(), "ORIGINAL MD5 - ŠPATNÝ CHECKSUM:", styleError);
                    doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
                    doc.insertString(doc.getLength(), fileName, styleFile);
                    doc.insertString(doc.getLength(), "\nchecksum ", styleNeutral);
                    doc.insertString(doc.getLength(), checksum, styleFile);
                    doc.insertString(doc.getLength(), "\nze souboru ", styleNeutral);
                    doc.insertString(doc.getLength(), originalFile, styleFolder);
                    doc.insertString(doc.getLength(), "\nse neshoduje", styleFile);
                    doc.insertString(doc.getLength(), " s checksumem stejného souboru ", styleNeutral);
                    doc.insertString(doc.getLength(), md5, styleFile);
                    doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
                    doc.insertString(doc.getLength(), folder, styleFolder);
                    doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
                }
            }
        }
        
        if (lineCounter == originalMap.size()) {
            if (fileExists == false) {
                doc.insertString(doc.getLength(), "ORIGINAL MD5:", styleWarning); 
            }
            else {
                doc.insertString(doc.getLength(), "ORIGINAL MD5:", styleInfo);
            }
        }
        else {
            doc.insertString(doc.getLength(), "ORIGINAL MD5:", styleError);  
        }
                    
        doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
        doc.insertString(doc.getLength(), originalFile, styleFile);
        doc.insertString(doc.getLength(), " má v sobě počet souborů: ", styleNeutral);
        doc.insertString(doc.getLength(), Integer.toString(lineCounter), styleFolder);
        doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
        doc.insertString(doc.getLength(), originalFile.replace(".md5", ""), styleFile);;
        doc.insertString(doc.getLength(), " má počet souborů: ", styleNeutral);
        doc.insertString(doc.getLength(), Integer.toString(originalMap.size()), styleFolder);
        doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
        doc.insertString(doc.getLength(), file.replace(originalFile, ""), styleFolder);
        doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
    }
    
    /**
     * Checks the md5 file in the NDK subfolder
     *
     * @param file - md5 file ex: md5_aba007-0007i9.md5
     * @return boolean - true if text was added
     */
    private static boolean checkMD5(String file) throws IOException, BadLocationException, Exception {
        String md5File = utils.Utils.getNameOfFileFromPath(file);
        boolean metsInMD5 = false;
        altoCnt = amdsecCnt = mastercopyCnt = txtCnt =  usercopyCnt = 0;
        altoExists = amdsecExists= mastercopyExists = txtExists = usercopyExists = true;
        String ndkFolder = file.replace(md5File, "");
        
        File fileMD5 = new File(file);
        if (!fileMD5.exists()) {
            infoMissingMainFile("MD5", md5File, ndkFolder);
            return true;
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<String> checksumpath = getChecksumAndPath(line);
                String checksum = checksumpath.get(0);
                String pathToFile = checksumpath.get(1);
                pathToFile = pathToFile.replace('\\', '/');
                
                String fileName = utils.Utils.getNameOfFileFromPath(pathToFile);
                String md5 = md5Map.get(fileName);
                
                countingFiles(fileName, md5);
                
                // mets_*******.xml
                if (fileName.matches("^mets_.*")) {
                    metsInMD5 = true;
                    String metsPath = ndkFolder + fileName;
                    
                    File mets = new File(metsPath);
                    
                    // NEEXISTUJE
                    if (!mets.exists()) {
                        md5 = null;
                    }
                    else {
                       md5 = utils.Utils.getMD5(metsPath);
                    }
                }
                
                // /mastercopy/ -> mastercopy/
                String subfolder = pathToFile.replace(fileName, "");
                if (subfolder.charAt(0) == '/' || subfolder.charAt(0) == '\\') {
                    subfolder = subfolder.substring(1);
                }
                
                if (md5 == null) {
                    infoMD5(true, fileName, checksum, md5, md5File, ndkFolder + subfolder);
                }
                if (md5 != null && !md5.equals(checksum)) {
                    infoMD5(false, fileName, checksum, md5, md5File, ndkFolder + subfolder);
                }
            }
        }
        
        // mets_*******.xml is completely missing in the file md5_*******.md5
        if (metsInMD5 == false) {
            doc.insertString(doc.getLength(), "MD5 - CHYBÍ SOUBOR:", styleError);
            doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
            doc.insertString(doc.getLength(), "mets_*******.xml", styleFile);
            doc.insertString(doc.getLength(), "\nnenalezen v souboru ", styleNeutral);
            doc.insertString(doc.getLength(), md5File, styleFolder);
            doc.insertString(doc.getLength(), "\nmožná chybí i ve složce ", styleNeutral);
            doc.insertString(doc.getLength(), ndkFolder, styleFolder);
            doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
        
        }
        
        infoSubFolder(altoExists, "MD5", md5File, altoCounter, altoCnt, "alto", "alto", ndkFolder);
        infoSubFolder(amdsecExists, "MD5", md5File, amdsecCounter, amdsecCnt, "amd_mets", "admsec", ndkFolder);
        infoSubFolder(mastercopyExists, "MD5", md5File, mastercopyCounter, mastercopyCnt, "mc", "mastercopy", ndkFolder);
        infoSubFolder(txtExists, "MD5", md5File, txtCounter, txtCnt, "txt", "txt", ndkFolder);
        infoSubFolder(usercopyExists, "MD5", md5File, usercopyCounter, usercopyCnt, "uc", "usercopy", ndkFolder);
        
        return true;
    }
    
    /**
     * Counting the files in the folders
     *
     * @param fileName - name of the file ex: mc_aba007-0007i9_0002.jp2
     * @param md5 - md5 of the file ex: 78500096af234f19e485ba32d1f3d0f9
     */
    private static void countingFiles(String fileName, String md5) {
        if (fileName.contains("alto")) {
            altoCnt++;
            if (md5 == null) {
                altoExists = false;
            }
        }
                
        if (fileName.contains("amd_mets")) {
            amdsecCnt++;
            if (md5 == null) {
                amdsecExists = false;
            }
        }
                
        if (fileName.contains("mc")) {
            mastercopyCnt++;
            if (md5 == null) {
                mastercopyExists = false;
            }
        }
                
        if (fileName.contains("txt")) {
            txtCnt++;
            if (md5 == null) {
                txtExists= false;
            }
        }

        if (fileName.contains("uc")) {
            usercopyCnt++;
            if (md5 == null) {
                usercopyExists = false;
            }
        }
    }
    
    /**
     * Checks the info file in the NDK subfolder
     *
     * @param file - info file ex: info_aba007-0007i9.xml
     * @return boolean - true if the text was added
     */
    private static boolean checkInfo(String file) throws IOException, BadLocationException, Exception {
        String infoFile = utils.Utils.getNameOfFileFromPath(file);
        String folder = file.replace(infoFile, "");
        
        File info = new File(file);
        if (!info.exists()) {
            infoMissingMainFile("INFO", infoFile, folder);
            return true;
        }
        
        Document document = XMLUtils.parseDocument(file);
        Element topElement = document.getDocumentElement();
        List<Element> items = XMLUtils.findElements(topElement, "item");
        altoCnt = amdsecCnt = mastercopyCnt = txtCnt =  usercopyCnt = 0;
        altoExists = amdsecExists = mastercopyExists = txtExists = usercopyExists = true;
        
        for (Element item : items) {
            String pathToFile = item.getTextContent();
            String fileName = utils.Utils.getNameOfFileFromPath(pathToFile);
            
            // /mastercopy/ -> mastercopy/
            String subfolder = pathToFile.replace(fileName, "");
            if (subfolder.charAt(0) == '/' || subfolder.charAt(0) == '\\') {
                subfolder = subfolder.substring(1);
            }
            
            boolean contains = md5Map.containsKey(fileName);

            if (contains == false) {
                if (fileName.matches("^mets_.*|^md5_.*|^info_.*")) {
                    File otherFile = new File(folder + fileName);
                    if (!otherFile.exists()) {
                        infoMissingFile(fileName, infoFile, folder);
                    }
                }
                else {
                    countingFiles(fileName, null);
                    infoMissingFile(fileName, infoFile, folder + subfolder);
                }
            }
            else {
                countingFiles(fileName, "");
            }
        }

        infoSubFolder(altoExists, "INFO", infoFile, altoCounter, altoCnt, "alto", "alto", folder);
        infoSubFolder(amdsecExists, "INFO", infoFile, amdsecCounter, amdsecCnt, "amd_mets", "admsec", folder);
        infoSubFolder(mastercopyExists, "INFO", infoFile, mastercopyCounter, mastercopyCnt, "mc", "mastercopy", folder);
        infoSubFolder(txtExists, "INFO", infoFile, txtCounter, txtCnt, "txt", "txt", folder);
        infoSubFolder(usercopyExists, "INFO", infoFile, usercopyCounter, usercopyCnt, "uc", "usercopy", folder);

        // we also have information about amount of files in folders
        return true;
    }
    
    /**
     * Checks the mets file in the NDK subfolder
     *
     * @param file - mets file ex: mets_aba007-0007i9.xml
     * @return boolean - true if the text was added
     */
    private static boolean checkMets(String file) throws IOException, BadLocationException, Exception {
        File mets = new File(file);
        String metsFile =  utils.Utils.getNameOfFileFromPath(file);
        String folder = file.replace(metsFile, "");
        
        if (!mets.exists()) {
            infoMissingMainFile("METS", metsFile, folder);
        }
        else {
            infoMainFileExists("METS", metsFile, folder);
        }
        return true;
    }
    
    /**
     * 
     * Sets the styles for the text
     */
    private static void setStyles() {
        styleError = jTextPane1.addStyle("Error", null);
        //styleErrorOriginal = jTextPane1.addStyle("ErrorOriginal", null);
        styleNeutral = jTextPane1.addStyle("Neutral", null);
        styleFile = jTextPane1.addStyle("File", null);
        styleFolder = jTextPane1.addStyle("Folder", null);
        styleInfo = jTextPane1.addStyle("Info", null);
        styleWarning = jTextPane1.addStyle("Warning", null);
        styleCheck = jTextPane1.addStyle("Check", null);
        //styleOriginalMD5 = jTextPane1.addStyle("OriginalInfo", null);
        styleInfoFile = jTextPane1.addStyle("InfoFile", null);
        
        StyleConstants.setForeground(styleError, Color.decode("#f94144"));
        StyleConstants.setBold(styleError, true);
        
        //StyleConstants.setForeground(styleErrorOriginal, Color.decode("#f94144"));
        //StyleConstants.setBold(styleErrorOriginal, true);
       
        StyleConstants.setForeground(styleNeutral, Color.BLACK);
        
        StyleConstants.setForeground(styleFile, Color.BLACK);
        StyleConstants.setBold(styleFile, true);
        
        StyleConstants.setForeground(styleFolder, Color.BLACK);
        StyleConstants.setUnderline(styleFolder, true);
        
        StyleConstants.setForeground(styleInfo, Color.decode("#90be6d"));
        StyleConstants.setBold(styleInfo, true);
        
        StyleConstants.setForeground(styleWarning, Color.decode("#f8961e"));
        StyleConstants.setBold(styleWarning, true);
        
        //StyleConstants.setForeground(styleOriginalMD5, Color.decode("#f94144"));
        //StyleConstants.setBold(styleOriginalMD5, true);
        
        StyleConstants.setForeground(styleCheck, Color.decode("#277da1"));
        StyleConstants.setBold(styleCheck, true);
        
        StyleConstants.setForeground(styleInfoFile, Color.decode("#43aa8b"));
        StyleConstants.setBold(styleInfoFile, true);
    }
     
    /**
     * Returns the checksum and the path to the file
     * 
     * @param line - line to be parse ex: 12133232434 
     * @return List<String> which contains checksum and path
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
     * Inserts the text about the md5
     *
     * @param isMD5null - if MD5 is null or not
     * @param fileName - name of the file
     * @param checksum - checksum of the file from the md5 file
     * @param md5 - checksum of the file
     * @param md5File - md5 file
     * @param folder - folder of the file
     */
    private static void infoMD5(boolean isMD5null, String fileName, String checksum, String md5, String md5File, String folder) throws BadLocationException {
        
        if (isMD5null == true) {
            doc.insertString(doc.getLength(), "MD5 - NENALEZENÝ SOUBOR:", styleError);
        }
        else {
            doc.insertString(doc.getLength(), "MD5 - ŠPATNÝ CHECKSUM:", styleError);
        }
        doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
        doc.insertString(doc.getLength(), fileName, styleFile);
        doc.insertString(doc.getLength(), "\nchecksum ", styleNeutral);
        doc.insertString(doc.getLength(), checksum, styleFile);
        doc.insertString(doc.getLength(), "\nze souboru ", styleNeutral);
        doc.insertString(doc.getLength(), md5File, styleFolder);
        
        if (isMD5null == true) {
            doc.insertString(doc.getLength(), "\nnebyl nenalezen", styleFile);
        }
        else {
            doc.insertString(doc.getLength(), "\nse neshoduje", styleFile);
            doc.insertString(doc.getLength(), " s checksumem stejného souboru ", styleNeutral);
            doc.insertString(doc.getLength(), md5, styleFile);
        }
        doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
        doc.insertString(doc.getLength(), folder, styleFolder);
        doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
    }
    
    /**
     * Inserts the text about the subfolder in NDK
     *
     * @param fileExists - file exists in md5 or info but it's not in the folder
     * @param name - if it's about the md5 file or the info file ex: md5 file -> MD5, info file -> INFO
     * @param file - md5 file or info file
     * @param pageCounter - counter of the files in its folder
     * @param pageCnt - counter of the files in md5_xxxxx.xml or info_xxxxx.xml
     * @param prefix -prefix of the files ex: alto, amd_mets, mc, txt, uc
     * @param folder - name of subfolder in NDK ex: alto, amdsec, mastercopy, txt, usercopy
     * @param folderNDK - NDK folder
     */
    private static void infoSubFolder(boolean fileExists, String name, String file, int pageCounter, int pageCnt, String prefix, String folder, String folderNDK) throws BadLocationException {
        
        if (pageCnt == pageCounter) {
            // The amount of the files in the folder is the same as the amount of the files with certain prefix in md5|info file, but the file is missing fileExists = false (it wasn't find in the folder)
            if (fileExists == false) {
                doc.insertString(doc.getLength(), name + " - " + folder.toUpperCase() + ":", styleWarning);
            }
            else {
               doc.insertString(doc.getLength(), name + " - " + folder.toUpperCase() + ":", styleInfo); 
            }
        }
        else {
            doc.insertString(doc.getLength(), name + " - " + folder.toUpperCase() + ":", styleError);  
        }
        
        doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
        doc.insertString(doc.getLength(), file, styleFile);
        doc.insertString(doc.getLength(), " má v sobě počet souborů s prefixem ", styleNeutral);
        doc.insertString(doc.getLength(), prefix, styleFile);
        doc.insertString(doc.getLength(), ": ", styleNeutral);
        doc.insertString(doc.getLength(), Integer.toString(pageCnt), styleFolder);
        doc.insertString(doc.getLength(), "\n- složka ", styleNeutral);
        doc.insertString(doc.getLength(), folder, styleFile);;
        doc.insertString(doc.getLength(), " má počet souborů: ", styleNeutral);
        doc.insertString(doc.getLength(), Integer.toString(pageCounter), styleFolder);
        doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
        doc.insertString(doc.getLength(), folderNDK, styleFolder);
        doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
    }
    
    /**
     * Inserts the text about the missing file from the info file
     *
     * @param fileName - file name which is missing
     * @param infoFile - info file
     * @param folder - name of the folder where we coudn't find the file
     */
    private static void infoMissingFile(String fileName, String infoFile, String folder) throws BadLocationException {
        doc.insertString(doc.getLength(), "INFO - NENALEZENÝ SOUBOR:", styleError);
        doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
        doc.insertString(doc.getLength(), fileName, styleFile);
        doc.insertString(doc.getLength(), "\nze souboru ", styleNeutral);
        doc.insertString(doc.getLength(), infoFile, styleFolder);
        doc.insertString(doc.getLength(), "\nnebyl nenalezen", styleFile);
        doc.insertString(doc.getLength(), "\nve složce ", styleNeutral);
        doc.insertString(doc.getLength(), folder, styleFolder);
        doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
        
    }
    
    /**
     * Inserts the text about the missing main file
     *
     * @param name - name of the problem ex: MD5, METS, INFO
     * @param fileName - name of the file which is missing
     * @param folder - name of the folder where we coudn't find the file
     */
    private static void infoMissingMainFile(String name, String fileName, String folder) throws BadLocationException {
        doc.insertString(doc.getLength(), name + ":", styleError);
        doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
        doc.insertString(doc.getLength(), fileName, styleFile);
        doc.insertString(doc.getLength(), "\nnenalezen ve složce ", styleNeutral);
        doc.insertString(doc.getLength(), folder, styleFolder);
        doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
    }
    
    /**
     * Inserts the text about the existence of the main file
     *
     * @param name - name of the problem ex: MD5, METS, INFO
     * @param fileName - name of the file which exists
     * @param folder - name of the folder where we find the file
     */
    private static void infoMainFileExists(String name, String fileName, String folder) throws BadLocationException {
        doc.insertString(doc.getLength(), name + ":", styleInfo);
        doc.insertString(doc.getLength(), "\n- soubor ", styleNeutral);
        doc.insertString(doc.getLength(), fileName, styleFile);
        doc.insertString(doc.getLength(), "\nexistuje ve složce ", styleNeutral);
        doc.insertString(doc.getLength(), folder, styleFolder);
        doc.insertString(doc.getLength(), ".\n\n", styleNeutral);
    }
    
    /**
     * Inserts the text about that the main folder doesn't have issues
     *
     * @param name - name of the folder ex: AUDIT, 
     */
    private static void infoMainFolder(String name) throws BadLocationException {
        doc.insertString(doc.getLength(), name + ":", styleInfo);
        doc.insertString(doc.getLength(), "\n- složka byla zkontrolována.\n\n", styleNeutral);
    }
}