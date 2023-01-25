package iminhak.ultimate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.OverridesCalloutGroupEnabledSetting;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyRecommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

@CalloutRepo(name = "Iminha's Day Zero and Bleeding Edge TOP Triggers", duty = KnownDuty.None)
public class TOPDayZeroAndBleedingEdge extends AutoChildEventHandler implements FilteredEventHandler, OverridesCalloutGroupEnabledSetting {
    private static final Logger log = LoggerFactory.getLogger(TOPDayZeroAndBleedingEdge.class);

    //Phase 1: Omega
    //Program Loop
    private final ModifiableCallout<?> firstInLineTower = new ModifiableCallout<>("Loop First: Tower", "One, take tower");
    private final ModifiableCallout<?> firstInLineTether = new ModifiableCallout<>("Loop First: Tether", "Take tether");
    private final ModifiableCallout<?> secondInLineLoop = new ModifiableCallout<>("Loop Second", "Two");
    private final ModifiableCallout<?> secondInLineTower = new ModifiableCallout<>("Loop Second: Tower", "Take tower");
    private final ModifiableCallout<?> secondInLineTether = new ModifiableCallout<>("Loop Second: Tether", "Take tether");
    private final ModifiableCallout<?> thirdInLineTower = new ModifiableCallout<>("Loop Third: Tower", "Take tower");
    private final ModifiableCallout<?> thirdInLineTether = new ModifiableCallout<>("Loop Third: Tether", "Three, Take tether");
    private final ModifiableCallout<?> fourthInLineLoop = new ModifiableCallout<>("Loop Fourth", "Four");
    private final ModifiableCallout<?> fourthInLineTower = new ModifiableCallout<>("Loop Fourth: Tower", "Take tower");
    private final ModifiableCallout<?> fourthInLineTether = new ModifiableCallout<>("Loop Fourth: Tether", "Take tether");
    //Pantokrator
    private final ModifiableCallout<?> firstInLineOut = new ModifiableCallout<>("Panto First: Out", "One, Move out");
    private final ModifiableCallout<?> firstInLineStack = new ModifiableCallout<>("Panto First: Stack", "Stack");
    private final ModifiableCallout<?> secondInLinePanto = new ModifiableCallout<>("Panto Second", "Two, start stacked");
    private final ModifiableCallout<?> secondInLineOut = new ModifiableCallout<>("Panto Second: Out", "Move out");
    private final ModifiableCallout<?> secondInLineStack = new ModifiableCallout<>("Panto Second: Stack", "Stack");
    private final ModifiableCallout<?> thirdInLinePanto = new ModifiableCallout<>("Panto Third", "Three, start stacked");
    private final ModifiableCallout<?> thirdInLineOut = new ModifiableCallout<>("Panto Third: Out", "Move out");
    private final ModifiableCallout<?> thirdInLineStack = new ModifiableCallout<>("Panto Third: Stack", "Stack");
    private final ModifiableCallout<?> fourthInLinePanto = new ModifiableCallout<>("Panto Fourth", "Four, start stacked");
    private final ModifiableCallout<?> fourthInLineOut = new ModifiableCallout<>("Panto Fourth: Out", "Move out");
    private final ModifiableCallout<?> pantoLasers = new ModifiableCallout<>("Pantokrator: Lasers", "Laser baits");
    private final ModifiableCallout<?> pantoLaserYou = new ModifiableCallout<>("Pantokrator: Laser you", "Laser on YOU");

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
        this.useAutomarks = new BooleanSetting(pers, "triggers.top.use-auto-markers", false);
        this.usePantokrator = new BooleanSetting(pers, "triggers.top.use-something", false);
        sortSetting = new JobSortSetting(pers, "triggers.top.job-prio", state);
    }

    private final XivState state;
    private final StatusEffectRepository buffs;
    private final BooleanSetting enabled;
    private boolean autoMarking = false;
    private final BooleanSetting useAutomarks;
    private final BooleanSetting usePantokrator;
    private final JobSortSetting sortSetting;

    private XivState getState() {
        return this.state;
    }

    private StatusEffectRepository getBuffs() {
        return this.buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return true;
//        return enabled.get();
    }

    @Override
    public BooleanSetting getCalloutGroupEnabledSetting() {
        return enabled;
    }

    @HandleEvents
    public void reset(EventContext conext, DutyRecommenceEvent event) {
        if(autoMarking) {
            conext.accept(new ClearAutoMarkRequest());
        }
    }

//    @HandleEvents
//    public void buffApplied(EventContext context, BuffApplied event) {
//        int id = (int) event.getBuff().getId();
//        final ModifiableCallout<BuffApplied> call;
//        if(!event.getTarget().isThePlayer())
//            return;
//        switch (id) {
//            case 0xDC8 -> call = cascadingLatentDefect;
//            case 0xDB3, 0xDB4, 0xDB5, 0xDB6 -> call = waveCannonKyrios;
//            case 0xDC5 -> call = criticalOverflowBug;
//            case 0xDC4 -> call = criticalSynchronizationBug;
//            case 0xDC6 -> call = criticalUnderflowBug;
//            case 0xD60, 0xDA7, 0xDA8, 0xDA9 -> call = guidedMissileKyrios;
//            case 0xD62 -> call = highPoweredSniperCannon;
//            case 0xDC7 -> call = latentDefect;
//            case 0xD6A -> call = latentSynchronizationBug;
//            case 0xD70, 0xDAF -> call = localCodeSmell;
//            case 0xDC9 -> call = localRegression;
//            case 0xD6D -> call = overflowCodeSmell;
//            case 0xDAC -> call = packetFilterF;
//            case 0xDAB -> call = packetFilterM;
//            case 0xD71, 0xDB0 -> call = remoteCodeSmell;
//            case 0xDCA -> call = remoteRegression;
//            case 0xD61 -> call = sniperCannon;
//            case 0xD6C -> call = synchronizationCodeSmell;
//            case 0xD6E -> call = underflowCodeSmell;
//            default -> {
//                return;
//            }
//        }
//        context.accept(call.getModified(event));
//    }

    private static boolean lineDebuff(BuffApplied ba) {
        int id = (int) ba.getBuff().getId();
        return id == 0xBBC || id == 0xBBD || id == 0xBBE || id == 0xD7B;
    }

    private enum NumberInLine {
        FIRST,
        SECOND,
        THIRD,
        FOURTH,
        UNKNOWN
    }

    private static NumberInLine lineFromDebuff(BuffApplied ba) {
        int id = (int) ba.getBuff().getId();
        log.info("NumberInLine: Buff id is {}", id);
        return switch (id) {
            case 0xBBC -> NumberInLine.FIRST;
            case 0xBBD -> NumberInLine.SECOND;
            case 0xBBE -> NumberInLine.THIRD;
            case 0xD7B -> NumberInLine.FOURTH;
            default -> NumberInLine.UNKNOWN;
        };
    }

    //returns first player with line debuff
    private XivPlayerCharacter supPlayerFromLine(NumberInLine il) {
        List<XivPlayerCharacter> players = getState().getPartyList().stream().filter(p -> p.getJob().isTank() || p.getJob().isHealer()).toList();
        int lineId = switch(il) {
            case FIRST -> 0xBBC;
            case SECOND -> 0xBBD;
            case THIRD -> 0xBBE;
            case FOURTH -> 0xD7B;
            default -> 0x0;
        };

        XivPlayerCharacter match = null;
        for(XivPlayerCharacter p : players) {
            List<BuffApplied> buffs = getBuffs().statusesOnTarget(p);
            if (buffs.stream().anyMatch(b -> b.buffIdMatches(lineId))) {
                match = p;
                break;
            }
        }

        return match;
    }

    //returns first player with line debuff
    private XivPlayerCharacter dpsPlayerFromLine(NumberInLine il) {
        List<XivPlayerCharacter> players = getState().getPartyList().stream().filter(p -> p.getJob().isDps()).toList();
        int lineId = switch(il) {
            case FIRST -> 0xBBC;
            case SECOND -> 0xBBD;
            case THIRD -> 0xBBE;
            case FOURTH -> 0xD7B;
            default -> 0x0;
        };

        XivPlayerCharacter match = null;
        for(XivPlayerCharacter p : players) {
            List<BuffApplied> buffs = getBuffs().statusesOnTarget(p);
            if (buffs.stream().anyMatch(b -> b.buffIdMatches(lineId))) {
                match = p;
                break;
            }
        }

        return match;
    }

    @AutoFeed
    public SequentialTrigger<BaseEvent> programLoopSq = SqtTemplates.sq(50_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x7B03),
            (e1, s) -> {
                log.info("Program Loop: Start");
                BuffApplied lineDebuff = s.waitEvent(BuffApplied.class, ba -> lineDebuff(ba) && ba.getTarget().isThePlayer());
                NumberInLine num = lineFromDebuff(lineDebuff);
                if(num == NumberInLine.FIRST) {
                    s.accept(firstInLineTower.getModified());
                } else if(num == NumberInLine.THIRD) {
                    s.accept(thirdInLineTether.getModified());
                } else if(num == NumberInLine.SECOND) {
                    s.accept(secondInLineLoop.getModified());
                } else if(num == NumberInLine.FOURTH){
                    s.accept(fourthInLineLoop.getModified());
                }

                //First tower goes off
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04));
                if(num == NumberInLine.SECOND) {
                    s.accept(secondInLineTower.getModified());
                } else if(num == NumberInLine.FOURTH) {
                    s.accept(fourthInLineTether.getModified());
                }

                //Second tower goes off
                s.waitMs(100);
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04));
                if(num == NumberInLine.THIRD) {
                    s.accept(thirdInLineTower.getModified());
                } else if(num == NumberInLine.FIRST) {
                    s.accept(firstInLineTether.getModified());
                }

                //Third tower goes off
                s.waitMs(100);
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04));
                if(num == NumberInLine.FOURTH) {
                    s.accept(fourthInLineTower.getModified());
                } else if(num == NumberInLine.SECOND) {
                    s.accept(secondInLineTether.getModified());
                }
            });

    //AM
//    @AutoFeed
//    public SequentialTrigger<BaseEvent> pantokratorAm = SqtTemplates.sq(50_000, AbilityCastStart.class,
//            acs -> acs.abilityIdMatches(0x7B0B),
//            (e1, s) -> {
//                if(getUseAutomarks().get() && getUsePantokrator().get()) {
//                    autoMarking = true;
//                    List<BuffApplied> buffs = s.waitEventsQuickSuccession(8, BuffApplied.class, TOPDayZeroAndBleedingEdge::lineDebuff, Duration.ofMillis(200));
//                    List<NumberInLine> supBuffs = buffs.stream().filter(ba -> {
//                        Job j = ((XivPlayerCharacter) ba.getTarget()).getJob();
//                        return j.isTank() || j.isHealer();
//                    }).map(TOPDayZeroAndBleedingEdge::lineFromDebuff).toList();
//                    List<NumberInLine> dpsBuffs = buffs.stream().filter(ba -> {
//                        Job j = ((XivPlayerCharacter) ba.getTarget()).getJob();
//                        return j.isDps();
//                    }).map(TOPDayZeroAndBleedingEdge::lineFromDebuff).toList();
//
//                    supBuffs = new ArrayList<>(supBuffs);
//                    dpsBuffs = new ArrayList<>(dpsBuffs);
//
//                    supBuffs.sort(Comparator.naturalOrder());
//                    dpsBuffs.sort(Comparator.naturalOrder());
//                    NumberInLine priorSup = null;
//                    for (NumberInLine n : supBuffs) {
//                        if (priorSup == n) {
//                            s.accept(new SpecificAutoMarkRequest(supPlayerFromLine(n), MarkerSign.BIND2));
//                            break;
//                        }
//                        priorSup = n;
//                    }
//
//                    NumberInLine priorDps = null;
//                    for (NumberInLine n : dpsBuffs) {
//                        if (priorDps == n) {
//                            s.accept(new SpecificAutoMarkRequest(dpsPlayerFromLine(n), MarkerSign.BIND1));
//                            break;
//                        }
//                        priorDps = n;
//                    }
//                }
//            });

    @AutoFeed
    public SequentialTrigger<BaseEvent> pantokratorAm = SqtTemplates.sq(50_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x7B0B),
            (e1, s) -> {
                if(getUseAutomarks().get() && getUsePantokrator().get()) {
                    List<BuffApplied> numbers = s.waitEventsQuickSuccession(8, BuffApplied.class, TOPDayZeroAndBleedingEdge::lineDebuff, Duration.ofMillis(200));
                    Map<NumberInLine, List<XivPlayerCharacter>> mechanics = new EnumMap<>(NumberInLine.class);
                    numbers.forEach(ba -> {
                        if (ba.getTarget() instanceof XivPlayerCharacter p) {
                            int id = (int) ba.getBuff().getId();
                            switch (id) {
                                case 0xBBC -> mechanics.computeIfAbsent(NumberInLine.FIRST, k -> new ArrayList<>()).add(p);
                                case 0xBBD -> mechanics.computeIfAbsent(NumberInLine.SECOND, k -> new ArrayList<>()).add(p);
                                case 0xBBE -> mechanics.computeIfAbsent(NumberInLine.THIRD, k -> new ArrayList<>()).add(p);
                                case 0xD7B -> mechanics.computeIfAbsent(NumberInLine.FOURTH, k -> new ArrayList<>()).add(p);
                            }
                        }
                    });
                    log.info("PantokratorAm: Mechanics: {}", mechanics);

                    Comparator<XivPlayerCharacter> jobSort = get_sortSetting().getPlayerJailSortComparator();
                    mechanics.values().forEach(list -> list.sort(jobSort));

                    List<XivPlayerCharacter> ones = mechanics.get(NumberInLine.FIRST);
                    List<XivPlayerCharacter> twos = mechanics.get(NumberInLine.SECOND);
                    List<XivPlayerCharacter> threes = mechanics.get(NumberInLine.THIRD);
                    List<XivPlayerCharacter> fours = mechanics.get(NumberInLine.FOURTH);

                    ///Confirm sizes in case someone was rezzing and missed debuff
                    //TODO: mark no matter how many ppl
                    if(ones.size() + twos.size() + threes.size() + fours.size() == 8) {
                        //g1
                        s.accept(new SpecificAutoMarkRequest(ones.get(0), MarkerSign.ATTACK_NEXT));
                        s.accept(new SpecificAutoMarkRequest(twos.get(0), MarkerSign.ATTACK_NEXT));
                        s.accept(new SpecificAutoMarkRequest(threes.get(0), MarkerSign.ATTACK_NEXT));
                        s.accept(new SpecificAutoMarkRequest(fours.get(0), MarkerSign.ATTACK_NEXT));

                        s.accept(new SpecificAutoMarkRequest(ones.get(1), MarkerSign.SQUARE));
                        s.accept(new SpecificAutoMarkRequest(twos.get(1), MarkerSign.CIRCLE));
                        s.accept(new SpecificAutoMarkRequest(threes.get(1), MarkerSign.TRIANGLE));
                        s.accept(new SpecificAutoMarkRequest(fours.get(1), MarkerSign.CROSS));
                    }
                }
            });

    @AutoFeed
    public SequentialTrigger<BaseEvent> pantokratorSq = SqtTemplates.sq(50_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x7B0B),
            (e1, s) -> {
                log.info("Pantokrator: Start");
                BuffApplied lineDebuff = s.waitEvent(BuffApplied.class, ba -> lineDebuff(ba) && ba.getTarget().isThePlayer());
                NumberInLine num = lineFromDebuff(lineDebuff);

                if(num == NumberInLine.FIRST) {
                    s.accept(firstInLineOut.getModified());
                } else if(num == NumberInLine.SECOND) {
                    s.accept(secondInLinePanto.getModified());
                } else if(num == NumberInLine.THIRD) {
                    s.accept(thirdInLinePanto.getModified());
                } else if(num == NumberInLine.FOURTH) {
                    s.accept(fourthInLinePanto.getModified());
                }

                //First laser
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B0F));
                if(num == NumberInLine.SECOND) {
                    s.accept(secondInLineOut.getModified());
                } else if (num == NumberInLine.FIRST) {
                    s.accept(firstInLineStack.getModified());
                }

                //Second laser
                s.waitMs(100);
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B0F));
                if(num == NumberInLine.THIRD) {
                    s.accept(thirdInLineOut.getModified());
                } else if(num == NumberInLine.SECOND) {
                    s.accept(secondInLineStack.getModified());
                }

                //Third laser
                s.waitMs(100);
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B0F));
                if(num == NumberInLine.FOURTH) {
                    s.accept(fourthInLineOut.getModified());
                } else if(num == NumberInLine.THIRD) {
                    s.accept(thirdInLineStack.getModified());
                }

                //Fourth laser
                s.waitMs(100);
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B0F));
                s.accept(pantoLasers.getModified());

                s.waitEvent(HeadMarkerEvent.class, hm -> hm.getMarkerId() == 0x84 && hm.getTarget().isThePlayer());
                s.accept(pantoLaserYou.getModified());
            });

    public BooleanSetting getUseAutomarks() {
        return useAutomarks;
    }

    public BooleanSetting getUsePantokrator() {
        return usePantokrator;
    }

    public JobSortSetting get_sortSetting() {
        return sortSetting;
    }
}
