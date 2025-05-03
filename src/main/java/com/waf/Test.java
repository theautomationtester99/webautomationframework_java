// package com.waf;

// import java.nio.file.*;
// import javax.crypto.*;
// import javax.crypto.spec.SecretKeySpec;
// import java.util.Base64;

// public class Test {
//     private static final String DEFAULT_KEY = "VVxjC-BkcMff4vxqD0RhZcCsf2T9OVotNm-Y4vwAyhM=";

//     public String encryptFile(String filePath, String outputFile, String encryptionKey) throws Exception {
//         // Read the file content
//         String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));

//         // Encrypt the file content (returns Base64-encoded string)
//         String encryptedContent = encryptString(fileContent, encryptionKey);

//         // Save the Base64-encoded encrypted content to a file
//         Files.write(Paths.get(outputFile), encryptedContent.getBytes());
//         System.out.println("File encrypted successfully and saved in readable format!");

//         return encryptedContent; // Return readable encrypted content
//     }

//     public String encryptString(String inputString, String encryptionKey) throws Exception {
//         // Create a key specification from the encryption key
//         byte[] key = Base64.getUrlDecoder().decode(encryptionKey);
//         SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

//         // Create a cipher object and initialize it
//         Cipher cipher = Cipher.getInstance("AES");
//         cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

//         // Encrypt the input string and encode as Base64
//         byte[] encryptedBytes = cipher.doFinal(inputString.getBytes());
//         return Base64.getEncoder().encodeToString(encryptedBytes); // Return Base64-encoded string
//     }

//     public String decryptString(String encryptedString, String encryptionKey) throws Exception {
//         // Create a key specification from the encryption key
//         byte[] key = Base64.getUrlDecoder().decode(encryptionKey);
//         SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

//         // Create a cipher object and initialize it
//         Cipher cipher = Cipher.getInstance("AES");
//         cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

//         // Decode Base64 string to get encrypted bytes and decrypt them
//         byte[] encryptedBytes = Base64.getDecoder().decode(encryptedString);
//         byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

//         return new String(decryptedBytes); // Return decrypted string
//     }

//     public String decryptFile(String encryptedFilePath, String decryptionKey) throws Exception {
//         // Read the Base64-encoded encrypted content from the file
//         String encodedEncryptedContent = new String(Files.readAllBytes(Paths.get(encryptedFilePath)));

//         // Decrypt the content (input is Base64 string)
//         return decryptString(encodedEncryptedContent, decryptionKey);
//     }

//     public static void main(String[] args) {
//         try {
//             String testString = "Hello, this is a test string!";
//             String inputFilePath = "C:\\Users\\vinay\\Desktop\\dockercommands.txt"; // Create this file with some sample content
//             String encryptedFilePath = "test_encrypted.txt";
//             Test test = new Test();

//             // Test string encryption and decryption
//             String encryptedString = test.encryptString(testString, Test.DEFAULT_KEY);
//             System.out.println("Encrypted String: " + encryptedString); // Readable format

//             String decryptedString = test.decryptString(encryptedString, Test.DEFAULT_KEY);
//             System.out.println("Decrypted String: " + decryptedString);

//             // Test file encryption and decryption
//             String encryptedFileContent = test.encryptFile(inputFilePath, encryptedFilePath, Test.DEFAULT_KEY);
//             // System.out.println("Encrypted File Content: " + encryptedFileContent);

//             String decryptedFileContent = test.decryptFile(encryptedFilePath, Test.DEFAULT_KEY);
//             // System.out.println("Decrypted File Content: " + decryptedFileContent);

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }
package com.waf;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.file.*;

public class Test {
    private static final String DEFAULT_KEY = "VVxjC-BkcMff4vxqD0RhZcCsf2T9OVotNm-Y4vwAyhM=";

    public String encryptString(String inputString) throws Exception {
        // Call the method with DEFAULT_KEY
        return encryptString(inputString, DEFAULT_KEY);
    }

    public String decryptString(String encryptedString) throws Exception {
        // Call the method with DEFAULT_KEY
        return decryptString(encryptedString, DEFAULT_KEY);
    }

    public String encryptFile(String filePath, String outputFile) throws Exception {
        // Call the method with DEFAULT_KEY
        return encryptFile(filePath, outputFile, DEFAULT_KEY);
    }

    public String decryptFile(String encryptedFilePath) throws Exception {
        // Call the method with DEFAULT_KEY
        return decryptFile(encryptedFilePath, DEFAULT_KEY);
    }

    public String decryptFile(String encryptedFilePath, String decryptionKey) throws Exception {
        // Read the Base64-encoded encrypted content from the file
        String encodedEncryptedContent = new String(Files.readAllBytes(Paths.get(encryptedFilePath)));

        // Decrypt the content (input is Base64 string)
        return decryptString(encodedEncryptedContent, decryptionKey);
    }

    public String encryptFile(String filePath, String outputFile, String encryptionKey) throws Exception {
        // Read the file content
        String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));

        // Encrypt the file content (returns Base64-encoded string)
        String encryptedContent = encryptString(fileContent, encryptionKey);

        // Save the Base64-encoded encrypted content to a file
        Files.write(Paths.get(outputFile), encryptedContent.getBytes());
        System.out.println("File encrypted successfully and saved in readable format!");

        return encryptedContent; // Return readable encrypted content
    }

    public String encryptString(String inputString, String encryptionKey) throws Exception {
        // Decode the encryption key
        byte[] key = Base64.getUrlDecoder().decode(encryptionKey);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // Generate a random IV
        byte[] iv = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Initialize cipher with AES and CBC mode
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        // Encrypt the input string
        byte[] encryptedBytes = cipher.doFinal(inputString.getBytes());

        // Combine IV and encrypted content (needed for decryption)
        byte[] ivAndEncrypted = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, ivAndEncrypted, iv.length, encryptedBytes.length);

        // Encode as Base64 for storage
        return Base64.getEncoder().encodeToString(ivAndEncrypted);
    }

    public String decryptString(String encryptedString, String encryptionKey) throws Exception {
        // Decode the encryption key
        byte[] key = Base64.getUrlDecoder().decode(encryptionKey);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // Decode Base64 input
        byte[] ivAndEncrypted = Base64.getDecoder().decode(encryptedString);

        // Extract IV from the input
        byte[] iv = new byte[16];
        System.arraycopy(ivAndEncrypted, 0, iv, 0, iv.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Extract encrypted content
        byte[] encryptedBytes = new byte[ivAndEncrypted.length - iv.length];
        System.arraycopy(ivAndEncrypted, iv.length, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        // Decrypt the content
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }

    public static void main(String[] args) {
        try {
            String inputFilePath = "D:\\allprojects\\java-projects\\webautomationframework\\src\\main\\resources\\jinjava_template.html"; // Create this file with some sample content
            String encryptedFilePath = "D:\\allprojects\\java-projects\\webautomationframework\\src\\main\\resources\\encrypted_jinjava_file.jinjav";

            String inputtsFilePath = "D:\\allprojects\\java-projects\\webautomationframework\\src\\main\\resources\\jinjava_ts_template.html"; // Create this file with some sample content
            String encryptedtsFilePath = "D:\\allprojects\\java-projects\\webautomationframework\\src\\main\\resources\\encrypted_jinjava_ts_file.jinjav";

            String testString = "Hello, this is a test string!";
            Test test = new Test();

            // Encrypt the string
            String encryptedString = test.encryptString(testString);
            System.out.println("Encrypted String: " + encryptedString);

            // Decrypt the string
            String decryptedString = test.decryptString(encryptedString);
            System.out.println("Decrypted String: " + decryptedString);

            // Test file encryption and decryption
            String encryptedFileContent = test.encryptFile(inputFilePath, encryptedFilePath);
            // System.out.println("Encrypted File Content: " + encryptedFileContent);

            String encryptedtsFileContent = test.encryptFile(inputtsFilePath, encryptedtsFilePath);

            String decryptedFileContent = test.decryptFile(encryptedFilePath);
            String decryptedtsFileContent = test.decryptFile(encryptedtsFilePath);
            System.out.println("Decrypted File Content: " + decryptedtsFileContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
