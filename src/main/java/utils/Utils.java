package utils;

import com.twmacinta.util.MD5;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
/**
 *
 * @author Gabriela Melingerová
 */
public class Utils {
    
    /**
     *  Returns true if the folder contains the folder named 'data'
     *
     * @param folder - folder which we want to inspect
     * @return Boolean true/false
     */
    public static Boolean containsFolderData(File folder) {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory() && file.getName().equals("data")) {
                return true;
            }
        }
        return false;         
    }
    
    /**
     *  Returns the extension of the file
     *
     * @param path - path of the file ex: /home/file.txt
     * @return String name of the extansion ex: txt
     */
    public static String getExtension(String path) {
        String extension = "";
        int i = path.lastIndexOf('.');
        if (i > 0) {
            extension = path.substring(i+1);
        }
        return extension;
    }
    
    /**
     *  Returns path to the file without the extension
     *
     * @param path - path of the file ex: /home/file.txt
     * @return String path to the file without the extension ex: /home/file
     */
    public static String getPathWithoutExtension(String path) {
        String pathWithoutExtension = "";
        
        int i = path.lastIndexOf('.');
        if (i > 0) {
            pathWithoutExtension = path.substring(0, i);
        }
        return pathWithoutExtension;
    }
    
    /**
     * Deletes the folder if exists
     * 
     * @param folder - folder which we want to erase
     */
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files != null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    
    /**
     * Untars
     * 
     * @param tarFile - tar file which will be untar
     * @param destFile - destination where will be the untar file
     * @throws IOException
     */
    public static void unTarFile(File tarFile, File destFile) throws IOException{
        FileInputStream fis = new FileInputStream(tarFile);
        TarArchiveInputStream tis = new TarArchiveInputStream(fis);
        TarArchiveEntry tarEntry = null;
        
        // tarIn is a TarArchiveInputStream
        while ((tarEntry = tis.getNextTarEntry()) != null) {
            File outputFile = new File(destFile + "/" + tarEntry.getName());
            
            if (tarEntry.isDirectory()) {
                if(!outputFile.exists()) {
                    outputFile.mkdirs();
                }
            }
            else {
                outputFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(outputFile); 
                IOUtils.copy(tis, fos);
                fos.close();
            }
        }
        tis.close();
    }
    
    /**
     * Returns the name without the prefix
     * 
     * @param name - name which we want to delete the prefix ex: archive_123123
     * @return String name withou the prefix ex: 123123
     */
    public static String getNameWithoutPrefix(String name) {
        char [] arr = name.toCharArray();
        Boolean isArchive = false;
        int from = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '_') {
                from = i;
                isArchive = true;
                break;
            }
        }
        
        String finalname = "";
        if (isArchive == true) {
            finalname = name.substring(from + 1, name.length());
        }
        else {
            finalname = name.substring(from, name.length());
        }
        return finalname;
    }
    
    /**
     * Returns the prefix
     * 
     * @param name - name which we want to get the prefix from ex: archive_123123
     * @param underscore - how many underscores we want in the result ex: raw_mix_ndkpage, underscore - 1 -> raw_mix, underscore - 0 -> raw
     * @return String the prefix without the name : archive
     */
    public static String getPrefix(String name, int underscore) {
        char [] arr = name.toCharArray();
        int to = 0;
        int underscoreCount = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '_') {
                to = i;
                underscoreCount++;
                // to get prefix raw_mix
                if (underscoreCount > underscore) {
                    break;
                }
            }
        }
        
        String prefix = "";
        prefix = name.substring(0, to);
        return prefix;
    }
    
    /**
     * Returns number in file name
     * 
     * @param file - name of the file which contains the numbers audit_ndkpage_0003_ef2effa8-269b-45b7-b81b-5fae92be4c7c.xml
     * @param underscore - how many underscores we want in the result ex: raw_mix_ndkpage, underscore - 1 -> raw_mix, underscore - 0 -> raw
     * @return Number from the name of the file ex: audit_ndkpage_0003_ef2effa8-269b-45b7-b81b-5fae92be4c7c.xml -> 3
     */
    public static int getNumber(String file, int underscore) {
        String secondName = utils.Utils.getPrefix(file, underscore + 1);
        //audit_ndkpage_0003_ef2effa8-269b-45b7-b81b-5fae92be4c7c.xml -> audit_ndkpage_0003
        String thirdName = utils.Utils.getPrefix(file, underscore + 3);
                    
        // audit_ndkpage_0003 -> 0003 -> 3
        String numberText = thirdName.replace(secondName + "_", "");
        int number = Integer.valueOf(numberText);
        
        return number;
    }
    
    /**
     * Returns uuid in the file name ex: audit_device_0001_60098dd7-21b9-417f-acc4-367cff61b127.xml -> 60098dd7-21b9-417f-acc4-367cff61b127
     * 
     * @param file - name of the file with the extension ex: audit_device_0001_60098dd7-21b9-417f-acc4-367cff61b127.xml
     * @param extension - extension of the file ex: xml
     * @param underscore - how many underscores we want in the result ex: raw_mix_ndkpage, underscore - 1 -> raw_mix, underscore - 0 -> raw
     * @return String - uuid ex: 60098dd7-21b9-417f-acc4-367cff61b12
     */
    public static String getUuidInFile(String file, String extension, int underscore) {
        String thirdName = utils.Utils.getPrefix(file, underscore + 3);
        String uuid = file.replace(thirdName + "_", "");
        uuid = uuid.replace("." + extension, "");
        
        return uuid;
    }
    
    /**
     * Returns the subname ex: audit_device_0001_60098dd7-21b9-417f-acc4-367cff61b127.xml -> device_0001
     * 
     * @param file - name of the file ex: audit_device_0001_60098dd7-21b9-417f-acc4-367cff61b127.xml
     * @param prefix - prefix of the file ex: audit
     * @param underscore - how many underscores we want in the result ex: raw_mix_ndkpage, underscore - 1 -> raw_mix, underscore - 0 -> raw
     * @return String - the subname ex: device_0001
     */
    public static String getSubname(String file, String prefix, int underscore) {
        String thirdName = utils.Utils.getPrefix(file, underscore + 3);
        String subname = thirdName.replace(prefix + "_", "");
        
        return subname;
    }
    
     /**
     * Returns the second name
     * 
     * @param name - name from which we want to get the second name ex: Original_Tif_LZW
     * @return String - the second name ex: Original_Tif_LZW -> Tif
     */
    public static String getSecondName(String name) {
        char [] arr = name.toCharArray();
        int from = 0;
        int to = arr.length;
        int underscoreCount = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '_') {
                if (underscoreCount == 0) {
                    from = i;
                }
                underscoreCount++;
                if (underscoreCount == 2) {
                    to = i;
                }
            }
        }
        
        String finalname = "";
        finalname = name.substring(from + 1, to);
        return finalname;
    }
    
    /**
     * Returns the name of the file/folder from the path
     * 
     * @param path - path to the file/folder
     * @return String - name of the file/folder ex: /home/gabriela/Plocha/26.2.2020/archive_xxxxx/xxxxx/RAW -> RAW
     *                                              /home/gabriela/Plocha/26.2.2020/archive_xxxxx/xxxxx/mets.xml -> mets.xml
     */
    public static String getNameOfFileFromPath(String path) {
        int length = path.length();

        for (int i = length - 1; i >= 0; i--) {
            if (path.charAt(i) == '/' || path.charAt(i) == '\\') {
                String file = path.substring(i + 1, length);
                return file;
            } 
        }
        return "";
    }
    
    /**
     * Returns the MD5 checksum
     * 
     * @param filePath - absolute path of the file ex: /home/archive/time/example.txt
     * @return String the checksum of the file
     */
    /*
    public static String getMD5(String filePath) throws IOException {
        try {
            FileInputStream inputStream = new FileInputStream(new File(filePath));
            MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            FileChannel channel = inputStream.getChannel();
            ByteBuffer buff = ByteBuffer.allocate(2048);
            while(channel.read(buff) != -1)
            {
                buff.flip();
                md.update(buff);
                buff.clear();
            }
            byte[] hashValue = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hashValue.length; ++i) {
                sb.append(Integer.toHexString((hashValue[i] & 0xFF) | 0x100).substring(1,3));
            }
            inputStream.close();
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
        }
        return null;
    } */
    
    
    /**
     * Returns the MD5 checksum
     * 
     * @param filePath - absolute path of the file ex: /home/archive/time/example.txt
     * @return String the checksum of the file
     */
    public static String getMD5(String filePath) throws Exception {
       String hash = MD5.asHex(MD5.getHash(new File(filePath)));
       return hash;
   }
    
    
}
