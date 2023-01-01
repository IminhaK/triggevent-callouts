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
import gg.xp.xivsupport.events.actlines.events.*;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@CalloutRepo(name = "Iminha's O11S", duty = KnownDuty.O11S)
public class O11S extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(O11S.class);

    private final ModifiableCallout<AbilityCastStart> atomicRay = ModifiableCallout.durationBasedCall("Atomic Ray", "Raidwide");
    private final ModifiableCallout<AbilityCastStart> flameThrower = ModifiableCallout.durationBasedCall("Flame Thrower start", "Proteans");
    private final ModifiableCallout<AbilityCastStart> flameThrowerMove = ModifiableCallout.durationBasedCall("Flame Thrower move", "Move");
    private final ModifiableCallout<AbilityCastStart> starboard = ModifiableCallout.durationBasedCall("Starboard", "Left safe");
    private final ModifiableCallout<AbilityCastStart> larboard = ModifiableCallout.durationBasedCall("Larboard", "Right safe");
    private final ModifiableCallout<AbilityCastStart> LRstay = ModifiableCallout.durationBasedCall("Larb/Star stay", "Stay");
    private final ModifiableCallout<AbilityCastStart> LRswap = ModifiableCallout.durationBasedCall("larb/Star swap", "Move");
    private final ModifiableCallout<AbilityCastStart> peripheralSynthesis1 = ModifiableCallout.durationBasedCall("Peripheral Synthesis", "Spread, match soon");
    private final ModifiableCallout<AbilityCastStart> peripheralYellow = ModifiableCallout.durationBasedCall("Peripheral Synthesis: Yellow", "Yellow on you");
    private final ModifiableCallout<AbilityCastStart> peripheralBlue = ModifiableCallout.durationBasedCall("Peripheral Synthesis: Blue", "Blue on you");
    private final ModifiableCallout<AbilityCastStart> mustardBomb = ModifiableCallout.durationBasedCall("Mustard Bomb", "Buster on {event.target}");
    private final ModifiableCallout<AbilityCastStart> ferroPush = ModifiableCallout.durationBasedCall("Ferro same polarity", "Same, get close");
    private final ModifiableCallout<AbilityCastStart> ferroPull = ModifiableCallout.durationBasedCall("Ferro different polarity", "Different, go far");
    private final ModifiableCallout<AbilityCastStart> executable2start = ModifiableCallout.durationBasedCall("Executable 2: Start", "Bait tether or move east");
    private final ModifiableCallout<AbilityCastStart> executable2Chain = ModifiableCallout.durationBasedCall("Executable 2: Chains of Memory", "Chain on YOU");
    private final ModifiableCallout<AbilityCastStart> executable2Looper = ModifiableCallout.durationBasedCall("Executable 2: Looper", "Looper, {duration} seconds");
    private final ModifiableCallout<AbilityCastStart> executable2chainFirst = ModifiableCallout.durationBasedCall("Executable 2: Chain first rotate", "Rotate, let party into tower");
    private final ModifiableCallout<AbilityCastStart> executable2looperFirst = ModifiableCallout.durationBasedCall("Executable 2: First looper", "Wait for chain, take first tower");
    private final ModifiableCallout<AbilityCastStart> executable2looperSecond = ModifiableCallout.durationBasedCall("Executable 2: Second looper", "Take second tower");
    private final ModifiableCallout<AbilityCastStart> executable2looperThird = ModifiableCallout.durationBasedCall("Executable 2: Third looper", "Take third tower");
    private final ModifiableCallout<AbilityCastStart> executable2chainRotate = ModifiableCallout.durationBasedCall("Executable 2: Chain rotate CW", "Clockwise into tower");
    private final ModifiableCallout<AbilityCastStart> executable2chainRotateCCW = ModifiableCallout.durationBasedCall("Executable 2: Chain rotate CCW", "Counterclockwise into tower");
    private final ModifiableCallout<AbilityCastStart> reset = ModifiableCallout.durationBasedCall("Reset", "Out, then in");
    private final ModifiableCallout<AbilityCastStart> reformat = ModifiableCallout.durationBasedCall("Reformat", "In");
    private final ModifiableCallout<AbilityCastStart> forceQuit = ModifiableCallout.durationBasedCall("Force Quit", "Enrage");
    private final ModifiableCallout<AbilityCastStart> deltaAttack = ModifiableCallout.durationBasedCall("Delta Attack", "Tank LB");
    private final ModifiableCallout<AbilityCastStart> gradualPetrification = ModifiableCallout.durationBasedCall("Gradual Petrification", "Heal to full");
    private final ModifiableCallout<AbilityCastStart> flameThrower2 = ModifiableCallout.durationBasedCall("Flamethrower 2", "Proteans then baits");
    private final ModifiableCallout<AbilityCastStart> ballisticImpact = ModifiableCallout.durationBasedCall("Ballistic Impact", "Move in");
    private final ModifiableCallout<AbilityCastStart> blaster = ModifiableCallout.durationBasedCall("Blaster", "Tether buster");
    private final ModifiableCallout<AbilityCastStart> electricSlide = ModifiableCallout.durationBasedCall("Electric Slide", "Stack");
    private final ModifiableCallout<AbilityCastStart> starboardSurge = ModifiableCallout.durationBasedCall("Starboard Wave Canon Surge", "Left then right");
    private final ModifiableCallout<AbilityCastStart> larboardSurge = ModifiableCallout.durationBasedCall("Larboard Wave Canon Surge", "Right then left");
    private final ModifiableCallout<AbilityCastStart> flamethrowerDOT = ModifiableCallout.durationBasedCall("Flamethrower DOT", "Proteans with Bleed");
    private final ModifiableCallout<AbilityCastStart> pantokrator = ModifiableCallout.durationBasedCall("Pantokrator", "Stack and bait");
    private final ModifiableCallout<AbilityCastStart> pantokratorMove = ModifiableCallout.durationBasedCall("Patokrator: Move", "Move");
    private final ModifiableCallout<AbilityCastStart> engageBallistic = ModifiableCallout.durationBasedCall("Engage Ballistic Systems", "Stack");
    private final ModifiableCallout<AbilityCastStart> pantokratorBait = ModifiableCallout.durationBasedCall("Pantokrator: Lasers 1", "First bait");
    private final ModifiableCallout<AbilityCastStart> pantokratorBait2 = ModifiableCallout.durationBasedCall("Pantokrator: Lasers 2", "Avoid puddles, second bait");
    private final ModifiableCallout<AbilityCastStart> longNeedleKyrios = ModifiableCallout.durationBasedCall("Long Needle Kyrios", "Move to corner");
    private final ModifiableCallout<AbilityCastStart> loop = ModifiableCallout.durationBasedCall("Loop", "Enrage");
    private final ModifiableCallout<AbilityCastStart> peripheralSynthesis2 = ModifiableCallout.durationBasedCall("Peripheral Synthesis 2/3: Start", "Rocket tethers");
    private final ModifiableCallout<AbilityCastStart> sprint = ModifiableCallout.durationBasedCall("Peripheral Synthesis 3: Sprint", "Sprint");
    private final ModifiableCallout<AbilityCastStart> peripheralSynthesis4 = ModifiableCallout.durationBasedCall("Peripheral Synthesis 4: Start", "Stack fists");
    private final ModifiableCallout<AbilityCastStart> peripheral4LB = ModifiableCallout.durationBasedCall("Peripheral Synthesis 4: Tank LB", "Tank LB!");
    private final ModifiableCallout<AbilityCastStart> pantokrator2 = ModifiableCallout.durationBasedCall("Pantokrator 2: Start", "Stack and bait, ranged player far");
    private final ModifiableCallout<AbilityCastStart> pantokrator2tank = ModifiableCallout.durationBasedCall("Pantokrator 2: Buster", "Busters soon");
    private final ModifiableCallout<AbilityCastStart> pantokrator2laser = ModifiableCallout.durationBasedCall("Pantokrator 2: Laser baits", "Away from tanks, first bait");
    private final ModifiableCallout<AbilityCastStart> pantokrator2second = ModifiableCallout.durationBasedCall("Pantokrator 2: Second laser baits", "Second bait");
    private final ModifiableCallout<AbilityCastStart> charybdis = ModifiableCallout.durationBasedCall("Charybdis", "White hole, healer tether soon");

    private final ModifiableCallout<BuffApplied> biohacked = ModifiableCallout.<BuffApplied>durationBasedCall("Biohacked", "Tether on YOU").autoIcon();

    private final ModifiableCallout<HeadMarkerEvent> waveCannonKyrios = new ModifiableCallout<>("Wave Cannon Kyrios", "Laser on you");


    private final XivState state;
    private final StatusEffectRepository buffs;

    public O11S(XivState state, StatusEffectRepository buffs) {
        this.state = state;
        this.buffs = buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.O11S);
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
            case 0x326C -> call = atomicRay;
            case 0x326D -> call = mustardBomb;
            case 0x325A -> call = forceQuit;
            case 0x326B -> call = electricSlide;
            case 0x3268 -> call = larboardSurge;
            case 0x3266 -> call = starboardSurge;
            case 0x36FE -> call = flamethrowerDOT;
            case 0x3701 -> call = loop;
            case 0x326E -> call = charybdis;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @HandleEvents
    public void headMarker(EventContext context, HeadMarkerEvent event) {
        if(!event.getTarget().isThePlayer())
            return;
        int id = (int) event.getMarkerId();
        final ModifiableCallout<HeadMarkerEvent> call;
        switch(id) {
            case 0x16 -> call = waveCannonKyrios;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> flameThrowerSq = SqtTemplates.multiInvocation(35_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x325C),
            this::flamethrower1,
            this::flamethrower2);

    private void flamethrower1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(flameThrower.getModified());
        s.waitMs(4_000);
        s.accept(flameThrowerMove.getModified());
    }

    private void flamethrower2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(flameThrower2.getModified());
        s.waitMs(4_000);
        s.accept(flameThrowerMove.getModified());
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3260));
        s.accept(ballisticImpact.getModified());
        //TODO: get tether event and call separately
        s.waitMs(24_000);
        s.accept(blaster.getModified());
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> LarbStarSq = SqtTemplates.sq(15_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x3264, 0x3262),
            (e1, s) -> {
                log.info("LarbStar: Start");
                boolean leftFirst = false;
                if(e1.abilityIdMatches(0x3264)) {
                    s.accept(larboard.getModified());
                } else {
                    leftFirst = true;
                    s.accept(starboard.getModified());
                }
                AbilityCastStart second = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x3265, 0x3263));
                if(leftFirst && second.abilityIdMatches(0x3265) || !leftFirst && second.abilityIdMatches(0x3263)) {
                    s.accept(LRstay.getModified());
                } else {
                    s.accept(LRswap.getModified());
                }

            });

    @AutoFeed
    private final SequentialTrigger<BaseEvent> peripheralSynthesisSq = SqtTemplates.multiInvocation(20_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x324A),
            this::peripheral1,
            this::peripheral2,
            this::peripheral3,
            this::peripheral4);

    private void peripheral1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(peripheralSynthesis1.getModified());
        List<XivCombatant> rockets;
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x324A));
        //7696|9330 blue
        //7697|9331 yellow
//                do {
//                    rockets = getState().getCombatants().values().stream().filter(cbt -> {
//                        long id = cbt.getbNpcId();
//                        return id == 0x7696 || id == 0x7697;
//                    }).toList();
//                    if(rockets.size() < 6) {
//                        s.refreshCombatants(200);
//                    }
//                    else {
//                        break;
//                    }
//                } while (true);
//                Optional<XivCombatant> rocketNearYou = rockets.stream().filter(cbt ->)
        //TODO: Find some way to figure which rocket is facing the player
        //note: they will spin as the player moves around
    }

    private void peripheral2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(peripheralSynthesis2.getModified());

        s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x6A7) && ba.getTarget().isThePlayer());
        s.accept(biohacked.getModified());
    }

    private void peripheral3(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(peripheralSynthesis2.getModified());

        s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x6A7) && ba.getTarget().isThePlayer());
        s.accept(biohacked.getModified());

        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3250));
        s.accept(sprint.getModified());
    }

    private void peripheral4(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(peripheralSynthesis4.getModified());

        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x324A));
        s.waitMs(3_500);
        s.accept(peripheral4LB.getModified());
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> executable = SqtTemplates.multiInvocation(40_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x3626),
            this::executable1,
            this::executable2);

    private void executable1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        //buff seems to always apply first
        log.info("Executable 1: Start");
        BuffApplied ferroBuff = s.waitEvent(BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0x2BA, 0x2BB));
        TetherEvent ferroTether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(getState().getPlayer()));

        XivCombatant ferroTetherBuddy = ferroTether.getTargetMatching(c -> !c.isThePlayer());
        Optional<BuffApplied> ferroBuddyBuff = getBuffs().statusesOnTarget(ferroTetherBuddy).stream().filter(ba -> ba.buffIdMatches(0x2BA, 0x2BB)).findAny();

        if(ferroBuddyBuff.isPresent()) {
            if(ferroBuff.getBuff().getId() == ferroBuddyBuff.get().getBuff().getId()) {
                s.accept(ferroPush.getModified());
            } else {
                s.accept(ferroPull.getModified());
            }
        } else {
            log.info("Executable 1: Couldnt find ferroBuddyBuff: {}", ferroBuddyBuff);
        }
    }

    //callouts based on Fennek F'ox text guide
    private void executable2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Executable 2: Start");
        s.accept(executable2start.getModified());
        //658 looper, 65B/273 chains of memory
        List<BuffApplied> buffs = s.waitEvents(8, BuffApplied.class, ba -> ba.buffIdMatches(0x658, 0x273, 0x65B)); //unsure if 273 or 65B, probably 65B because its closer to 658

        Optional<BuffApplied> playerDebuff = buffs.stream().filter(ba -> ba.getTarget().isThePlayer()).findAny();

        if(playerDebuff.isPresent()) {
            boolean looper = playerDebuff.get().buffIdMatches(0x658);
            int looperDuration = (int)playerDebuff.get().getInitialDuration().toSeconds();
            if(looper) {
                s.accept(executable2Looper.getModified(Map.of("duration", looperDuration)));
            } else {
                s.accept(executable2Chain.getModified());
            }

            s.waitMs(3_000);
            if(!looper) {
                s.accept(executable2chainFirst.getModified());
            } else if(looper && looperDuration < 15){
                s.accept(executable2looperFirst.getModified());
            }

            log.info("Executable 2: Waiting for first cleanse");
            s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0x658));
            if(looper && looperDuration < 25) {
                s.accept(executable2looperSecond.getModified());
            } else if(!looper) {
                s.accept(executable2chainRotate.getModified());
            }

            log.info("Executable 2: Waiting for second cleanse");
            s.waitMs(200);
            s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0x658));
            if(looper) {
                s.accept(executable2looperThird.getModified());
            } else {
                s.accept(executable2chainRotate.getModified());
            }

            log.info("Executable 2: Waiting for third cleanse");
            s.waitMs(200);
            s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0x658));
            if(!looper) {
                s.accept(executable2chainRotateCCW.getModified());
            } else {
                s.accept(reset.getModified());
            }

            log.info("Executable 2: Waiting for reformat cast");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x3627));
            s.accept(reformat.getModified());
        } else {
            log.info("Executable 2: playerDebuff not found: {}", playerDebuff);
        }
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> deltaAttackSq = SqtTemplates.sq(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x325B),
            (e1, s) -> {
                s.waitMs(5_000);
                s.accept(deltaAttack.getModified());
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x325B));
                s.waitMs(500);
                s.accept(gradualPetrification.getModified());
            });

    @AutoFeed
    private final SequentialTrigger<BaseEvent> pantokratorSq = SqtTemplates.multiInvocation(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x3702),
            this::pantokrator1,
            this::pantokrator2);

    private void pantokrator1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(pantokrator.getModified());
        log.info("Pantokrator 1: Start bait loop");
        for(int i = 1; i <= 4; i++) {
            log.info("Pantokrator 1: Waiting for puddle {}", i);
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x370B));
            s.waitMs(100);
            s.accept(pantokratorMove.getModified(Map.of("number", i)));
        }

        log.info("Pantokrator 1: Waiting for Engage Ballstics Systems");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x370D));
        s.accept(engageBallistic.getModified());

        log.info("Pantokrator 1: Waiting for stack to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3704));
        s.accept(pantokratorBait.getModified());

        log.info("Pantokrator 1: Waiting for first lasers to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3706));
        s.accept(pantokratorBait2.getModified());

        log.info("Pantokrator 1: Waiting for Long Needle Kyrios");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x370C));
        s.accept(longNeedleKyrios.getModified());

        log.info("Pantokrator 1: Waiting for needle to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x370C));
        s.accept(blaster.getModified());
    }

    private void pantokrator2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(pantokrator2.getModified());
        log.info("Pantokrator 2: Starting bait loop");
        for(int i = 1; i <= 10; i++) {
            log.info("Pantokrator 2: Waiting for puddle {}", i);
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x370B));
            s.waitMs(100);
            s.accept(pantokratorMove.getModified(Map.of("number", i)));
        }

        log.info("Pantokrator 2: Waiting for Enrage Ballistics Systems");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x370D));
        s.accept(engageBallistic.getModified());

        log.info("Pantokrator 2: Waiting for stack to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3704));
        if(getState().getPlayer().getJob().isTank()) {
            s.accept(pantokrator2tank.getModified());
        } else {
            s.accept(pantokrator2laser.getModified());
        }

        log.info("Pantokrator 2: Waiting for first lasers to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3706));
        if(!getState().getPlayer().getJob().isTank()) {
            s.accept(pantokrator2second.getModified());
        }

        log.info("Pantokrator 2: Waiting for Long Needle Kyrios");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x370C));
        s.accept(longNeedleKyrios.getModified());
    }
}
