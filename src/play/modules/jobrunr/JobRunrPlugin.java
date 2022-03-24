package play.modules.jobrunr;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import redis.clients.jedis.JedisPool;

public class JobRunrPlugin extends PlayPlugin {

    private JobRunrConfiguration config;

    public static boolean isWorkerNode() {
        return Play.configuration.getProperty("jobrunr.worker", "false").equals("true");
    }

    public static String getRedisURL() {
        return Play.configuration.getProperty("jobrunr.redisurl");
    }

    private String getRedisPrefix() {
        String environmentName = Play.configuration.getProperty("application.env", "DEV");
        String appName = Play.configuration.getProperty("application.name", "any");
        return "jobrunr:" + appName + ":" + environmentName;
    }

    @Override
    public void onApplicationStart() {
        Logger.info("Starting JobRunr, worker : " + isWorkerNode() + ", prefix : " + getRedisPrefix());

        String redisUrl = getRedisURL();
        JedisPool workerPool = new JedisPool(redisUrl);
        config = JobRunr.configure()
                .useStorageProvider(new JedisRedisStorageProvider(workerPool, getRedisPrefix()))
                .useBackgroundJobServerIf(isWorkerNode());

        // dashboard doesn't play well w/ play reloading, only start in non-dev environments
        if (isWorkerNode()) {
            try {
                config.useDashboard(Integer.parseInt(Play.configuration.getProperty("jobrunr.adminport", "9999")));
            } catch (Exception e) {
                if (!Play.mode.isDev()) {
                    Logger.error("Unable to start the JobRunr Web UI.", e);
                } else {
                    Logger.info("Unable to start the JobRunr Web UI.  In dev this is usually due to dynamic reloading.");
                }
            }
        }
        config.initialize();
    }
}