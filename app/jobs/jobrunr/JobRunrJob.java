package jobs.jobrunr;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import play.modules.jobrunr.JobRunrPlugin;

import java.time.Instant;

public abstract class JobRunrJob<T extends JobRunrJob> implements JobRequest, JobRequestHandler<T> {

    private static final String LOCK_PREFIX = "job-locks:";
    // Redisson requires a redis:// prefix
    public static final String REDIS_PREFIX = "redis://";

    private int timeDelay;

    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return this.getClass();
    }

    @Override
    public final void run(T jobRequest) throws Exception {
        jobRequest.doJob();
    }

    protected abstract void doJob() throws Exception;

    public T run(){
        if (timeDelay == 0) {
            BackgroundJobRequest.enqueue(this);
        } else {
            BackgroundJobRequest.schedule(Instant.now().plusSeconds(timeDelay), this);
        }
        return (T) this;
    }

    public T runIn(int time) {
        timeDelay = time;
        return (T) this;
    }

    public T seconds(){
        return run();
    }

    public T minutes(){
        timeDelay = timeDelay * 60;
        return run();
    }

    public NoThrowAutoCloseable withLock(String lockName) {
        Config config = new Config();
        config.useSingleServer().setAddress(REDIS_PREFIX + JobRunrPlugin.getRedisURL());
        RedissonClient client = Redisson.create();
        RLock lock = client.getLock(LOCK_PREFIX + lockName);
        lock.lock();
        return lock::unlock;
    }

    public interface NoThrowAutoCloseable extends AutoCloseable{
        void close();
    }

}
