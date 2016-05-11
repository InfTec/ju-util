package ch.inftec.ju.jasypt;

import static junit.framework.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.util.text.BasicTextEncryptor;
import org.junit.After;
import org.junit.Test;

/**
 * Created by rotscher on 10/9/14.
 */
public class JasyptPropertiesDecryptorTest {

    List<String> filesToDelete = new ArrayList<>();

    @After
    public void cleanup() {
        for (String fileName : filesToDelete) {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }
    }


    @Test
    public void testInitialization() {

        final String passwordFileEnvValue = "jasypt-init-test.txt";
        try {
            FileWriter writer = new FileWriter(passwordFileEnvValue);
            writer.write("jasypt-init-test");
            writer.flush();
            writer.close();
            filesToDelete.add(passwordFileEnvValue);

            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new Environment() {
                @Override
                public String getenv(String envName) {
                    return passwordFileEnvValue;
                }
            });
            assertNotNull(decryptor);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testInitializationWithEmptyFile() {

        final String passwordFileEnvValue = "jasypt-emptyfile-test.txt";
        try {
            FileWriter writer = new FileWriter(passwordFileEnvValue);
            writer.write("     \n     \n  ");
            writer.flush();
            writer.close();
            filesToDelete.add(passwordFileEnvValue);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        try {
            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new Environment() {
            @Override
            public String getenv(String envName) {
                return passwordFileEnvValue;
            }
        });
            fail("should fail in case the given file has no content");
        } catch (Exception e) {
            //ok
        }
    }

    @Test
    public void testInitializationWithoutFile() {

        try {
            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new Environment() {
                @Override
                public String getenv(String envName) {
                    return "non-exisiting-file.txt";
                }
            });
            fail("should fail in case the given file doesn't exist");
        } catch (Exception e) {
            //ok
        }
    }

    @Test
    public void testInitializationWithNullEnvironment() {

        try {
            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new Environment() {
                @Override
                public String getenv(String envName) {
                    return null;
                }
            });
            fail("should fail in case the environment variable is null");
        } catch (Exception e) {
            //ok
        }
    }


    @Test
    public void testInitializationWithEmptyStringEnvironment() {

        try {
            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new Environment() {
                @Override
                public String getenv(String envName) {
                    return "      ";
                }
            });
            fail("should fail in case the environment variable is null");
        } catch (Exception e) {
            //ok
        }
    }

    @Test
    public void testDecryption() {

        final String passwordFileEnvValue = "jasypt-password.txt";
        final String password = "foobar";
		File inputFile = new File("target/input-1.properties");
        try {

            createPropertyFile(inputFile, password);
            assertTrue("no encrypted values!!!!", fileContainsEncryptedValues(inputFile));
            FileWriter writer = new FileWriter(passwordFileEnvValue);
            writer.write("foobar");
            writer.flush();
            writer.close();
            filesToDelete.add(passwordFileEnvValue);
			filesToDelete.add("target/input-1.properties");
			filesToDelete.add("target/output-1.properties");
            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new Environment() {
                @Override
                public String getenv(String envName) {
                    return passwordFileEnvValue;
                }
            });
            assertNotNull(decryptor);

			decryptor.processPropertyFile(inputFile, new File("target/output-1.properties"));
			File decryptedFile = new File("target/output-1.properties");
            assertTrue("decrypted file " + decryptedFile.getName() + " doesn't exist", decryptedFile.exists());
            assertTrue(keyMatchesValue(decryptedFile));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSingleInputDecryption() {

        final String passwordFileEnvValue = "jasypt-password.txt";
        final String password = "foobar";
        final String clearText = "hello world!";
        try {

            String encryptedInput = createSingleInput(clearText, password);
            FileWriter writer = new FileWriter(passwordFileEnvValue);
            writer.write("foobar");
            writer.flush();
            writer.close();
            filesToDelete.add(passwordFileEnvValue);
            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new Environment() {
                @Override
                public String getenv(String envName) {
                    return passwordFileEnvValue;
                }
            });
            assertNotNull(decryptor);

            String output = decryptor.processInput("decrypt.sh", encryptedInput);
            assertEquals(clearText, output);

            String output1 = decryptor.processInput("decrypt.sh", "ENC(" + encryptedInput + ")");
            assertEquals(clearText, output1);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDecryptionWithWrongPassword() {
        final String passwordFileEnvValue = "jasypt-password.txt";
        final String password = "foobar";
		File inputFile = new File("target/input-1.properties");
        try {

            createPropertyFile(inputFile, password);
            assertTrue("no encrypted values!!!!", fileContainsEncryptedValues(inputFile));
            FileWriter writer = new FileWriter(passwordFileEnvValue);
            writer.write("wrongpassword");
            writer.flush();
            writer.close();
            filesToDelete.add(passwordFileEnvValue);
			filesToDelete.add("target/input-1.properties");
			filesToDelete.add("target/output-1.properties");
            JasyptPropertiesDecryptor decryptor = new JasyptPropertiesDecryptor(new Environment() {
                @Override
                public String getenv(String envName) {
                    return passwordFileEnvValue;
                }
            });
            assertNotNull(decryptor);

			decryptor.processPropertyFile(inputFile, new File("target/output-1.properties"));
            fail("decryption with wrong password should not work");
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (EncryptionOperationNotPossibleException e) {
            //ok
        }
    }

    private String createSingleInput(String clearText, String password) throws IOException {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(password);
        return textEncryptor.encrypt(clearText);
    }

    private void createPropertyFile(File propertyFile, String password) throws IOException {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(password);

        Properties properties = new Properties();
        properties.setProperty("key.1.clear", "key.1.clear");
        properties.setProperty("key.2.clear", "key.2.clear");
        properties.setProperty("key.3.enc", String.format("ENC(%s)", textEncryptor.encrypt("key.3.enc")));
        properties.setProperty("key.4.clear", "key.4.clear");
        properties.setProperty("key.5.enc", String.format("ENC(%s)", textEncryptor.encrypt("key.5.enc")));
        properties.store(new FileOutputStream(propertyFile), "");

    }

    private boolean fileContainsEncryptedValues(File file) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(file));
        for (Object value : props.values()) {
            if (value.toString().startsWith("ENC")) {
                return true;
            }
        }

        return false;
    }

    private boolean keyMatchesValue(File decryptedPropertyFile) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(decryptedPropertyFile));
        for (Object key : props.keySet()) {
            if (!key.toString().equals(props.getProperty(key.toString()))) {
                return false;
            }
        }

        return true;
    }
}
