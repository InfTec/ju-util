package ch.inftec.ju.devops;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: rotscher
 * Date: 2/27/14
 * Time: 9:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class FilesystemCleanup {

    private static final Logger logger = Logger.getLogger(FilesystemCleanup.class);

    public static final int OLDER_THAN = 10;
    public static final int MAX_RELEASES = 15;
    public static final int MIN_RELEASES = 5;

    private boolean dryRun = true;

    public FilesystemCleanup() {
        this(false);
    }

    public FilesystemCleanup(boolean dryRun) {
        this.dryRun = dryRun;
    }
    
    public int cleanup(File directory) {
        return cleanup(directory, OLDER_THAN, MAX_RELEASES, MIN_RELEASES);
    }

    /**
     * delete releases which are older than the given parameter (in days, from today's midnight).
     * This rule has two constraints:
     * when there are still more than the keepMaxReleases available, also releases within the olderThan
     * range are deleted, the oldest first until the keepMaxReleases is reached.
     *
     * The keepMinReleases prevents the deletion of releases in case there are only few ones
     * even when they are older than the olderThan parameter.
     *
     * @param olderThan
     * @param keepMaxReleases
     * @param keepMinReleases
     * @return the total number of deleted releases
     */
    public int cleanup(File directory, int olderThan, int keepMaxReleases, int keepMinReleases) {
        if (!directory.isDirectory()) {
            throw new RuntimeException(String.format("path %s is not a directory", directory.getAbsolutePath()));
        }

        File cleanupAuthorisationFile = new File(directory, ".cleanup-authorisation");
        if (!cleanupAuthorisationFile.exists()) {
            throw new RuntimeException(String.format("directory %s must contain a file named .cleanup-authorisation, otherwise a cleanup is not possible", directory.getAbsolutePath()));
        }

        logger.info(String.format("cleanup directory %s (olderThan %d days, keepMaxReleases %d, keepMinReleases %d)", directory.getPath(), olderThan, keepMaxReleases, keepMinReleases));

        File[] files = null;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(cleanupAuthorisationFile));
            List<String> filenamePatterns = new ArrayList<>();

            String line = reader.readLine();
            while (line != null) {
                filenamePatterns.add(line);
                line = reader.readLine();
            }
            files = directory.listFiles(new AuthorizedFilesNameFilter(filenamePatterns.toArray(new String[0])));
        } catch (IOException e) {
            throw new RuntimeException(String.format("directory %s must contain a file named .cleanup-authorisation, otherwise a cleanup is not possible", directory.getAbsolutePath()));
        }


        logger.info(String.format("found %d files to process", files.length));

        if (files.length <= keepMinReleases) {
            logger.info(String.format("number of releases don't reach the keepMinReleases (=%d) value, no releases are deleted", files.length));
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("repodir: %s, number of files: %d", directory.getPath(), files.length));
            }
            return 0;
        }

        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - olderThan);
        Date limit = new Date(cal.getTimeInMillis());

        SortedSet<File> sortedFiles = new TreeSet<>(new ModifiedFileComparator());
        List<File> filesToRemove = new ArrayList<>();

        sortedFiles.addAll(Arrays.asList(files));
        logger.info(String.format("sorted %d files to process", sortedFiles.size()));

        for (Iterator<File> fileIt = sortedFiles.iterator(); fileIt.hasNext(); ) {
            File file = fileIt.next();
            Date modifDate = new Date(file.lastModified());
            if (modifDate.before(limit)) {
                if (sortedFiles.size() <= keepMinReleases) {
                    logger.info(String.format("%s not removed (%s, keep min releases)", file.getName(), modifDate.toString()));
                } else {
                    logger.debug(String.format("%s will be removed (%s)", file.getName(), modifDate.toString()));
                    filesToRemove.add(file);
                    fileIt.remove();
                }
            } else {
                logger.info(String.format("%s not removed (%s, keep because of time))", file.getName(), modifDate.toString()));
            }
        }

        Iterator<File> fileIt = sortedFiles.iterator();
        while (sortedFiles.size() > keepMaxReleases) {
            File file = fileIt.next();
            logger.debug(String.format("%s to be removed (%s, above quota)", file.getName(), new Date(file.lastModified()).toString()));
            filesToRemove.add(file);
            fileIt.remove();
        }

        for (File fileToRemove : filesToRemove) {
            String message = String.format("%s removed (%s)", fileToRemove.getName(), new Date(fileToRemove.lastModified()));

            if (!dryRun) {
                removeDirectory(fileToRemove);
                logger.info(message);
            } else {
                logger.info(String.format("dryRun - %s", message));
            }
        }

        if (!dryRun) {
            logger.info(String.format("%d files removed, %d files kept", filesToRemove.size(), sortedFiles.size()));
        } else {
            logger.info(String.format("%d files removed (dryRun), %d files kept", filesToRemove.size(), sortedFiles.size()));
        }

        return filesToRemove.size();
    }

    public void removeDirectory(File directory) {
        if (!directory.isDirectory()) {
            throw new RuntimeException(String.format("file path %s is not a directory", directory.getAbsolutePath()));
        }

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                removeDirectory(file);
            } else {
                file.delete();
            }
        }

        directory.delete();

    }

    /**
     * main method
     *
     */
    public static void main(String[] args) throws FileNotFoundException {
        File directory = new File(System.getProperty("directory"));

        if (!directory.isDirectory()) {
            throw new FileNotFoundException(String.format("directory %s does not exist", System.getProperty("directory")));
        }

        boolean dryRun = Boolean.parseBoolean(System.getProperty("dryRun", "true"));
        int olderThan = Integer.parseInt(System.getProperty("olderThan", FilesystemCleanup.OLDER_THAN + ""));
        int keepMaxFiles = Integer.parseInt(System.getProperty("keepMax", FilesystemCleanup.MAX_RELEASES + ""));
        int keepMinFiles = Integer.parseInt(System.getProperty("keepMin", FilesystemCleanup.MIN_RELEASES + ""));

        FilesystemCleanup cleanup = new FilesystemCleanup(dryRun);
        logger.info(String.format("start cleaning from %s", directory.getPath()));
        int result = cleanup.cleanup(directory, olderThan, keepMaxFiles, keepMinFiles);
        logger.info(String.format("cleaned %d directories from %s", result, directory.getPath()));
    }

    public static class ModifiedFileComparator implements Comparator<File> {

        @Override
        public int compare(File o1, File o2) {
            Date modifDate1 = new Date(o1.lastModified());
            Date modifDate2 = new Date(o2.lastModified());
            logger.trace(String.format("%s %s - %s %s", o1.getName(), modifDate1.toString(), o2.getName(), modifDate2.toString()));
            int result = modifDate1.compareTo(modifDate2);
            if (result == 0) {
                //when the modification date is the same, one element is excluded from the sorted set
                //maybe this is related to the fact that this compare is not conform to File.equals (and File instances
                //are finally added to the SortedSet). Read something at Stackoverflow.
                //we don't care about ordering then (which might be wrong in rare cases)
                return -1;
            }

            return result;
        }
    }
}
