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

    private final OmegaProtocol omegaProtocol;
    private JPanel inner;
    private JPanel innerDummyMechanic;

    private final GlobalUiRegistry reg;

    public OmegaProtocolGUI(OmegaProtocol omegaProtocol, GlobalUiRegistry reg) {
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
        innerDummyMechanic = new JPanel();
        innerDummyMechanic.setLayout(new GridBagLayout());
        JCheckBox useSomeMechanic = new BooleanSettingGui(omegaProtocol.getUseCircleProgram(), "Use 'Some mechanic' markers").getComponent();
        ReadOnlyText text = new ReadOnlyText("""
                Here I would explain what each checkbox does, but the fight isnt out yet so I don't even know
                """);

        inner.add(useSomeMechanic, c);
        c.gridy++;
        c.weighty++;
        inner.add(text, c);

        omegaProtocol.getUseAutomarks().addAndRunListener(this::checkVis);
        omegaProtocol.getUseCircleProgram().addAndRunListener(this::checkSMVis);
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
        boolean enabled = omegaProtocol.getUseCircleProgram().get();
        innerDummyMechanic.setVisible(enabled);
    }
}
