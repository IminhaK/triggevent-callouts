package iminhak.ultimate;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.OverridesCalloutGroupEnabledSetting;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerHeadmarker;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "Iminha's Day Zero and Bleeding Edge TOP Triggers", duty = KnownDuty.None)
public class TOPDayZeroAndBleedingEdge extends AutoChildEventHandler implements FilteredEventHandler, OverridesCalloutGroupEnabledSetting {
    private static final Logger log = LoggerFactory.getLogger(TOPDayZeroAndBleedingEdge.class);

    //Phase 1: Omega
    @NpcCastCallout(0x0)
    private final ModifiableCallout<AbilityCastStart> programLoop = ModifiableCallout.durationBasedCall("Program Loop", "Check debuff");
    @PlayerHeadmarker(value = 0, offset = true) //TODO: Program loop headmarker IDs
    private final ModifiableCallout<HeadMarkerEvent> programLoop1 = new ModifiableCallout<>("Program Loop: 1", "1", 20_000);

    //Debuffs, from Locrian Mode (https://cdn.discordapp.com/attachments/1067362348798574602/1067362349041856553/Preliminary_TOP_status_triggers.xml)
    private final ModifiableCallout<BuffApplied> cascadingLatentDefect = ModifiableCallout.durationBasedCall("Cascading Latent Defect", "Get Rot");
    private final ModifiableCallout<BuffApplied> waveCannonKyrios = ModifiableCallout.durationBasedCall("Condensed Wave Cannon Kyrios", "Laser soon");
    private final ModifiableCallout<BuffApplied> criticalOverflowBug = ModifiableCallout.durationBasedCall("Critical Overflow Bug", "Defamation");
    private final ModifiableCallout<BuffApplied> criticalSynchronizationBug = ModifiableCallout.durationBasedCall("Critical Synchronization Bug", "Stack");
    private final ModifiableCallout<BuffApplied> criticalUnderflowBug = ModifiableCallout.durationBasedCall("Critical Underflow Bug", "Rot");
    private final ModifiableCallout<BuffApplied> guidedMissileKyrios = ModifiableCallout.durationBasedCall("Guided Missile Kyrios", "Light pillar soon");
    private final ModifiableCallout<BuffApplied> highPoweredSniperCannon = ModifiableCallout.durationBasedCall("High-powered Sniper Cannon", "Super sniper soon");
    private final ModifiableCallout<BuffApplied> latentDefect = ModifiableCallout.durationBasedCall("Latent Defect", "Get defamation");
    private final ModifiableCallout<BuffApplied> latentSynchronizationBug = ModifiableCallout.durationBasedCall("Latent Synchronization Bug", "Stack");
    private final ModifiableCallout<BuffApplied> localCodeSmell = ModifiableCallout.durationBasedCall("Local Code Smell", "Christmas tether soon");
    private final ModifiableCallout<BuffApplied> localRegression = ModifiableCallout.durationBasedCall("Local Regression", "Christmas tether");
    private final ModifiableCallout<BuffApplied> overflowCodeSmell = ModifiableCallout.durationBasedCall("Overflow Code Smell", "Defamation soon");
    private final ModifiableCallout<BuffApplied> packetFilterF = ModifiableCallout.durationBasedCall("Packet Filter F", "Attack M");
    private final ModifiableCallout<BuffApplied> packetFilterM = ModifiableCallout.durationBasedCall("Packet Filter M", "Attack F");
    private final ModifiableCallout<BuffApplied> remoteCodeSmell = ModifiableCallout.durationBasedCall("Remote Code Smell", "Blue tether soon");
    private final ModifiableCallout<BuffApplied> remoteRegression = ModifiableCallout.durationBasedCall("Remote Regression", "Blue tether");
    private final ModifiableCallout<BuffApplied> sniperCannon = ModifiableCallout.durationBasedCall("Sniper Cannon", "Snper soon");
    private final ModifiableCallout<BuffApplied> synchronizationCodeSmell = ModifiableCallout.durationBasedCall("Synchronization Code Smell", "Stack soon");
    private final ModifiableCallout<BuffApplied> underflowCodeSmell = ModifiableCallout.durationBasedCall("Underflow Code Smell", "Rot soon");

    public TOPDayZeroAndBleedingEdge(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
        this.state = state;
        this.buffs = buffs;
        this.enabled = new BooleanSetting(pers, "triggers.top.triggers.enabled", false);
    }

    private final XivState state;
    private final StatusEffectRepository buffs;
    private final BooleanSetting enabled;

    private XivState getState() {
        return this.state;
    }

    private StatusEffectRepository getBuffs() {
        return this.buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return enabled.get();
    }

    @Override
    public BooleanSetting getCalloutGroupEnabledSetting() {
        return enabled;
    }

    @HandleEvents
    public void buffApplied(EventContext context, BuffApplied event) {
        int id = (int) event.getBuff().getId();
        final ModifiableCallout<BuffApplied> call;
        if(!event.getTarget().isThePlayer())
            return;
        switch (id) {
            case 0xDC8 -> call = cascadingLatentDefect;
            case 0xDB3, 0xDB4, 0xDB5, 0xDB6 -> call = waveCannonKyrios;
            case 0xDC5 -> call = criticalOverflowBug;
            case 0xDC4 -> call = criticalSynchronizationBug;
            case 0xDC6 -> call = criticalUnderflowBug;
            case 0xD60, 0xDA7, 0xDA8, 0xDA9 -> call = guidedMissileKyrios;
            case 0xD62 -> call = highPoweredSniperCannon;
            case 0xDC7 -> call = latentDefect;
            case 0xD6A -> call = latentSynchronizationBug;
            case 0xD70, 0xDAF -> call = localCodeSmell;
            case 0xDC9 -> call = localRegression;
            case 0xD6D -> call = overflowCodeSmell;
            case 0xDAC -> call = packetFilterF;
            case 0xDAB -> call = packetFilterM;
            case 0xD71, 0xDB0 -> call = remoteCodeSmell;
            case 0xDCA -> call = remoteRegression;
            case 0xD61 -> call = sniperCannon;
            case 0xD6C -> call = synchronizationCodeSmell;
            case 0xD6E -> call = underflowCodeSmell;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }
}
