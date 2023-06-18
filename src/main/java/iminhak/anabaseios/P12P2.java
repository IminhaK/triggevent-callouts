package iminhak.anabaseios;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.Job;
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
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@CalloutRepo(name = "Iminha's P12SP2", duty = KnownDuty.P12S)
public class P12P2 extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(P12P2.class);

    public P12P2(XivState state, StatusEffectRepository buffs) {
        this.state = state;
        this.buffs = buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.P12S);
    }

    private XivState state;
    private StatusEffectRepository buffs;

    private XivState getState() {
        return state;
    }

    private StatusEffectRepository getBuffs() {
        return buffs;
    }

    //Caloric 1 calls
    private final ModifiableCallout<?> caloricFireYou = new ModifiableCallout<>("Caloric: Fire Mark YOU", "Fire on YOU").extendedDescription("""
            Callouts assume 2 melee and 2 ranged standard comp
            These callouts are based purely on the Panpan strategy used in Aether party finder. The strat can be found at
            https://ff14.toolboxgaming.space/?id=286982839316861&preview=1""");
    private final ModifiableCallout<?> caloricFireBuddy = new ModifiableCallout<>("Caloric: Fire Mark Buddy", "Fire on Buddy");
    private final ModifiableCallout<?> caloricFireNone = new ModifiableCallout<>("Caloric: No Fire Mark", "Go Mid");

    private final ModifiableCallout<BuffApplied> windDPS = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric: Wind DPS", "Wind, match to fire. Wait for supports").autoIcon();
    private final ModifiableCallout<BuffApplied> fireDPS = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric: Fire DPS", "Fire, match to wind. Wait for supports").autoIcon();
    private final ModifiableCallout<BuffApplied> windSup = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric: Wind Support", "Wind, match to fire.").autoIcon();
    private final ModifiableCallout<BuffApplied> fireSup = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric: Fire Support", "Fire, match to wind.").autoIcon();
    private final ModifiableCallout<BuffApplied> fireOut = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric: Fire already out", "Fire, wait for explosion").autoIcon();
    private final ModifiableCallout<BuffApplied> windOut = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric: Wind already out", "Wind, wait for explosion then move out").autoIcon();

    private final ModifiableCallout<BuffApplied> windMoveOut = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric: Wind move out", "Move out").autoIcon();
    private final ModifiableCallout<BuffApplied> fireStacks2 = ModifiableCallout.<BuffApplied>durationBasedCall("Caloric: Second fire stacks", "East/West fire stacks").autoIcon();

    @AutoFeed
    private final SequentialTrigger<BaseEvent> caloricSq = SqtTemplates.multiInvocation(60_000,
            //Headmarkers appear before cast. begin on crush helm before mechanic
            AbilityCastStart.class, acs -> acs.abilityIdMatches(0x8317),
            //Caloric 1
            (e1, s) -> {
                List<HeadMarkerEvent> flames = s.waitEvents(2, HeadMarkerEvent.class, hm -> hm.getMarkerOffset() == -165);
                List<XivCombatant> initialFlames = List.of(flames.get(0).getTarget(), flames.get(1).getTarget());
                List<Job> flameJobs = getState().getPartyList().stream().filter(initialFlames::contains).map(XivPlayerCharacter::getJob).toList();
                Job playerJob = getState().getPlayerJob();
                log.info("Caloric marked jobs: {}", flameJobs);
                boolean outside = false;
                if(initialFlames.contains(getState().getPlayer())) {
                    s.updateCall(caloricFireYou);
                    outside = true;
                } else if (sameRole(playerJob, flameJobs)) {
                    s.updateCall(caloricFireBuddy);
                    outside = true;
                } else {
                    s.updateCall(caloricFireNone);
                }

                //Cast finishes, buffs go out
                BuffApplied playerDebuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xE06, 0xE07) && ba.getTarget().isThePlayer());
                if(outside) {
                    if(playerDebuff.buffIdMatches(0xE06)) {
                        s.updateCall(fireOut, playerDebuff);
                    } else {
                        s.updateCall(windOut, playerDebuff);
                    }
                } else if(getState().getPlayerJob().isDps()) {
                    if(playerDebuff.buffIdMatches(0xE06)) {
                        s.updateCall(fireDPS, playerDebuff);
                    } else {
                        s.updateCall(windDPS, playerDebuff);
                    }
                } else {
                    if(playerDebuff.buffIdMatches(0xE06)) {
                        s.updateCall(fireSup, playerDebuff);
                    } else {
                        s.updateCall(windSup, playerDebuff);
                    }
                }

                //First stacks
                s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x86FD));
                if(playerDebuff.buffIdMatches(0xE07)) {
                    s.updateCall(windMoveOut, playerDebuff);
                } else {
                    s.updateCall(fireStacks2, playerDebuff);
                }
            },
            //Caloric 2
            (e1, s) -> {

    });

    private boolean sameRole(Job playerJob, List<Job> flameJobs) {
        return flameJobs.stream().anyMatch(j -> (playerJob.isTank() && j.isTank()) ||
                (playerJob.isHealer() && j.isHealer()) ||
                (playerJob.isMeleeDps() && j.isMeleeDps()) ||
                ((playerJob.isPranged() || playerJob.isCaster()) && (j.isPranged() || j.isCaster())));
    }
}
