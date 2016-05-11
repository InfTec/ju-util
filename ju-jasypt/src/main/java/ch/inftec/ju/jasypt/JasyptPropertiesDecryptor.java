package ch.inftec.ju.jasypt;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.intf.cli.JasyptPBEStringDecryptionCLI;
import org.jasypt.properties.EncryptableProperties;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

/**
 * Created by rotscher on 10/9/14.
 */
public class JasyptPropertiesDecryptor {


    private final String password;

    public static void main(String[] args) {

        try {
            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new DefaultEnvironment());
            if (args.length == 2) {
                String method = args[0];
                String input = args[1];
                System.out.println(decryptor.processInput(method, input));
            } else if (args.length == 3) {
                String method = args[0];

                if ("encrypt.sh".equals(method)) {
                    System.err.println("method encrypt.sh is not supported for property files");
                    System.exit(1);
                }
                String inputFileName = args[1];
                String outputFileName = args[2];
                decryptor.processPropertyFile(new File(inputFileName), new File(outputFileName));
            } else {
                System.err.println("this programm can be use as follows:");
                System.err.println("   1) encrypt/decrypt single values");
                System.err.println("        jasypt-encrypt.sh CLEAR_TEXT");
                System.err.println("        jasypt-decrypt.sh DECRYPTED_TEXT");
                System.err.println("   2) decrypt property files");
                System.err.println("        jasypt-decrypt.sh input.filename output.filename");

                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        System.exit(0);

    }

    public JasyptPropertiesDecryptor(Environment environment) throws IOException {
        String passwordFileName = environment.getenv("JASYPT_PASSWORD_FILE");
        if (passwordFileName == null || passwordFileName.trim().length() == 0) {

            //lookup of ESW_ENCRYPTION_KEY is deprecated!!
            password = environment.getenv("ESW_ENCRYPTION_KEY");

            if (password == null || password.trim().length() == 0) {
                throw new IllegalArgumentException("the environment variable 'JASYPT_PASSWORD_FILE' must be set to a valid file");
            }

            return;
        }

        File passwordFile = new File(passwordFileName);
        List<String> lines = Files.readAllLines(passwordFile.toPath(), Charset.forName("UTF-8"));

        if (lines.size() < 1) {
            throw new IOException(String.format("no password set on line one in %s", passwordFileName));
        }

        password = lines.get(0).trim();
        if (password.length() < 1) {
            throw new IOException(String.format("empty password is not allowed in %s", passwordFileName));
        }
    }

    public String processInput(String method, String input) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);

        if ("encrypt.sh".equals(method)) {
            return encryptor.encrypt(input);
        } else if ("decrypt.sh".equals(method)) {
            input = input.replace("ENC(", "");
            input = input.replace(")", "");
            return encryptor.decrypt(input);
        }

        throw new RuntimeException(String.format("method %s not allowed%n", method));
    }

    public void processPropertyFile(File input, File output) throws IOException {
        InputStream fileInputStream = new FileInputStream(input);
        Properties decryptedProperties = decrypt(fileInputStream, password);
        OutputStream fileOutputStream = new FileOutputStream(output);
        decryptedProperties.store(fileOutputStream, "decrypted by jasypt");
        fileOutputStream.close();
        fileInputStream.close();
    }


    private Properties decrypt(InputStream propertyInputStream, String password) throws IOException {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);
        Properties props = new EncryptableProperties(encryptor);
        props.load(propertyInputStream);
        return props;
    }
}
