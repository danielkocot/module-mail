package com.reedelk.mail.component;

import com.reedelk.mail.internal.SchedulerProvider;
import com.reedelk.mail.internal.listener.imap.IMAPIdleListener;
import com.reedelk.mail.internal.listener.imap.IMAPPollingStrategy;
import com.reedelk.runtime.api.annotation.*;
import com.reedelk.runtime.api.component.AbstractInbound;
import org.osgi.service.component.annotations.Component;

import static com.reedelk.runtime.api.commons.ConfigurationPreconditions.requireNotNull;
import static org.osgi.service.component.annotations.ServiceScope.PROTOTYPE;

@ModuleComponent("Mail Listener (IMAP)")
@Description("The Email listener can be used to trigger events whenever new emails " +
        "are received on the server.")
@Component(service = IMAPMailListener.class, scope = PROTOTYPE)
public class IMAPMailListener extends AbstractInbound {

    @Property("IMAP Connection")
    @Group("General")
    private IMAPConfiguration configuration;

    @Property("Strategy")
    @Example("IDLE")
    @DefaultValue("POLLING")
    private IMAPListeningStrategy strategy;

    @Property("Poll Interval")
    @Group("General")
    @Hint("120000")
    @Example("120000")
    @DefaultValue("60000")
    @Description("Poll interval delay. New messages will be checked every T + 'poll interval' time.")
    @When(propertyName = "strategy", propertyValue = "POLLING")
    @When(propertyName = "strategy", propertyValue = When.NULL)
    private Integer pollInterval;

    @Property("Limit")
    @Hint("10")
    @Example("10")
    @Group("General")
    @Description("Limits the number of emails to be processed.")
    private Integer limit;

    @Property("Search flags")
    @When(propertyName = "strategy", propertyValue = "POLLING")
    @When(propertyName = "strategy", propertyValue = When.NULL)
    private IMAPFlags flags;

    @Property("Delete on success")
    @DefaultValue("false")
    @Example("true")
    @Group("General")
    @Description("If true deletes completely a message from the mailbox. If you only want to mark a message as 'deleted' use the property below.")
    private Boolean deleteOnSuccess;

    @Property("Batch messages")
    @DefaultValue("false")
    @Example("true")
    @Group("General")
    @Description("If true emails are batched in a list")
    private Boolean batch;

    private IMAPIdleListener idle;
    private SchedulerProvider schedulerProvider;
    private IMAPPollingStrategy pollingStrategy;

    @Override
    public void onStart() {
        requireNotNull(IMAPMailListener.class, configuration, "IMAP Configuration");
        requireNotNull(IMAPMailListener.class, configuration.getHost(), "IMAP hostname must not be empty.");
        requireNotNull(IMAPMailListener.class, configuration.getUsername(), "IMAP username must not be empty.");
        requireNotNull(IMAPMailListener.class, configuration.getPassword(), "IMAP password must not be empty.");

        if (IMAPListeningStrategy.POLLING.equals(strategy)) {
            pollingStrategy = new IMAPPollingStrategy(this, configuration, flags, deleteOnSuccess, batch, limit);
            schedulerProvider = new SchedulerProvider();
            schedulerProvider.schedule(pollInterval, pollingStrategy);
        } else {
            // IDLE
            idle = new IMAPIdleListener(this, configuration, deleteOnSuccess, batch);
            idle.start();
        }
    }

    @Override
    public void onShutdown() {
        if (pollingStrategy != null) {
            pollingStrategy.stop();
        }
        if (schedulerProvider != null) {
            schedulerProvider.stop();
        }
        if (idle != null) {
            idle.stop();
        }
    }

    public IMAPConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IMAPConfiguration configuration) {
        this.configuration = configuration;
    }

    public IMAPListeningStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(IMAPListeningStrategy strategy) {
        this.strategy = strategy;
    }

    public IMAPFlags getFlags() {
        return flags;
    }

    public void setFlags(IMAPFlags flags) {
        this.flags = flags;
    }

    public Integer getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Integer pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Boolean getDeleteOnSuccess() {
        return deleteOnSuccess;
    }

    public void setDeleteOnSuccess(Boolean deleteOnSuccess) {
        this.deleteOnSuccess = deleteOnSuccess;
    }

    public Boolean getBatch() {
        return batch;
    }

    public void setBatch(Boolean batch) {
        this.batch = batch;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

}
