package iminhak.ultimate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyRecommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@CalloutRepo(name = "Iminha's Omega Protocol", duty = KnownDuty.None)
public class OmegaProtocol extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OmegaProtocol.class);

    private final BooleanSetting useAutomarks;

    private final BooleanSetting useCircleProgram;

    //Phase 1: Omega
    private final ModifiableCallout<BuffApplied> circleProgram_mustard1 = new ModifiableCallout<BuffApplied>("Circle program: First  Mustard", "One, take tether").autoIcon();
    private final ModifiableCallout<BuffApplied> circleProgram_mustard2 = new ModifiableCallout<BuffApplied>("Circle program: Second  Mustard", "Two, tether later").autoIcon();
    private final ModifiableCallout<BuffApplied> circleProgram_patch1 = new ModifiableCallout<BuffApplied>("Circle program: First  Patch", "One, take tower").autoIcon();
    private final ModifiableCallout<BuffApplied> circleProgram_patch2 = new ModifiableCallout<BuffApplied>("Circle Program: Second  Patch", "Two, tower later").autoIcon();
    private final ModifiableCallout<AbilityCastStart> circleProgram_takeMustard = new ModifiableCallout<AbilityCastStart>("Circle Program: Take tether", "Take tether");

    public OmegaProtocol(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
        this.state = state;
        this.buffs = buffs;
        this.useAutomarks = new BooleanSetting(pers, "triggers.top.use-auto-markers", false);

        this.useCircleProgram = new BooleanSetting(pers, "triggers.top.use-something", false);
    }

    private final XivState state;
    private final StatusEffectRepository buffs;

    private XivState getState() {
        return this.state;
    }

    private StatusEffectRepository getBuffs() {
        return this.buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.None);
    }

    @HandleEvents
    public void reset(EventContext context, DutyRecommenceEvent drce) {
        context.accept(new ClearAutoMarkRequest());
    }

    private final Predicate<BuffApplied> circleProgramNumber = ba -> {
        long id = ba.getBuff().getId();
        return id >= 0x0 && id <= 0x0; //TODO: Circle Program dive numbers, low then high ID
    };

    @AutoFeed
    private final SequentialTrigger<BaseEvent> circleProgram = SqtTemplates.sq(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x0), //TODO: Insert correct ID for Circle Program cast
            (e1, s) -> {
                Boolean useThisAM = getUseCircleProgram().get() && getUseAutomarks().get();
                log.info("Circle Program: start");
                List<BuffApplied> lineDebuffs = s.waitEvents(8, BuffApplied.class, circleProgramNumber); //TODO: Confirm number of line debuffs applied
                Optional<BuffApplied> lineDebuffOnPlayer = lineDebuffs.stream().filter(ba -> ba.getTarget().isThePlayer()).findFirst();
                if(lineDebuffOnPlayer.isPresent()) {
                    int linePos = (int) lineDebuffOnPlayer.get().getBuff().getId() - 0x0 + 1; //TODO: Insert line buff offset (first buff minus one)
                    boolean inTower = getBuffs().getBuffs().stream().anyMatch(ba -> ba.getBuff().getId() == 0x0 && ba.getTarget().isThePlayer()); //TODO: Get Patch ID
                    switch(linePos) {
                        case 1 -> s.accept(inTower ? circleProgram_patch1.getModified() : circleProgram_mustard1.getModified());
                        case 2 -> s.accept(inTower ? circleProgram_patch2.getModified() : circleProgram_mustard2.getModified());
                    }

                    s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x0)); //TODO: Mustard bomb cast ID

                    //TODO: If only 4 line debuffs, create prio for who takes first/second mustards
                    if(!inTower) {
                        s.accept(circleProgram_takeMustard.getModified());
                    }
                }
            });

    @AutoFeed
    private final SequentialTrigger<BaseEvent> someMechanicThatIsCaseMultipleTimes = SqtTemplates.multiInvocation(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x0),
            this::firstUse,
            this::secondUse);

    private void firstUse(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {

    }

    private void secondUse(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {

    }

    public BooleanSetting getUseAutomarks() {
        return useAutomarks;
    }

    public BooleanSetting getUseCircleProgram() {
        return useCircleProgram;
    }
}
