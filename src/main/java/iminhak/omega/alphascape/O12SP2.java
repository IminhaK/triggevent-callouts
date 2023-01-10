package iminhak.omega.alphascape;

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
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@CalloutRepo(name = "Iminha's O12SP2", duty = KnownDuty.O12S)
public class O12SP2 extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(O12SP2.class);

    private final ModifiableCallout<AbilityCastStart> targetAnalysis = ModifiableCallout.durationBasedCall("Target Analysis", "Buster on {event.target}");
    private final ModifiableCallout<AbilityUsedEvent> savageWaveCannon = new ModifiableCallout<>("Savage Wave Cannon", "Buster laser on {event.target}");
    private final ModifiableCallout<AbilityCastStart> patch1start = ModifiableCallout.durationBasedCall("Patch 1: Start", "Stack south");
    private final ModifiableCallout<AbilityCastStart> diffuseWaveCannonLR = ModifiableCallout.durationBasedCall("Diffuse Wave Cannon: Sides", "Sides, move clockwise");
    private final ModifiableCallout<AbilityCastStart> diffuseWaveCannonFB = ModifiableCallout.durationBasedCall("Diffuse Wave Cannon: Front/Back", "Front/Back");
    private final ModifiableCallout<AbilityUsedEvent> patch1tanksAcross = new ModifiableCallout<>("Patch 1: Tanks accross", "Move across");
    private final ModifiableCallout<AbilityCastStart> oversampledWaveCannonTank = ModifiableCallout.durationBasedCall("Oversampled Wave Cannon: Tank", "Move to blue side");
    private final ModifiableCallout<AbilityCastStart> patch1OSWChealerClose = ModifiableCallout.durationBasedCall("Oversampled Wave Cannon: Local Healer", "Opposite blue, slightly north");
    private final ModifiableCallout<AbilityCastStart> patch1OSWCdpsClose = ModifiableCallout.durationBasedCall("Oversampled Wave Cannon: Local DPS", "Opposite blue, slightly south");
    private final ModifiableCallout<AbilityCastStart> oversampledWaveCannon = ModifiableCallout.durationBasedCall("Oversampled Wave Cannon: Other", "Move opposite of blue");
    private final ModifiableCallout<AbilityCastStart> patch1localBreak = ModifiableCallout.durationBasedCall("Patch 1: Break local", "Move to partner");
    private final ModifiableCallout<AbilityCastStart> patch1remoteBreak = ModifiableCallout.durationBasedCall("Patch 1: Break remote", "Move away");
    private final ModifiableCallout<AbilityCastStart> ionEfflux = ModifiableCallout.durationBasedCall("Ion Efflux", "Heavy raidwide");
    private final ModifiableCallout<AbilityCastStart> archivePeripheral = ModifiableCallout.durationBasedCall("Archive Peripheral", "Find safe spot");
    private final ModifiableCallout<AbilityCastStart> indexAndArchivePeripheral = ModifiableCallout.durationBasedCall("Index and Archive Peripheral", "Stretch tether or spread");

    private final ModifiableCallout<BuffApplied> patch1healerAway = ModifiableCallout.<BuffApplied>durationBasedCall("Patch 1: Healer close tether", "Move north").autoIcon();

    private final XivState state;
    private final StatusEffectRepository buffs;

    public O12SP2(XivState state, StatusEffectRepository buffs) {
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
        boolean tank = getState().getPlayer().getJob().isTank();
        switch (id) {
            case 0x336C -> call = targetAnalysis;
            case 0x3357 -> call = ionEfflux;
            case 0x3358 -> call = archivePeripheral;
            case 0x339A -> call = indexAndArchivePeripheral;
            /*case 0x3364, 0x3365 -> {
                if(tank)
                    call = oversampledWaveCannonTank;
                else
                    return;
            }*/
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @HandleEvents
    public void abilityUsed(EventContext context, AbilityUsedEvent event) {
        int id = (int) event.getAbility().getId();
        final ModifiableCallout<AbilityUsedEvent> call;
        switch (id) {
            //Target Analysis finishes
            case 13164 -> call = savageWaveCannon;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    private static final int localRegression = 0x688; //break when close
    private static final int remoteRegression = 0x689; //break when far
    private static final int lrSafe = 0x3367;
    private static final int fbSafe = 0x3368;

    @AutoFeed
    //Calls according to text guide
    private final SequentialTrigger<BaseEvent> patchSq = SqtTemplates.sq(35_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(13174),
            (e1, s) -> {
                log.info("Patch 1: Start");
                s.accept(patch1start.getModified());
                List<BuffApplied> regressions = s.waitEvents(8, BuffApplied.class, ba -> ba.buffIdMatches(localRegression, remoteRegression));
                Optional<BuffApplied> regressionYou = regressions.stream().filter(ba -> ba.getTarget().isThePlayer()).findAny();
                if(regressionYou.isPresent()) {
                    log.info("Patch 1: Got regressions, on you: {}", regressionYou.get().getInfo().name());
                    s.refreshCombatants(100);
                    boolean local = regressionYou.get().buffIdMatches(localRegression);
                    //dont break yet, move away
                    if(getState().getPlayer().getJob().isHealer() && local) {
                        s.accept(patch1healerAway.getModified(regressionYou.get()));
                    }
                    //0x3367 sides safe
                    log.info("Patch 1: Waiting for diffuse to start");
                    AbilityCastStart diffuseWaveCannon = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(lrSafe, fbSafe));
                    if(diffuseWaveCannon.abilityIdMatches(fbSafe)) {
                        s.accept(diffuseWaveCannonFB.getModified());
                    } else {
                        s.accept(diffuseWaveCannonLR.getModified());
                    }

                    //Diffuse AUE
                    log.info("Patch 1: Waiting for diffuse to finish");
                    s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(lrSafe, fbSafe));

                    //Oversampled Wave Cannon ACS, 0x3365 left, 0x3364 right
                    log.info("Patch 1: waiting for oversampled to start");
                    s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x3364, 0x3365));
                    if(getState().getPlayer().getJob().isTank()) {
                        s.accept(oversampledWaveCannonTank.getModified());
                    } else if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(localRegression))) {
                        //still has close
                        if(getState().getPlayer().getJob().isDps()) {
                            s.accept(patch1OSWCdpsClose.getModified());
                        } else {
                            s.accept(patch1OSWChealerClose.getModified());
                        }
                    } else {
                        s.accept(oversampledWaveCannon.getModified());
                    }

                    //Oversampled AUE
                    log.info("Patch 1: Waiting for oversampled to finish");
                    s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x3364, 0x3365));
                    if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(localRegression)) && getState().getPlayer().getJob().isDps()) {
                        s.accept(patch1localBreak.getModified());
                    }

                    //final magic vuln drops
                    log.info("Patch 1: Waiting for magic vuln to drop");
                    s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0x292));
                    if(!local && getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(remoteRegression)) && getState().getPlayer().getJob().isDps()) {
                        s.accept(patch1remoteBreak.getModified());
                    }

                    log.info("Patch 1: Done.");
                } else {
                    log.info("Patch 1: Couldn't find debuff on player");
                }
            });

    private final ModifiableCallout<AbilityCastStart> archiveAllStart = ModifiableCallout.durationBasedCall("Archive All: Start", "Find hand away from jump");
    private final ModifiableCallout<AbilityCastStart> archiveAllMove = ModifiableCallout.durationBasedCall("Archive All: Move", "Move");
    private final ModifiableCallout<AbilityCastStart> archiveAllSpread = ModifiableCallout.durationBasedCall("Archive All: Spread", "Spread");
    private final ModifiableCallout<AbilityCastStart> archiveAllStackYou = ModifiableCallout.durationBasedCall("Archive All: Stack on you", "Stack on YOU");
    private final ModifiableCallout<AbilityCastStart> archiveAllStack = ModifiableCallout.durationBasedCall("Archive All: Stack", "Stack");

    @AutoFeed
    private final SequentialTrigger<BaseEvent> archiveAll = SqtTemplates.sq(35_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(13149),
            (e1, s) -> {
                s.accept(archiveAllStart.getModified());

                //Floodlight ACS
                s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(13178));
                s.accept(archiveAllMove.getModified());
                //Fire/Spotlight ACSs
                Optional<AbilityCastStart> onYou = s.waitEvents(5, AbilityCastStart.class, acs -> acs.getTarget().isThePlayer()).stream().filter(acs -> acs.getTarget().isThePlayer() && acs.abilityIdMatches(13177, 13179)).findAny();
                if(onYou.isPresent()) {
                    if(onYou.get().abilityIdMatches(13179)) {
                        s.accept(archiveAllStackYou.getModified());
                    } else {
                        s.accept(archiveAllSpread.getModified());
                    }
                } else {
                    s.accept(archiveAllStack.getModified());
                }
            });

    @AutoFeed
    private final SequentialTrigger<BaseEvent> helloWorldSq = SqtTemplates.multiInvocation(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(13166),
            this::helloWorld1,
            this::helloWorld2);

    //Latent Defect - 1670
    private static final int latentData = 1670;
    //Critical Overflow Bug - 1665
    private static final int COB = 1665;
    //Critical Synchronization Bug - 1664
    private static final int CSB = 1664;
    //Written according to https://streamable.com/24iuj
    private void helloWorld1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Hello World 1: Waiting for buffs");
        s.waitEvents(6, BuffApplied.class, ba -> ba.buffIdMatches(latentData, COB, CSB));
        Job playerJob = getState().getPlayer().getJob();
        if(playerJob.isTank()) {
            if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(COB))) {
                hw1Tank1(e1, s);
            } else {
                hw1Tank2(e1, s);
            }
        } else if(playerJob.isHealer()) {
            if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(latentData))) {
                hw1Healer1(e1, s);
            } else {
                hw1Healer2(e1, s);
            }
        } else if(playerJob.isDps()) {
            if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(latentData))) {
                hw1DPS3(e1, s);
            } else if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(CSB) && ba.getInitialDuration().toSeconds() < 10)) {
                hw1DPS1(e1, s);
            } else if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(CSB) && ba.getInitialDuration().toSeconds() > 10)) {
                hw1DPS2(e1, s);
            } else {
                hw1DPS4(e1, s);
            }
        }
    }

    private final ModifiableCallout<AbilityCastStart> hw1tank1first = ModifiableCallout.durationBasedCall("HW1: COB Tank 1", "Move to A");
    private final ModifiableCallout<AbilityCastStart> hw1tank1second = ModifiableCallout.durationBasedCall("HW1: COB Tank 2", "Move to C");

    //Tank with COB
    private void hw1Tank1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw1tank1first.getModified());

        //Their COB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB) && br.getTarget().isThePlayer());
        s.accept(hw1tank1second.getModified());
    }

    private final ModifiableCallout<AbilityCastStart> hw1tank2first = ModifiableCallout.durationBasedCall("HW1: Data Tank 1", "Move to 1");
    private final ModifiableCallout<AbilityCastStart> hw1tank2second = ModifiableCallout.durationBasedCall("HW1: Data Tank 2", "Move to C");
    private final ModifiableCallout<AbilityCastStart> hw1tank2third = ModifiableCallout.durationBasedCall("HW1: Data Tank 3", "Move to A");

    //Tank with Latent Data
    private void hw1Tank2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw1tank2first.getModified());

        //CSB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(CSB) && br.getTarget().isThePlayer());
        s.accept(hw1tank2second.getModified());

        //Critical error AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(13182));
        s.accept(hw1tank2third.getModified());
    }

    private final ModifiableCallout<AbilityCastStart> hw1healer1first = ModifiableCallout.durationBasedCall("HW1: Data Healer 1", "Move towards A");
    private final ModifiableCallout<AbilityCastStart> hw1healer1second = ModifiableCallout.durationBasedCall("HW1: Data Healer 2", "Move to B");
    private final ModifiableCallout<AbilityCastStart> hw1healer1third = ModifiableCallout.durationBasedCall("HW1: Data Healer 3", "Move to C");

    //Healer with Latent Data
    private void hw1Healer1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw1healer1first.getModified());

        //Latent Data removed
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(latentData) && br.getTarget().isThePlayer());
        s.accept(hw1healer1second.getModified());

        //COB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB) && br.getTarget().isThePlayer());
        s.accept(hw1healer1third.getModified());

    }

    private final ModifiableCallout<AbilityCastStart> hw1healer2first = ModifiableCallout.durationBasedCall("HW1: Nothing Healer 1", "Go to C");
    private final ModifiableCallout<AbilityCastStart> hw1healer2second = ModifiableCallout.durationBasedCall("HW1: Nothing Healer 2", "Move towards B");
    private final ModifiableCallout<AbilityCastStart> hw1healer2third = ModifiableCallout.durationBasedCall("HW1: Nothing Healer 3", "Go between A and B");

    //Healer with Nothing
    private void hw1Healer2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw1healer2first.getModified());

        //First COB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB));
        s.accept(hw1healer2second.getModified());

        //Second COB expires
        s.waitMs(200);
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB));
        s.accept(hw1healer2third.getModified());

        //Third COB expires
        s.waitMs(200);
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB));
        s.accept(hw1healer2first.getModified());
    }

    private final ModifiableCallout<AbilityCastStart> hw1dps1first = ModifiableCallout.durationBasedCall("HW1: 8s CSB DPS 1", "Go to 1");
    private final ModifiableCallout<AbilityCastStart> hw1dps1second = ModifiableCallout.durationBasedCall("HW1: 8s CSB DPS 2", "Go to 2");
    private final ModifiableCallout<AbilityCastStart> hw1dps1third = ModifiableCallout.durationBasedCall("HW1: 8s CSB DPS 3", "Go to C");
    private final ModifiableCallout<AbilityCastStart> hw1dps1fourth = ModifiableCallout.durationBasedCall("HW1: 8s CSB DPS 4", "Go to D");

    //DPS with CSB 8s
    private void hw1DPS1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw1dps1first.getModified());

        //CSB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(CSB) && br.getTarget().isThePlayer());
        s.accept(hw1dps1second.getModified());

        //CSB expires again
        s.waitMs(200);
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(CSB));
        s.accept(hw1dps1third.getModified());

        //Critical Error AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(13182));
        s.accept(hw1dps1fourth.getModified());
    }

    private final ModifiableCallout<AbilityCastStart> hw1dps2first = ModifiableCallout.durationBasedCall("HW1: 12s CSB DPS 1", "Go to 2");
    private final ModifiableCallout<AbilityCastStart> hw1dps2second = ModifiableCallout.durationBasedCall("HW1: 12s CSB DPS 2", "Go to C");
    private final ModifiableCallout<AbilityCastStart> hw1dps2third = ModifiableCallout.durationBasedCall("HW1: 12s CSB DPS 3", "Go to B");

    //DPS with CSB 12s
    private void hw1DPS2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw1dps2first.getModified());

        //CSB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(CSB) && br.getTarget().isThePlayer());
        s.accept(hw1dps2second.getModified());

        //Critical Error AUE
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(13182));
        s.accept(hw1dps2third.getModified());
    }

    private final ModifiableCallout<AbilityCastStart> hw1dps3first = ModifiableCallout.durationBasedCall("HW1: Data DPS 1", "Move towards A");
    private final ModifiableCallout<AbilityCastStart> hw1dps3second = ModifiableCallout.durationBasedCall("HW1: Data DPS 2", "Go to D");
    private final ModifiableCallout<AbilityCastStart> hw1dps3third = ModifiableCallout.durationBasedCall("HW1: Data DPS 3", "Go to C");

    //DPS with Latent Data
    private void hw1DPS3(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw1dps3first.getModified());

        //first COB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB));
        s.accept(hw1dps3second.getModified());

        //COB expires
        s.waitMs(200);
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB) && br.getTarget().isThePlayer());
        s.accept(hw1dps3third.getModified());
    }

    private final ModifiableCallout<AbilityCastStart> hw1dps4first = ModifiableCallout.durationBasedCall("HW1: Nothing DPS 1", "Go to C");
    private final ModifiableCallout<AbilityCastStart> hw1dps4second = ModifiableCallout.durationBasedCall("HW1: Nothing DPS 2", "Move near D");
    private final ModifiableCallout<AbilityCastStart> hw1dps4third = ModifiableCallout.durationBasedCall("HW1: Nothing DPS 3", "Go between D and C");
    private final ModifiableCallout<AbilityCastStart> hw1dps4fourth = ModifiableCallout.durationBasedCall("HW1: Nothing DPS 4", "Go to C");


    //DPS with Nothing
    private void hw1DPS4(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw1dps4first.getModified());

        //first COB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB));
        s.accept(hw1dps4second.getModified());

        //Gain COB
        s.waitEvent(BuffApplied.class, br -> br.buffIdMatches(COB) && br.getTarget().isThePlayer());
        s.accept(hw1dps4third.getModified());

        //COB expires
        s.waitMs(200);
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB) && br.getTarget().isThePlayer());
        s.accept(hw1dps4fourth.getModified());
    }

    private final ModifiableCallout<AbilityCastStart> hw2start = ModifiableCallout.durationBasedCall("HW2: Start", "Spread");
    private final ModifiableCallout<AbilityCastStart> hw2starthealer = ModifiableCallout.durationBasedCall("HW2: Start healer", "Stack with healer");

    private static final int underflow = 1666;
    private static final int defect = 1671;

    //written according to https://streamable.com/oxyx2
    private void helloWorld2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        Job playerJob = getState().getPlayer().getJob();
        if(playerJob.isHealer()) {
            s.accept(hw2starthealer.getModified());
        } else {
            s.accept(hw2start.getModified());
        }
        log.info("Hello World 2: Waiting for buffs");
        s.waitEvents(8, BuffApplied.class, ba -> ba.buffIdMatches(latentData, COB, CSB, underflow));

        if(playerJob.isHealer()) {
            if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(latentData))) {
                hw2Healer2(e1, s);
            } else {
                hw2Healer1(e1, s);
            }
        } else {
            if(playerJob.isTank()) {
                if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(latentData))) {
                    hw2Tank2(e1, s);
                } else {
                    hw2Tank1(e1, s);
                }
            } else {
                if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(CSB) && ba.getInitialDuration().toSeconds() < 10)) {
                    hw2DPS1(e1, s);
                } else if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(CSB) && ba.getInitialDuration().toSeconds() > 10)) {
                    hw2DPS2(e1, s);
                } else if(getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(latentData))) {
                    hw2DPS3(e1, s);
                } else {
                    hw2DPS4(e1, s);
                }
            }
        }
    }

    //Tank with COB
    private void hw2Tank1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
    }

    //Tank with Data
    private void hw2Tank2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
    }

    //Healer with Nothing (Just underflow)
    private void hw2Healer1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
    }

    //Healer with Data
    private void hw2Healer2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
    }

    private final ModifiableCallout<AbilityCastStart> hw2DPS1first = ModifiableCallout.durationBasedCall("HW2: 8s CSB DPS 1", "Go to 1");
    private final ModifiableCallout<AbilityCastStart> hw2DPS1second = ModifiableCallout.durationBasedCall("HW2: 8s CSB Dps 2", "Go to 2");
    private final ModifiableCallout<AbilityCastStart> hw2DPS1third = ModifiableCallout.durationBasedCall("HW2: 8s CSB Dps 3", "Take rot at C");
    private final ModifiableCallout<AbilityCastStart> hw2DPS1fourth = ModifiableCallout.durationBasedCall("HW2: 8s CSB Dps 4", "Take east tower");
    private final ModifiableCallout<AbilityCastStart> hw2DPS1fifth = ModifiableCallout.durationBasedCall("HW2: 8s CSB Dps 5", "Go to D");

    //DPS with CSB 8s
    private void hw2DPS1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw2DPS1first.getModified());

        //CSB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(CSB) && br.getTarget().isThePlayer());
        s.accept(hw2DPS1second.getModified());

        //Second COB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB));
        s.accept(hw2DPS1third.getModified());

        //Third COB expires
        s.waitMs(200);
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(COB));
        s.accept(hw2DPS1fourth.getModified());

        //Defect applied
        s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(defect));
        s.accept(hw2DPS1fifth.getModified());
    }

    private final ModifiableCallout<AbilityCastStart> hw2DPS2first = ModifiableCallout.durationBasedCall("HW2: 12s CSB DPS 1", "Go to 2");
    private final ModifiableCallout<AbilityCastStart> hw2DPS2second = ModifiableCallout.durationBasedCall("HW2: 12s CSB DPS 2", "Take rot at C");

    //DPS with CSB 12s
    private void hw2DPS2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        s.accept(hw2DPS2first.getModified());

        //CSB expires
        s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(CSB) && br.getTarget().isThePlayer());
        s.accept(hw2DPS2second.getModified());
    }

    //DPS with Data
    private void hw2DPS3(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
    }

    //DPS with Nothing
    private void hw2DPS4(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
    }

}
