package iminhak.ultimate;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class OmegaProtocolGUI implements DutyPluginTab {

    private final TOPDayZeroAndBleedingEdge omegaProtocol;
    private JPanel inner;
    private JPanel innerPantokrator;

    private final GlobalUiRegistry reg;

    public OmegaProtocolGUI(TOPDayZeroAndBleedingEdge omegaProtocol, GlobalUiRegistry reg) {
        this.omegaProtocol = omegaProtocol;
        this.reg = reg;
    }

    @Override
    public String getTabName() {
        return "Iminha's Omega Protocol Automarkers";
    }

    @Override
    public Component getTabContents() {
        TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Omega Protocol Automarkers");
        outer.setLayout(new BorderLayout());
        JCheckBox TBDMarkers = new BooleanSettingGui(omegaProtocol.getUseAutomarks(), "Omega Protocol Automarkers").getComponent();
        outer.add(TBDMarkers, BorderLayout.NORTH);
        GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0);

        inner = new JPanel();
        inner.setLayout(new GridBagLayout());
        innerPantokrator = new JPanel();
        innerPantokrator.setLayout(new GridBagLayout());
        JCheckBox usePantokrator = new BooleanSettingGui(omegaProtocol.getUsePantokrator(), "Use Pantokrator").getComponent();
        ReadOnlyText text = new ReadOnlyText("""
                Pantokrator - If a support and dps need to swap sides, they will be marked with Bind 1 and Bind 2
                """);

        inner.add(usePantokrator, c);
        c.gridy++;
        c.weighty++;
        inner.add(text, c);

        omegaProtocol.getUseAutomarks().addAndRunListener(this::checkVis);
        omegaProtocol.getUsePantokrator().addAndRunListener(this::checkSMVis);
        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    @Override
    public KnownDuty getDuty() {
        return KnownDuty.None;
    }

    private void checkVis() {
        boolean enabled = omegaProtocol.getUseAutomarks().get();
        inner.setVisible(enabled);
    }

    private void checkSMVis() {
        boolean enabled = omegaProtocol.getUsePantokrator().get();
        innerPantokrator.setVisible(enabled);
    }
}
