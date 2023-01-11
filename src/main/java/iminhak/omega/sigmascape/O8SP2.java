package iminhak.omega.sigmascape;

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
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@CalloutRepo(name = "Iminha's O8SP2", duty = KnownDuty.O8S)
public class O8SP2 extends AutoChildEventHandler implements FilteredEventHandler {

    Logger log = LoggerFactory.getLogger(O8SP2.class);

    //General
    private final ModifiableCallout<AbilityCastStart> heartlessAngel = ModifiableCallout.durationBasedCall("Heartless Angel", "White hole");
    private final ModifiableCallout<AbilityCastStart> ultima = ModifiableCallout.durationBasedCall("Ultima", "Raidwide");
    private final ModifiableCallout<AbilityCastStart> hyperdrive = ModifiableCallout.durationBasedCall("Hyperdrive", "Buster on {event.target}");
    private final ModifiableCallout<AbilityCastStart> trine = ModifiableCallout.durationBasedCall("Trine", "Start on third trine"); //TODO: Trine sequential
    private final ModifiableCallout<AbilityCastStart> wingsOfDestructionLeft = ModifiableCallout.durationBasedCall("Wings of Destruction: Left cleave", "Right");
    private final ModifiableCallout<AbilityCastStart> wingsOfDestructionRight = ModifiableCallout.durationBasedCall("Wings of Destruction: Right cleave", "Left");
    private final ModifiableCallout<AbilityCastStart> wingsOfDestructionBuster = ModifiableCallout.durationBasedCall("Wings of Destruction: Buster", "Tanks close and far");
    private final ModifiableCallout<AbilityCastStart> futuresNumbered = ModifiableCallout.durationBasedCall("Futures Numbered", "Stack then behind");
    private final ModifiableCallout<AbilityCastStart> pastsForgotten = ModifiableCallout.durationBasedCall("Pasts Forgotten", "Stack then stay");
    private final ModifiableCallout<AbilityCastStart> ultimateEmbrace = ModifiableCallout.durationBasedCall("Ultimate Embrace", "Shared buster");

    //Celestriad
    private final ModifiableCallout<AbilityCastStart> celestDPS1 = ModifiableCallout.durationBasedCall("Celestriad: DPS spread soon", "Spread soon, out");
    private final ModifiableCallout<AbilityCastStart> celestTH1 = ModifiableCallout.durationBasedCall("Celestriad: T/H stack soon", "Stack soon, out");
    private final ModifiableCallout<AbilityUsedEvent> celestIn = new ModifiableCallout<>("Celestriad: Move in", "In");
    private final ModifiableCallout<AbilityUsedEvent> celestDPS2 = new ModifiableCallout<>("Celestriad: DPS spread", "Spread");

    //Forsaken 1
    private final ModifiableCallout<AbilityCastStart> forsaken1 = ModifiableCallout.durationBasedCall("Forsaken: Start", "Heavy raidwide");
    private final ModifiableCallout<AbilityUsedEvent> forsaken1heal = new ModifiableCallout<>("Forsaken: Heal", "Heal to full");
    private final ModifiableCallout<AbilityCastStart> forsaken1healer = ModifiableCallout.durationBasedCall("Forsaken: Healer tower", "Take center tower");
    private final ModifiableCallout<AbilityCastStart> forsaken1tank = ModifiableCallout.durationBasedCall("Forsaken: Tank no tower", "Stay out of towers");
    private final ModifiableCallout<TetherEvent> forsakenTether = new ModifiableCallout<>("Forsaken: Tether", "Take {opposite} tower");
    private final ModifiableCallout<AbilityUsedEvent> forsakenHealSlightly = new ModifiableCallout<>("Forsaken: Heal slightly", "Heal");
    private final ModifiableCallout<?> forsakenSkullPop = new ModifiableCallout<>("Forsaken: Skull pop", "Pop skull");
    private final ModifiableCallout<AbilityCastStart> forsakenLightOfJudgement = ModifiableCallout.durationBasedCall("Forsaken: Light of Judgement", "Heavy raidwide");

    //Forsaken 2
    private final ModifiableCallout<AbilityCastStart> forsaken2 = ModifiableCallout.durationBasedCall("Forsaken 2: Start", "Heavy raidwide, ranged far");

    //Forsaken 3
    private final ModifiableCallout<AbilityCastStart> forsaken3 = ModifiableCallout.durationBasedCall("Forsaken 3: Start", "Heavy raidwide");
    private final ModifiableCallout<AbilityCastStart> forsaken3DPS = ModifiableCallout.durationBasedCall("Forsaken 3: DPS tower", "Take south tower");
    private final ModifiableCallout<AbilityCastStart> forsaken3Healer = ModifiableCallout.durationBasedCall("Forsaken 3: Healer tower", "Take west tower");
    private final ModifiableCallout<AbilityCastStart> forsaken3Tank = ModifiableCallout.durationBasedCall("Forsaken 3: Tank tower", "Take east tower");
    private final ModifiableCallout<AbilityCastStart> forsaken3Knockback = ModifiableCallout.durationBasedCall("Forsaken 3: Knockback", "Knockback");
    private final ModifiableCallout<AbilityCastStart> forsaken3IndulgentWill = ModifiableCallout.durationBasedCall("Forsaken 3: Indulgent Will", "Go to edge");
    private final ModifiableCallout<AbilityCastStart> forsaken3IdyllicWill = ModifiableCallout.durationBasedCall("Forsaken 3: Idyllic Will", "Sleep, take tower");

    private final XivState state;
    private final StatusEffectRepository buffs;
    private final ArenaPos arenaPos = new ArenaPos(0, 0, 5, 5);

    public O8SP2(XivState state, StatusEffectRepository buffs) {
        this.state = state;
        this.buffs = buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        //return state.dutyIs(KnownDuty.O8S);
        return true;
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
            case 10490 -> call = heartlessAngel;
            case 10513, 11069 -> call = ultima; //11069 is triple raidwide at enrage
            case 10514 -> call = hyperdrive;
            case 10509 -> call = trine;
            case 10494 -> call = wingsOfDestructionLeft;
//            case 0 -> call = wingsOfDestructionRight;
            case 10496 -> call = wingsOfDestructionBuster;
            case 10478 -> call = futuresNumbered;
            case 10481 -> call = pastsForgotten;
            case 10512 -> call = ultimateEmbrace;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> celestriad = SqtTemplates.sq(15_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(10503),
            (e1, s) -> {
                log.info("Celestriad: Start");
                if(getState().getPlayer().getJob().isDps())
                    s.accept(celestDPS1.getModified());
                else
                    s.accept(celestTH1.getModified());

                log.info("Celestriad: Waiting for chariot");
                //Chariot goes off
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(10504));
                s.accept(celestIn.getModified());

                log.info("Celestriad: Waiting for thunder");
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(10506));
                if(getState().getPlayer().getJob().isDps())
                    s.accept(celestDPS2.getModified());
            });

    @AutoFeed
    private final SequentialTrigger<BaseEvent> forsakenSq = SqtTemplates.multiInvocation(65_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(10473),
            this::forsaken1,
            this::forsaken2,
            this::forsaken3,
            this::forsaken3);

    private void forsaken1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Forsaken 1: Start");
        s.accept(forsaken1.getModified());

        //Wait for forsaken to finish casting
        log.info("Forsaken 1: Waiting for forsaken cast to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(10473));
        s.accept(forsaken1heal.getModified());

        //Wait for Heartless Archangel
        log.info("Forsaken 1: Waiting for Heartless Archangel");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(10491));
        if(getState().getPlayer().getJob().isHealer())
            s.accept(forsaken1healer.getModified());
        else if(getState().getPlayer().getJob().isTank())
            s.accept(forsaken1tank.getModified());

        //Wait for tether events
        log.info("Forsaken 1: Waiting for tethers to spawn");
        List<TetherEvent> tethers = s.waitEvents(4, TetherEvent.class, te -> true); //TOOO: get tether ID
        s.refreshCombatants(200);
        Optional<TetherEvent> tetherOnYou = tethers.stream().filter(te -> te.eitherTargetMatches(getState().getPlayer())).findFirst();
        log.info("Forsaken 1: tether ID: {}", tetherOnYou);
        if(tetherOnYou.isPresent()) {
            log.info("Forsaken 1: Found tether");
            XivCombatant skull = tetherOnYou.get().getTargetMatching(cbt -> !cbt.isThePlayer());
            log.info("Skull: {}", skull);
            ArenaSector skullPos = arenaPos.forPosition(skull.getPos());
            ArenaSector oppositeSkull = skullPos == ArenaSector.NORTHWEST || skullPos == ArenaSector.SOUTHWEST ? ArenaSector.EAST : ArenaSector.WEST;
            s.accept(forsakenTether.getModified(Map.of("skull", skullPos, "opposite", oppositeSkull, "same", oppositeSkull.opposite())));
        }

        //Wait for heartless archangel to finish
        log.info("Forsaken 1: Waiting for archangel to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(10491));
        s.waitMs(3_000);
        s.accept(forsakenHealSlightly.getModified());

        //Wait a bit to call skull pop
        s.waitMs(5_000);
        if(getState().getPlayer().getJob().isDps()) {
            s.accept(forsakenSkullPop.getModified());
        }

        log.info("Forsaken 1: Waiting for wings of destruction");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(10496));
//        s.accept(forsakenWingsOfDestructionBuster.getModified()); called with basic event

        log.info("Forsaken 1: Waiting for ultima");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(10513));

        //REPEAT copy paste

        //Wait for Heartless Archangel
        log.info("Forsaken 1: Waiting for Heartless Archangel");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(10491));
        if(getState().getPlayer().getJob().isHealer())
            s.accept(forsaken1healer.getModified());
        else if(getState().getPlayer().getJob().isTank())
            s.accept(forsaken1tank.getModified());

        //Wait for tether events
        log.info("Forsaken 1: Waiting for tethers to spawn");
        List<TetherEvent> tethers2 = s.waitEvents(4, TetherEvent.class, te -> true); //TOOO: get tether ID
        Optional<TetherEvent> tetherOnYou2 = tethers2.stream().filter(te -> te.eitherTargetMatches(getState().getPlayer())).findFirst();
        log.info("Forsaken 1: tether ID: {}", tetherOnYou2);
        if(tetherOnYou2.isPresent()) {
            log.info("Forsaken 1: Found tether");
            XivCombatant skull = tetherOnYou2.get().getTargetMatching(cbt -> !cbt.isThePlayer());
            ArenaSector skullPos = arenaPos.forPosition(skull.getPos());
            ArenaSector oppositeSkull = skullPos == ArenaSector.NORTHWEST || skullPos == ArenaSector.SOUTHWEST ? ArenaSector.EAST : ArenaSector.WEST;
            s.accept(forsakenTether.getModified(Map.of("skull", skullPos, "opposite", oppositeSkull, "same", oppositeSkull.opposite())));
        }

        //Wait for heartless archangel to finish
        log.info("Forsaken 1: Waiting for archangel to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(10491));
        s.waitMs(3_000);
        s.accept(forsakenHealSlightly.getModified());

        //Wait a bit to call skull pop
        s.waitMs(5_000);
        if(getState().getPlayer().getJob().isDps()) {
            s.accept(forsakenSkullPop.getModified());
        }

        //Wait for light of judgement
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(10477));
        s.accept(forsakenLightOfJudgement.getModified());
    }

    //TODO
    private void forsaken2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Forsaken 2: Start");
        s.accept(forsaken2.getModified());

        //Wait for light of judgement
        log.info("Waiting for light of judgemnent");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(10477));
        s.accept(forsakenLightOfJudgement.getModified());
    }

    private void forsaken3(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Forsaken 3: Start");
        s.accept(forsaken3.getModified());

        Job job = getState().getPlayer().getJob();
        log.info("Forsaken 3: Waiting for forsaken cast to finish");
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(10473));
        if(job.isDps())
            s.accept(forsaken3DPS.getModified());
        else if(job.isHealer())
            s.accept(forsaken3Healer.getModified());
        else
            s.accept(forsaken3Tank.getModified());

        log.info("Forsaken 3: Waiting for pulse waves");
        List<AbilityCastStart> pulseWaves = s.waitEvents(4, AbilityCastStart.class, acs -> acs.abilityIdMatches(10461));
        if(pulseWaves.stream().map(AbilityCastStart::getTarget).toList().contains(getState().getPlayer())) {
            s.accept(forsaken3Knockback.getModified());
        }

        log.info("Forsaken 3: Waiting for tank buster");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(10515));
        if(job.isTank())
            s.accept(ultimateEmbrace.getModified());

        log.info("Forsaken 3: Waiting for tethers");
        AbilityCastStart tether = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(10470, 10469));
        if(tether.getAbility().getId() == 10469)
            s.accept(forsaken3IndulgentWill.getModified());
        else
            s.accept(forsaken3IdyllicWill.getModified());

        //TODO: forsaken 1-esque callouts
        //ultima
        //heartless archangel
        //wings of destruction
        //ultima
        //double cleave
        //light of judgement end
    }
}
