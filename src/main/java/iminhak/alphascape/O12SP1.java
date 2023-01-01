package iminhak.alphascape;

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
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@CalloutRepo(name = "Iminha's O12SP1", duty = KnownDuty.O12S)
public class O12SP1 extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(O12SP1.class);

    private final ModifiableCallout<AbilityCastStart> supression = ModifiableCallout.durationBasedCall("Suppression", "East/West of Eye");
    private final ModifiableCallout<AbilityCastStart> syntheticShield = ModifiableCallout.durationBasedCall("Synthetic Shield", "Shield, spread soon");
    private final ModifiableCallout<AbilityCastStart> beyondDefense = ModifiableCallout.durationBasedCall("Beyond Defense", "Spread, stack soon");
    private final ModifiableCallout<AbilityUsedEvent> pilePitch = new ModifiableCallout<>("Pile Pitch", "Stack");
    private final ModifiableCallout<AbilityUsedEvent> beyondDefenseYou = new ModifiableCallout<>("Beyond Defense on you", "Stay spread");
    private final ModifiableCallout<AbilityCastStart> advancedSuppression = ModifiableCallout.durationBasedCall("Advanced Suppression", "Away from Eye");
    private final ModifiableCallout<AbilityCastStart> optimizedFire3soon = ModifiableCallout.durationBasedCall("Optimized Fire III soon", "Spread soon");
    private final ModifiableCallout<AbilityCastStart> optimizedFire3 = ModifiableCallout.durationBasedCall("Optimized Fire III", "Spread");
    private final ModifiableCallout<AbilityCastStart> subjectSimulationF = ModifiableCallout.durationBasedCall("Subject Simulation F", "Knockback soon");
    private final ModifiableCallout<AbilityCastStart> subjectSimulationM = ModifiableCallout.durationBasedCall("Subject Simulation M", "Light parties, knockback soon, away from M");
    private final ModifiableCallout<AbilityCastStart> firewall = ModifiableCallout.durationBasedCall("Firewall", "Check debuff");
    private final ModifiableCallout<AbilityCastStart> fundamentalSynergySwap = ModifiableCallout.durationBasedCall("Fundamental Synergy: Swap", "Swap");
    private final ModifiableCallout<AbilityCastStart> laserShower = ModifiableCallout.durationBasedCall("Laser Shower", "Raidwide");
    private final ModifiableCallout<AbilityCastStart> solarRay = ModifiableCallout.durationBasedCall("Solar Ray", "Double buster");
    private final ModifiableCallout<AbilityCastStart> syntheticShield2 = ModifiableCallout.durationBasedCall("Synthetic Shield 2: Start", "Intercardinals soon");
    private final ModifiableCallout<AbilityCastStart> beyondStrength = ModifiableCallout.durationBasedCall("Beyond Strength", "Inside M");
    private final ModifiableCallout<AbilityCastStart> efficientBladeworks = ModifiableCallout.durationBasedCall("Efficient Bladeworks", "Out");
    private final ModifiableCallout<AbilityCastStart> syntheticBlades = ModifiableCallout.durationBasedCall("Synthetic Blades 2: Start", "Under F line");
    private final ModifiableCallout<AbilityCastStart> synBlOutDonut = ModifiableCallout.durationBasedCall("Synthetic Blades 2: Blizzard + Donut", "Under M");
    private final ModifiableCallout<AbilityCastStart> superliminalMotion = ModifiableCallout.durationBasedCall("Superliminal Motion", "Behind F");
    private final ModifiableCallout<AbilityCastStart> optimizedFire3out = ModifiableCallout.durationBasedCall("Synthetic Blades 2: Fire + Out", "Spread, out");
    private final ModifiableCallout<AbilityCastStart> cosmoMemory = ModifiableCallout.durationBasedCall("Cosmo Memory", "Heavy raidwide");

    private final ModifiableCallout<BuffApplied> packetFilterM = ModifiableCallout.<BuffApplied>durationBasedCall("Packet Filter M", "Attack F").autoIcon();
    private final ModifiableCallout<BuffApplied> packetFilterF = ModifiableCallout.<BuffApplied>durationBasedCall("Packet Filter F", "Attack M").autoIcon();

    private final ModifiableCallout<BuffApplied> localResonance = ModifiableCallout.<BuffApplied>durationBasedCall("Local Resonance", "Split adds").autoIcon();
    private final ModifiableCallout<BuffApplied> remoteResonance = ModifiableCallout.<BuffApplied>durationBasedCall("Remote Resonance", "Stack adds").autoIcon();

    private final ModifiableCallout<HeadMarkerEvent> blue1 = new ModifiableCallout<>("Blue #1", "Blue 1");
    private final ModifiableCallout<HeadMarkerEvent> blue2 = new ModifiableCallout<>("Blue #2", "Blue 2");
    private final ModifiableCallout<HeadMarkerEvent> blue3 = new ModifiableCallout<>("Blue #3", "Blue 3");
    private final ModifiableCallout<HeadMarkerEvent> blue4 = new ModifiableCallout<>("Blue #4", "Blue 4");
    private final ModifiableCallout<HeadMarkerEvent> purp1 = new ModifiableCallout<>("Purple #1", "Purple 1");
    private final ModifiableCallout<HeadMarkerEvent> purp2 = new ModifiableCallout<>("Purple #2", "Purple 2");
    private final ModifiableCallout<HeadMarkerEvent> purp3 = new ModifiableCallout<>("Purple #3", "Purple 3");
    private final ModifiableCallout<HeadMarkerEvent> purp4 = new ModifiableCallout<>("Purple #4", "Purple 4");
    private final ModifiableCallout<HeadMarkerEvent> HMerror = new ModifiableCallout<>("Head Marker error", "Head Marker Error");

    private final XivState state;
    private final StatusEffectRepository buffs;
    private final RepeatSuppressor refire = new RepeatSuppressor(Duration.ofSeconds(1));

    public O12SP1(XivState state, StatusEffectRepository buffs) {
        this.state = state;
        this.buffs = buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.O12S);
    }

    private XivState getState() {
        return this.state;
    }

    private StatusEffectRepository getBuffs() {
        return this.buffs;
    }

    @HandleEvents
    public void abilityCast(EventContext context, AbilityCastStart event) {
        int id = (int) event.getAbility().getId();
        final ModifiableCallout<AbilityCastStart> call;
        switch (id) {
            case 0x3345 -> call = supression;
            case 0x3349 -> call = advancedSuppression;
            case 0x32F1 -> call = subjectSimulationF;
            case 0x32F4 -> call = subjectSimulationM;
            case 0x3338/*, 0x3339*/ -> call = firewall;
            case 0x3353/*, 0x3352*/ -> call = laserShower;
            case 0x3351/*, 0x3350*/ -> call = solarRay;
            case 0x3343/*,0x3342*/ -> call = cosmoMemory;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @HandleEvents
    public void buffApplied(EventContext context, BuffApplied event) {
        if(!event.getTarget().isThePlayer())
            return;
        int id = (int) event.getBuff().getId();
        final ModifiableCallout<BuffApplied> call;
        switch (id) {
            case 0x67D -> call = packetFilterF;
            case 0x67C -> call = packetFilterM;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @HandleEvents
    public void buffAppliedEnemy(EventContext context, BuffApplied event) {
        if(event.getTarget().getType() != CombatantType.NPC)
            return;
        if(refire.check(event)) {
            int id = (int) event.getBuff().getId();
            final ModifiableCallout<BuffApplied> call;
            switch (id) {
                case 0x67E -> call = localResonance;
                case 0x67F -> call = remoteResonance;
                default -> {
                    return;
                }
            }
            context.accept(call.getModified(event));
        }
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> syntheticShieldSq = SqtTemplates.multiInvocation(40_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x32FD),
            this::shield1,
            this::shield2);

    //Start of fight with suppression laser
    private void shield1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(syntheticShield.getModified());
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x332B));
        s.accept(beyondDefense.getModified());
        AbilityUsedEvent beyondDefenseAUE = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x332C));
        if(beyondDefenseAUE.getTarget().isThePlayer()) {
            s.accept(beyondDefenseYou.getModified());
        } else {
            s.accept(pilePitch.getModified());
        }
    }

    //After either Fundamental Synergy
    private void shield2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(syntheticShield2.getModified());
        //Beyond defense ACS
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x332B));
        s.accept(beyondDefense.getModified());
        //Beyond defense AUE
        AbilityUsedEvent beyondDefenseAUE = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x332C));
        if(beyondDefenseAUE.getTarget().isThePlayer()) {
            s.accept(beyondDefenseYou.getModified());
        } else {
            s.accept(pilePitch.getModified());
        }
        //Fire 3 AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x332E));
        s.accept(optimizedFire3.getModified());
        //Beyond Strength ACS
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x3328));
        s.accept(beyondStrength.getModified());
        //Beyond Strength AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3328));
        s.accept(efficientBladeworks.getModified());
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> syntheticBladesSq = SqtTemplates.multiInvocation(40_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x3301),
            this::blades1,
            this::blades2);

    private void blades1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(superliminalMotion.getModified());
        //Superliminal Motion ACS
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x3334));
        s.accept(optimizedFire3soon.getModified());
        //Superliminal Motion AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3334));
        s.accept(optimizedFire3.getModified());
    }

    //After either Fundamental Synergy
    private void blades2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(syntheticBlades.getModified());
        //Superliminal Steel AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x332F));
        s.accept(synBlOutDonut.getModified());
        //Pile pitch AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x332D));
        s.accept(superliminalMotion.getModified());
        //Superliminal Motion AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3334));
        s.accept(optimizedFire3out.getModified());
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> fundamentalSynergy = SqtTemplates.sq(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x333D),
            (e1, s) -> {
                log.info("Fundamental Synergy: Start");
                List<Integer> headMarkerIds = new ArrayList<>();
                headMarkerIds.add(0x91);
                headMarkerIds.add(0x92);
                headMarkerIds.add(0x93);
                headMarkerIds.add(0x94);
                headMarkerIds.add(0x95);
                headMarkerIds.add(0x96);
                headMarkerIds.add(0x97);
                headMarkerIds.add(0x98);
                List<HeadMarkerEvent> headMarkerEvents = s.waitEvents(8, HeadMarkerEvent.class, hme -> headMarkerIds.contains((int) hme.getMarkerId()));
                Optional<HeadMarkerEvent> HMplayer = headMarkerEvents.stream().filter(hme -> hme.getTarget().isThePlayer()).findAny();
                if(HMplayer.isPresent()) {
                    int HMid = (int) HMplayer.get().getMarkerId();
                    log.info("Fundamental Synergy: HM id: {}", HMid);
                    ModifiableCallout<HeadMarkerEvent> call1;
                    Optional<XivCombatant> swapBuddy;
                    switch (HMid) {
                        case 0x91 -> {
                            call1 = blue1;
                            swapBuddy = headMarkerEvents.stream().filter(hme -> hme.getMarkerId() == 0x91).map(HeadMarkerEvent::getTarget).findAny();
                        }
                        case 0x92 -> {
                            call1 = blue2;
                            swapBuddy = headMarkerEvents.stream().filter(hme -> hme.getMarkerId() == 0x92).map(HeadMarkerEvent::getTarget).findAny();
                        }
                        case 0x93 -> {
                            call1 = blue3;
                            swapBuddy = headMarkerEvents.stream().filter(hme -> hme.getMarkerId() == 0x91).map(HeadMarkerEvent::getTarget).findAny();
                        }
                        case 0x94 -> {
                            call1 = blue4;
                            swapBuddy = headMarkerEvents.stream().filter(hme -> hme.getMarkerId() == 0x92).map(HeadMarkerEvent::getTarget).findAny();
                        }
                        case 0x95 -> {
                            call1 = purp1;
                            swapBuddy = headMarkerEvents.stream().filter(hme -> hme.getMarkerId() == 0x95).map(HeadMarkerEvent::getTarget).findAny();
                        }
                        case 0x96 -> {
                            call1 = purp2;
                            swapBuddy = headMarkerEvents.stream().filter(hme -> hme.getMarkerId() == 0x96).map(HeadMarkerEvent::getTarget).findAny();
                        }
                        case 0x97 -> {
                            call1 = purp3;
                            swapBuddy = headMarkerEvents.stream().filter(hme -> hme.getMarkerId() == 0x95).map(HeadMarkerEvent::getTarget).findAny();
                        }
                        case 0x98 -> {
                            call1 = purp4;
                            swapBuddy = headMarkerEvents.stream().filter(hme -> hme.getMarkerId() == 0x96).map(HeadMarkerEvent::getTarget).findAny();
                        }
                        default -> {
                            call1 = HMerror;
                            swapBuddy = Optional.<XivCombatant>empty();
                        }
                    }
                    s.accept(call1.getModified());
                    if(swapBuddy.isPresent()) {
                        log.info("Fundamental Synergy: Swap buddy is: {}", swapBuddy.get().getName());
                        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x333E, 0x333F) && aue.getTarget() == swapBuddy.get());
                        s.accept(fundamentalSynergySwap.getModified());
                        //333E/F electric slide
                    } else {
                        log.info("Fundamental Synergy: Couldn't find buddy: {}", swapBuddy);
                    }
                } else {
                    log.info("Fundamental Synergy: Couldn't find HM: {}", HMplayer);
                }
            });
}
