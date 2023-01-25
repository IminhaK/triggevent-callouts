package iminhak.ultimate;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.persistence.gui.JobSortGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class OmegaProtocolPrioGUI implements DutyPluginTab {

    private final TOPDayZeroAndBleedingEdge top;
    private JobSortGui jsg;
    private JPanel inner;

    public OmegaProtocolPrioGUI(TOPDayZeroAndBleedingEdge top) {
        this.top = top;
    }

    @Override
    public String getTabName() {
        return "TOP Automarks Prio";
    }

    @Override
    public Component getTabContents() {
        jsg = new JobSortGui(top.get_sortSetting());
        RefreshLoop<JobSortGui> refresher = new RefreshLoop<>("TopAmRefresh", jsg, JobSortGui::externalRefresh, unused -> 10_000L);
        TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("TOP Automarks Prio") {
            @Override
            public void setVisible(boolean aFlag) {
                super.setVisible(aFlag);
                if (aFlag) {
                    jsg.externalRefresh();
                    refresher.startIfNotStarted();
                }
            }
        };
        outer.setLayout(new BorderLayout());
        GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

        inner = new JPanel();
        inner.setLayout(new GridBagLayout());
        ReadOnlyText helpText = new ReadOnlyText("""
                Priority for all automarkers. Higher will be marked first.
                """);

        inner.add(helpText);
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        inner.add(jsg.getResetButton(), c);
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        c.weighty = 1;
        inner.add(jsg.getJobListWithButtons(), c);
        c.gridx++;
        c.weightx = 1;
        inner.add(jsg.getPartyPane(), c);

        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    @Override
    public KnownDuty getDuty() {
        return KnownDuty.OmegaProtocol;
    }

    @Override
    public int getSortOrder() {
        return 101;
    }
}
