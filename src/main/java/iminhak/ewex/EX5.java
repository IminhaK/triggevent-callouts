package iminhak.ewex;

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
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@CalloutRepo(name = "Rubicante Extreme", duty = KnownDuty.RubicanteEx)
public class EX5 extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(EX5.class);

    private final ModifiableCallout<AbilityCastStart> inferno = ModifiableCallout.durationBasedCall("Inferno", "Raidwide");
    private final ModifiableCallout<AbilityCastStart> infernoWinged = ModifiableCallout.durationBasedCall("Inferno: Spread", "Spread with bleed");
    private final ModifiableCallout<AbilityCastStart> blazingRapture = ModifiableCallout.durationBasedCall("Blazing Rapture", "Heavy raidwide");
    private final ModifiableCallout<AbilityCastStart> shatteringHeat = ModifiableCallout.durationBasedCall("Shattering Heat", "Buster on {event.target}");
    private final ModifiableCallout<AbilityCastStart> scaldingSignal = ModifiableCallout.durationBasedCall("Scalding Signal", "Out, spread");
    private final ModifiableCallout<AbilityCastStart> scaldingRing = ModifiableCallout.durationBasedCall("Scalding Ring", "In, spread");
    private final ModifiableCallout<AbilityCastStart> awayfrom = ModifiableCallout.durationBasedCall("Something", "Away from {clock}");
    private final ModifiableCallout<AbilityCastStart> nextto = ModifiableCallout.durationBasedCall("Something", "Near {clock}");
    private final ModifiableCallout<AbilityCastStart> dualfire = ModifiableCallout.durationBasedCall("Dualfire", "Double buster");
    private final ModifiableCallout<AbilityCastStart> archInferno = ModifiableCallout.durationBasedCall("Arch Inferno", "Healer stacks");
    private final ModifiableCallout<HeadMarkerEvent> limitCutNumber = new ModifiableCallout<>("Limit Cut number", "{number}", 20_000);
    private final ModifiableCallout<AbilityCastStart> radialFlagration = ModifiableCallout.durationBasedCall("Radial Flagration", "Proteans");
    private final ModifiableCallout<TetherEvent> ghastlyWind = new ModifiableCallout<>("Ghastly Wind", "Point tether out");
    private final ModifiableCallout<TetherEvent> shatteringHeatTether = new ModifiableCallout<>("Shattering Heat tether", "Tank tether on YOU");
    private final ModifiableCallout<AbilityCastStart> sweepingImmolationSpread = ModifiableCallout.durationBasedCall("Sweeping Immolation: Spread", "Behind and Spread");
    private final ModifiableCallout<AbilityCastStart> sweepingImmolationStack = ModifiableCallout.durationBasedCall("Sweeping Immolatiom: Stack", "Behind and Stack");

    private final ModifiableCallout<BuffApplied> flamespireOut = new ModifiableCallout<>("Flamespire Brand: Flare", "Flare, out soon");
    private final ModifiableCallout<BuffApplied> flamespireIn = new ModifiableCallout<>("Flamespire Brand: Nothing", "Stack middle soon");
    private final ModifiableCallout<BuffApplied> flamespireSpread = new ModifiableCallout<>("Flamespire Brand: Spread", "Spread");

    public EX5(XivState state, StatusEffectRepository buffs) {
        this.state = state;
        this.buffs = buffs;
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
        return state.dutyIs(KnownDuty.RubicanteEx);
    }

    private Long firstHeadmark;

    public int getHeadmarkerOffset(HeadMarkerEvent event) {
        if (firstHeadmark == null) {
            firstHeadmark = event.getMarkerId();
        }

        return (int) (event.getMarkerId() - firstHeadmark);
    }

    @HandleEvents
    public void reset(EventContext context, PullStartedEvent event) {
        firstHeadmark = null;
    }

    @HandleEvents(order = -50_000)
    public void headmarkSolver(EventContext context, HeadMarkerEvent event) {
        getHeadmarkerOffset(event);
    }

    //7f6d auto
    //8024 -> used event when he starts ordeal
    //80E9 ordeal of purgation
    @HandleEvents
    public void abilityCast(EventContext context, AbilityCastStart event) {
        int id = (int) event.getAbility().getId();
        final ModifiableCallout<AbilityCastStart> call;
        switch (id) {
            case 0x7D2C -> call = inferno;
            case 0x7CF9 -> call = archInferno;
            case 0x7D0F -> call = infernoWinged;
            case 0x7D07 -> call = blazingRapture;
            case 0x7D25 -> call = scaldingRing;
            case 0x7D24 -> call = scaldingSignal;
            case 0x7D2E -> call = dualfire;
            case 0x7D2D -> call = shatteringHeat;
            case 0x7CFE -> call = radialFlagration;
            case 0x7D20 -> call = sweepingImmolationSpread;
            case 0x7D21 -> call = sweepingImmolationStack;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @HandleEvents
    public void headMarker(EventContext context, HeadMarkerEvent event) {
        int offset = getHeadmarkerOffset(event);
        final ModifiableCallout<HeadMarkerEvent> call;
        if(offset >= -263 && offset <= -256 && event.getTarget().isThePlayer()) {
            context.accept(limitCutNumber.getModified(event, Map.of("number", offset + 264)));
        }
    }

    @HandleEvents
    public void tetherEvent(EventContext context, TetherEvent event) {
        int id = (int) event.getId();
        final ModifiableCallout<TetherEvent> call;
        if(!event.eitherTargetMatches(getState().getPlayer()))
            return;
        switch (id) {
            case 0xC0 -> call = ghastlyWind;
            case 0x54 -> call = shatteringHeatTether; //TODO: give refire and call only for tanks
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @AutoFeed
    public SequentialTrigger<BaseEvent> flamespireBrandSq = SqtTemplates.sq(22_000, AbilityCastStart.class,
            ace -> ace.abilityIdMatches(0x7D13),
            (e1, s) -> {
                log.info("Flamespire Brand: Start");
                List<BuffApplied> stack = s.waitEventsQuickSuccession(4, BuffApplied.class, ba -> ba.buffIdMatches(0xD9C), Duration.ofMillis(200));
                if(stack.contains(getState().getPlayer())) {
                    s.accept(flamespireOut.getModified());
                } else {
                    s.accept(flamespireIn.getModified());
                }

                log.info("Flamespire Brand: Waiting for stack to drop");
                s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xD9C)); //ID for stack debuff
                s.accept(flamespireSpread.getModified());
            });

    //Possibilities:
    //Status loop vfx (noticed 21E, 221, 222)
    //circles used 8024 when starting purg, and 8025 when ending
    @AutoFeed
    public SequentialTrigger<BaseEvent> purgatorySq = SqtTemplates.sq(13_000, AbilityCastStart.class,
            ace -> ace.abilityIdMatches(0x80E9),
            (e1, s) -> {
                log.info("Purgatory: Start");

            });
}
