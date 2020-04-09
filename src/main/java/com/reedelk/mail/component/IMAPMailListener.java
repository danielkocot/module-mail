package com.reedelk.mail.component;

import com.reedelk.mail.internal.SchedulerProvider;
import com.reedelk.mail.internal.listener.imap.IMAPIdleListener;
import com.reedelk.mail.internal.listener.imap.IMAPPollingStrategy;
import com.reedelk.runtime.api.annotation.*;
import com.reedelk.runtime.api.component.AbstractInbound;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.concurrent.ScheduledFuture;

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

    @Property("IMAP Strategy")
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

    @Property("Fetch matchers")
    @When(propertyName = "strategy", propertyValue = "POLLING")
    @When(propertyName = "strategy", propertyValue = When.NULL)
    private IMAPMatcher matcher;

    @Property("Deletes a message if success")
    @DefaultValue("false")
    @Example("true")
    @Group("General")
    @Description("If true deletes completely a message from the mailbox. If you only want to mark a message as 'deleted' use the property below.")
    private Boolean deleteOnSuccess;

    @Property("Mark as deleted if success")
    @DefaultValue("false")
    @Example("true")
    @Group("General")
    @Description("If true marks a message deleted in the mailbox. This flag does not delete the message.")
    private Boolean markAsDeletedOnSuccess;

    @Property("Batch Emails")
    @DefaultValue("false")
    @Example("true")
    @Group("General")
    @Description("If true emails are batched in a list")
    private Boolean batchEmails;

    @Reference
    private SchedulerProvider schedulerProvider;

    private ScheduledFuture<?> scheduled;
    private IMAPIdleListener idle;

    @Override
    public void onStart() {
        requireNotNull(IMAPMailListener.class, configuration, "IMAP Configuration");
        requireNotNull(IMAPMailListener.class, configuration.getHost(), "IMAP hostname must not be empty.");
        requireNotNull(IMAPMailListener.class, configuration.getUsername(), "IMAP username must not be empty.");
        requireNotNull(IMAPMailListener.class, configuration.getPassword(), "IMAP password must not be empty.");

        if (IMAPListeningStrategy.POLLING.equals(strategy)) {
            IMAPPollingStrategy pollingStrategy = new IMAPPollingStrategy(this, configuration, matcher, deleteOnSuccess, markAsDeletedOnSuccess, batchEmails);
            scheduled = schedulerProvider.schedule(pollInterval, pollingStrategy);
        } else {
            // IDLE
            // TODO: Check if for IDLE the delete is just a flag or it can be effectively deleted.
            idle = new IMAPIdleListener(this, configuration, deleteOnSuccess, batchEmails);
            idle.start();
        }
    }

    @Override
    public void onShutdown() {
        schedulerProvider.cancel(scheduled);
        if (idle != null) idle.stop();
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

    public IMAPMatcher getMatcher() {
        return matcher;
    }

    public void setMatcher(IMAPMatcher matcher) {
        this.matcher = matcher;
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

    public Boolean getBatchEmails() {
        return batchEmails;
    }

    public void setBatchEmails(Boolean batchEmails) {
        this.batchEmails = batchEmails;
    }
}
