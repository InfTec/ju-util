package ch.inftec.ju.devops;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 * User: rotscher
 * Date: 2/27/14
 * Time: 9:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class FilesystemCleanupTest {

    Logger logger = Logger.getLogger(FilesystemCleanupTest.class);

    /* this is the maven repo directory for the unit test */
    private static File baseDir = new File("target", "maven-repo");
    private static File repoDir;

    /* which directories should be removed */
    private final String[] filenamePatterns = new String[]{"-S-", "-custom-"};

    private void cleanup() {

    }

    @Before
    public void setUp() throws Exception {
        //FilesystemCleanup cleanup = new FilesystemCleanup();
        //cleanup.removeDirectory(repoDir);
        baseDir.mkdir();
    }

    private void createDefaultTestSet(boolean createCleanupAuthFile) throws IOException {
        long now = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
        createDefaultTestSet(30, createCleanupAuthFile, now);
    }

    private void createDefaultTestSet(int numbOfReleases, boolean createCleanupAuthFile, long now) throws IOException {
        repoDir.mkdir();
        if (createCleanupAuthFile) {
            createCleanupAuthorisationFile();
        }

        //create releases
        for (int i = 0, j = numbOfReleases; i < numbOfReleases; i++, j--) {
            long modTime = now - (((long) i) * 24 * 60 * 60 * 1000);
            logger.trace(String.format("%s", new Date(modTime).toString()));
            createRelease("0.5-RC-" + j, modTime);
            if (i % 2 == 0) {
                createRelease("0.6-S-" + j, modTime);
            } else {
                createRelease("0.6-custom-" + j, modTime);
            }
        }
    }

    private void create2ReleasesTestSet() throws IOException {
        long now = System.currentTimeMillis() - 1 * 60 * 60 * 1000;
        createDefaultTestSet(2, true, now);
    }

    private void create2VeryOldReleasesTestSet() throws IOException {

        //fifty days ago
        long now = System.currentTimeMillis() - 50 * 24 * 60 * 60 * 1000;
        createDefaultTestSet(2, true, now);
    }

    private void createOnly4youngerThanReleasesTestSet() throws IOException {
        repoDir.mkdir();
        createCleanupAuthorisationFile();

        long now = System.currentTimeMillis() - 1 * 60 * 60 * 1000;

        for (int i = 0, j = 40; i < 4; i++, j--) {
            long modTime = now - (((long) i) * 24 * 60 * 60 * 1000);
            logger.trace(String.format("%s", new Date(modTime).toString()));
            createRelease("0.5-RC-" + j, modTime);
            if (i % 2 == 0) {
                createRelease("0.6-S-" + j, modTime);
            } else {
                createRelease("0.6-custom-" + j, modTime);
            }
        }

        long twelveDaysAgo = System.currentTimeMillis() - 12 * 24 * 60 * 60 * 1000;
        for (int i = 4, j = 36; i < 40; i++, j--) {
            long modTime = twelveDaysAgo - (((long) i) * 24 * 60 * 60 * 1000);
            logger.trace(String.format("%s", new Date(modTime).toString()));
            createRelease("0.5-RC-" + j, modTime);
            if (i % 2 == 0) {
                createRelease("0.6-S-" + j, modTime);
            } else {
                createRelease("0.6-custom-" + j, modTime);
            }
        }

        assertEquals(40, countFiles());
    }

    private void create30youngerThanReleasesTestSet() throws IOException {
        repoDir.mkdir();
        createCleanupAuthorisationFile();

        long now = System.currentTimeMillis() - 1 * 60 * 60 * 1000;

        for (int i = 0, j = 40; i < 30; i++, j--) {
            //a release every 6 hours => 30 releases in the last seven 7 days
            long modTime = now - (((long) i) * 6 * 60 * 60 * 1000);
            logger.trace(String.format("%s", new Date(modTime).toString()));
            createRelease("0.5-RC-" + j, modTime);
            if (i % 2 == 0) {
                createRelease("0.6-S-" + j, modTime);
            } else {
                createRelease("0.6-custom-" + j, modTime);
            }
        }

        long twelveDaysAgo = System.currentTimeMillis() - 12 * 24 * 60 * 60 * 1000;
        for (int i = 30, j = 10; i < 40; i++, j--) {
            long modTime = twelveDaysAgo - (((long) i) * 24 * 60 * 60 * 1000);
            logger.trace(String.format("%s", new Date(modTime).toString()));
            createRelease("0.5-RC-" + j, modTime);
            if (i % 2 == 0) {
                createRelease("0.6-S-" + j, modTime);
            } else {
                createRelease("0.6-custom-" + j, modTime);
            }
        }
    }

    private void createRelease(String version, long modTime) throws IOException {
        File dir1 = new File(repoDir, version);
        dir1.mkdir();
        createFileInDir(dir1, "test-artifact-" + version + ".pom", modTime);
        dir1.setLastModified(modTime);
    }

    private void createFileInDir(File dir, String name, long modTime)
            throws IOException {
        File file = new File(dir, name);
        file.createNewFile();
        file.setLastModified(modTime);
    }


    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    @Test
    public void testNonDirectoryCleanup() {
        logger.debug(String.format("%s", "testNonDirectoryCleanup"));
        repoDir = new File(baseDir, "testNonDirectoryCleanup");
        try {
            createDefaultTestSet(false);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        try {
            FilesystemCleanup cleanup = new FilesystemCleanup();
            cleanup.cleanup(new File(repoDir + "/.cleanup-authorisation"));
            fail("should throw an illegal argument exception, as only directories can be cleaned");
        } catch (RuntimeException e) {
            //ok
        }
    }

    @Test
    public void testUnauthorizedCleanup() {
        logger.debug(String.format("%s", "testUnauthorizedCleanup"));
        repoDir = new File(baseDir, "testUnauthorizedCleanup");
        try {
            createDefaultTestSet(false);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        FilesystemCleanup cleanup = new FilesystemCleanup();
        int result = -1;
        try {
            result = cleanup.cleanup(repoDir);
            fail("");
        } catch (RuntimeException e) {
            //ok
            assertEquals(-1, result);
        }
    }


    @Test
    public void testCleanupWithNonMatchingPatterns() {
        logger.debug(String.format("%s", "testUnauthorizedCleanup"));
        repoDir = new File(baseDir, "testUnauthorizedCleanup");
        try {
            createDefaultTestSet(false);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        FilesystemCleanup cleanup = new FilesystemCleanup();
        int result = -1;
        try {
            result = cleanup.cleanup(repoDir);
            fail("");
        } catch (RuntimeException e) {
            //ok
            assertEquals(-1, result);
        }
    }

    @Test
    public void testDefaultCleanup() {
        logger.debug(String.format("%s", "testDefaultCleanup"));
        repoDir = new File(baseDir, "testDefaultCleanup");
        try {
            createDefaultTestSet(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        try {
            FilesystemCleanup cleanup = new FilesystemCleanup();
            int result = cleanup.cleanup(repoDir);
            assertEquals(20, result);
            assertEquals(10, countFiles());
        } catch (RuntimeException e) {
            //this should not happen
            fail(e.getMessage());
        }
    }


    @Test
    public void testDefaultKeepMaxReleasesCleanup() {
        logger.debug(String.format("%s", "testDefaultKeepMaxReleasesCleanup"));
        repoDir = new File(baseDir, "testDefaultKeepMaxReleasesCleanup");
        try {
            create30youngerThanReleasesTestSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        try {
            FilesystemCleanup cleanup = new FilesystemCleanup();
            int result = cleanup.cleanup(repoDir);
            assertEquals(25, result);
            assertEquals(15, countFiles());
        } catch (RuntimeException e) {
            //this should not happen
            fail(e.getMessage());
        }
    }

    @Test
    public void testDefaultKeepMinReleasesCleanup() {
        logger.debug(String.format("%s", "testDefaultKeepMinReleasesCleanup"));
        repoDir = new File(baseDir, "testDefaultKeepMinReleasesCleanup");
        try {
            create2ReleasesTestSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        try {
            FilesystemCleanup cleanup = new FilesystemCleanup();
            int result = cleanup.cleanup(repoDir);
            assertEquals(0, result);
            assertEquals(2, countFiles());
        } catch (RuntimeException e) {
            //this should not happen
            fail(e.getMessage());
        }
    }

    @Test
    public void testDefaultKeepMin2ReleasesCleanup() {
        logger.debug(String.format("%s", "testDefaultKeepMin2ReleasesCleanup"));
        repoDir = new File(baseDir, "testDefaultKeepMin2ReleasesCleanup");
        try {
            createOnly4youngerThanReleasesTestSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        try {
            FilesystemCleanup cleanup = new FilesystemCleanup();
            int result = cleanup.cleanup(repoDir);
            assertEquals(35, result);
            assertEquals(FilesystemCleanup.MIN_RELEASES, countFiles());
        } catch (RuntimeException e) {
            //this should not happen
            fail(e.getMessage());
        }
    }

    @Test
    public void testDefaultKeepMin3ReleasesCleanup() {
        logger.debug(String.format("%s", "testDefaultKeepMin3ReleasesCleanup"));
        repoDir = new File(baseDir, "testDefaultKeepMin3ReleasesCleanup");
        try {
            create2VeryOldReleasesTestSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        try {
            FilesystemCleanup cleanup = new FilesystemCleanup();
            int result = cleanup.cleanup(repoDir);
            assertEquals(0, result);
            assertEquals(2, countFiles());
        } catch (RuntimeException e) {
            //this should not happen
            fail(e.getMessage());
        }
    }

    @Test
    public void testLightweightReleaseNameFilter() {
        logger.debug(String.format("%s", "testLightweightReleaseNameFilter"));
        repoDir = new File(baseDir, "testLightweightReleaseNameFilter");

        try {
            createDefaultTestSet(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }

        try {
            FilesystemCleanup cleanup = new FilesystemCleanup();
            assertEquals(30, countFiles());
        } catch (IllegalArgumentException e) {
            //this should not happen
            fail(e.getMessage());
        }
    }

    @Test
    public void testMain() {
        logger.debug(String.format("%s", "testMain"));
        repoDir = new File(baseDir, "testMain");
        try {
            createDefaultTestSet(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }
        System.setProperty("directory", repoDir.getAbsolutePath());
        System.setProperty("dryRun", "false");

        try {
            FilesystemCleanup.main(new String[0]);
        } catch (FileNotFoundException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testMainWithoutSetDirectory() {
        logger.debug(String.format("%s", "testMainWithoutSetDirectory"));
        repoDir = new File(baseDir, "testMainWithoutSetDirectory");
        System.setProperty("dryRun", "false");
        try {
            createDefaultTestSet(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }
        //we don't set the directory system property

        try {
            FilesystemCleanup.main(new String[0]);
            fail("test should fail");
        } catch (FileNotFoundException e) {
            //should end in an exception
        }
    }

    @Test
    public void testMainWithWrongDirectorySet() {
        logger.debug(String.format("%s", "testMainWithWrongDirectorySet"));
        repoDir = new File(baseDir, "testMainWithWrongDirectorySet");
        try {
            createDefaultTestSet(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Not yet implemented");
        }
        System.setProperty("directory", "foobar");
        System.setProperty("dryRun", "false");

        try {
            FilesystemCleanup.main(new String[0]);
            fail("test should fail");
        } catch (FileNotFoundException e) {
            //should end in an exception
        }
    }

    private int countFiles() {
        return repoDir.listFiles(new AuthorizedFilesNameFilter(filenamePatterns)).length;
    }

    private void createCleanupAuthorisationFile() throws IOException {
        File file = new File(repoDir, ".cleanup-authorisation");

        PrintWriter writer = new PrintWriter(file, "UTF-8");
        for (String each : filenamePatterns) {
            writer.println(each);
        }
        writer.flush();
        writer.close();
    }

}
