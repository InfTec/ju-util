package ch.inftec.ju.jasypt;

/**
 * Created by rotscher on 10/9/14.
 */
public class DefaultEnvironment implements Environment {
    @Override
    public String getenv(String envName) {
        return System.getenv(envName);
    }
}
