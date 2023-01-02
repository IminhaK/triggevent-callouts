package iminhak.alphascape;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class O10S extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(O10S.class);

    private final ModifiableCallout<AbilityUsedEvent> verticalFirst = new ModifiableCallout<>("Vertical first", "Cardinals or Corners");
    private final ModifiableCallout<AbilityUsedEvent> horizontalFirst = new ModifiableCallout<>("Horizontal first", "In or out");
    private final ModifiableCallout<AbilityUsedEvent> doubleVertical = new ModifiableCallout<>("Double vertical", "Corners");
    private final ModifiableCallout<AbilityUsedEvent> verticalHorizontal = new ModifiableCallout<>("Vertical then Horizontal", "Cardinals");
    private final ModifiableCallout<AbilityUsedEvent> doubleHorizontal = new ModifiableCallout<>("Double horizontal", "Out");
    private final ModifiableCallout<AbilityUsedEvent> horizontalVertical = new ModifiableCallout<>("Horizontal then Vertical", "In");

    private final ModifiableCallout<AbilityCastStart> akhMorn = ModifiableCallout.durationBasedCall("Akh Morn", "Stack on {event.target}");
    private final ModifiableCallout<AbilityCastStart> tailEnd = ModifiableCallout.durationBasedCall("Tail End", "Buster on {event.target}");
    private final ModifiableCallout<AbilityCastStart> thunderstorm = ModifiableCallout.durationBasedCall("Thunderstorm", "Sprad");
    private final ModifiableCallout<AbilityCastStart> northernCross = ModifiableCallout.durationBasedCall("Northern Cross", "Raidwide, thin ice"); //TODO: add thin ice icon
    private final ModifiableCallout<AbilityCastStart> akhRhai = ModifiableCallout.durationBasedCall("Akh Rhai: Other", "Move soon");
    private final ModifiableCallout<AbilityCastStart> akhRhaiHealer = ModifiableCallout.durationBasedCall("Akh Rhai: Healer", "Bait AOE far");
    private final ModifiableCallout<AbilityCastStart> horridRoar = ModifiableCallout.durationBasedCall("Horrid Roar", "Move");

    private final ModifiableCallout<HeadMarkerEvent> earthshaker = new ModifiableCallout<>("Earthshaker", "Proteans");
    private final ModifiableCallout<HeadMarkerEvent> deathFromAbove = new ModifiableCallout<>("Death from Above", "Aggro ground unit");
    private final ModifiableCallout<HeadMarkerEvent> deathFromBelow = new ModifiableCallout<>("Death from Below", "Aggro flying unit");

    private final XivState state;
    private final StatusEffectRepository buffs;
    private final RepeatSuppressor refire = new RepeatSuppressor(Duration.ofMillis(200));

    public O10S(XivState state, StatusEffectRepository buffs) {
        this.state = state;
        this.buffs = buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.O10S);
    }

    private XivState getState() {
        return this.state;
    }

    private StatusEffectRepository getBuffs() {
        return this.buffs;
    }

    private boolean belowOnYou = false;
    private StockedSpin stockedSpin;

    private enum StockedSpin {
        VERTICAL, //Card or inter, double flip corners
        HORIZONTAL; //In or out, double roll out
    }

    @HandleEvents
    public void reset(EventContext context, DutyCommenceEvent event) {
        stockedSpin = null;
        belowOnYou = false;
    }

    @HandleEvents
    public void abilityCast(EventContext context, AbilityCastStart event) {
        int id = (int) event.getAbility().getId();
        final ModifiableCallout<AbilityCastStart> call;
        switch (id) {
            case 0x31AB -> call = akhMorn;
            case 0x31AA -> call = tailEnd;
            case 0x31B8 -> {
                if(refire.check(event))
                    call = thunderstorm;
                else
                    return;
            }
            case 0x3625 -> call = northernCross;
            case 0x3622 -> {
                if(getState().getPlayer().getJob().isHealer())
                    call = akhRhaiHealer;
                else
                    call = akhRhai;
            }
            case 0x31B9 -> call = horridRoar;
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @HandleEvents
    public void headMarker(EventContext context, HeadMarkerEvent event) {
        int id = (int) event.getMarkerId();
        final ModifiableCallout<HeadMarkerEvent> call;
        if(event.getTarget().isThePlayer()) {
            switch(id) {
                case 0x28 -> call = earthshaker;
                case 0x8E -> call = deathFromAbove;
                case 0x8F -> {
                    call = deathFromBelow;
                    belowOnYou = true;
                }
                default -> {
                    return;
                }
            }
            context.accept(call.getModified(event));
        }
    }

    @HandleEvents
    public void abilityUsed(EventContext context, AbilityUsedEvent event) {
        int id = (int) event.getAbility().getId();
        final ModifiableCallout<AbilityUsedEvent> call;
        switch (id) {
            case 0x31AC -> {
                stockedSpin = StockedSpin.HORIZONTAL;
                call = horizontalFirst;
            }
            case 0x31AD -> {
                stockedSpin = StockedSpin.VERTICAL;
                call = verticalFirst;
            }
            case 0x31AE -> {
                if(stockedSpin == StockedSpin.HORIZONTAL) {
                    call = doubleHorizontal;
                } else {
                    call = verticalHorizontal;
                }
            }
            case 0x3180 -> {
                if(stockedSpin == StockedSpin.VERTICAL) {
                    call = doubleVertical;
                } else {
                    call = horizontalVertical;
                }
            }
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }
}
