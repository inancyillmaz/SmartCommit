package com.example.lastplugin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DefaultCommitMessageHandler extends CheckinHandler {
    private final CheckinProjectPanel checkinPanel;

    public DefaultCommitMessageHandler(@NotNull CheckinProjectPanel checkinPanel) {
        checkinPanel.setCommitMessage("Deneme");
        this.checkinPanel = checkinPanel;
    }

    @Override
    public ReturnResult beforeCheckin() {
        String defaultMessage = "Default commit message";
        checkinPanel.setCommitMessage(defaultMessage);
        return ReturnResult.COMMIT;
    }
    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        String defaultMessage = "Default commit message getBeforeCheckinConfigurationPanel";
        checkinPanel.setCommitMessage(defaultMessage);
        return super.getBeforeCheckinConfigurationPanel();
    }

    @Override
    public @Nullable UnnamedConfigurable getBeforeCheckinSettings() {
        return super.getBeforeCheckinSettings();
    }

    @Override
    public @Nullable RefreshableOnComponent getAfterCheckinConfigurationPanel(Disposable parentDisposable) {
        return super.getAfterCheckinConfigurationPanel(parentDisposable);
    }

    @Override
    public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
        return super.beforeCheckin(executor, additionalDataConsumer);
    }

    @Override
    public void checkinSuccessful() {
        super.checkinSuccessful();
    }

    @Override
    public void checkinFailed(List<VcsException> exception) {
        super.checkinFailed(exception);
    }

    @Override
    public void includedChangesChanged() {
        super.includedChangesChanged();
    }

    @Override
    public boolean acceptExecutor(CommitExecutor executor) {
        return super.acceptExecutor(executor);
    }
}


