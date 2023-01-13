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
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final ModifiableCallout<BuffApplied> flamespireOutME = new ModifiableCallout<>("Flamespire Brand: Flare + Safety", "Flare, out soon, {safe} safe");
    private final ModifiableCallout<BuffApplied> flamespireInME = new ModifiableCallout<>("Flamespire Brand: Nothing + Safety", "Stack middle soon, {safe} safe");

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
        marksOfPurgatory = null;
        rubicante = null;
        circlesOfHell = null;
        innerCircle = null;
        middleCircle = null;
        outerCircle = null;
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

    //Slots:
    //04 = Flamespire brand indicator
    //04 flags:
    //01000100 = cardinals safe?
    //200020 = intercards safe?
    //00080004 = clear indicator
    //Buffs:
    //D9B flare
    //D9C stack
    //D9D aoe
    @AutoFeed
    public SequentialTrigger<BaseEvent> flamespireBrandSq = SqtTemplates.sq(22_000, AbilityCastStart.class,
            ace -> ace.abilityIdMatches(0x7D13),
            (e1, s) -> {
                log.info("Flamespire Brand: Start");
                List<BuffApplied> stack = s.waitEventsQuickSuccession(4, BuffApplied.class, ba -> ba.buffIdMatches(0xD9B), Duration.ofMillis(200));
                List<MapEffectEvent> me = s.waitEventsUntil(1, MapEffectEvent.class, mee -> mee.getIndex() == 4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7D17));
                if(!me.isEmpty()) {
                    String safe;
                    log.info("Flamespire Brand: me: {} (0x1000100/16777472 should be card safe)", me.get(0).getFlags());
                    if(me.get(0).getFlags() == 0x1000100) {
                        safe = "cardinals";
                    } else {
                        safe = "intercardinals";
                    }
                    log.info("Flamespire Brand: {} safe", safe);

                    if(stack.stream().map(BuffApplied::getTarget).anyMatch(XivCombatant::isThePlayer)) {
                        s.accept(flamespireOutME.getModified(Map.of("safe", safe)));
                    } else {
                        s.accept(flamespireInME.getModified(Map.of("safe", safe)));
                    }
                } else {
                    if(stack.stream().map(BuffApplied::getTarget).anyMatch(XivCombatant::isThePlayer)) {
                        s.accept(flamespireOut.getModified());
                    } else {
                        s.accept(flamespireIn.getModified());
                    }
                }


                log.info("Flamespire Brand: Waiting for stack to drop");
                s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xD9C)); //ID for stack debuff
                s.accept(flamespireSpread.getModified());
            });

    private static boolean ringMapEffect(MapEffectEvent mee) {
        return mee.getIndex() == 1 || mee.getIndex() == 2 || mee.getIndex() == 3;
    }

    private static EX5.Rotation rotationFromMapEffect(MapEffectEvent mee) {
        int flag = (int) mee.getFlags();
        return switch (flag) {
            case 0x00020001 -> Rotation.CLOCKWISE;
            case 0x00200010 -> Rotation.COUNTERCLOCKWISE;
            default -> Rotation.UNKNOWN;
        };
    }

    private static EX5.Ring ringFromMapEffect(MapEffectEvent mee) {
        int index = (int) mee.getIndex();
        return switch (index) {
            case 01 -> Ring.INNER;
            case 02 -> Ring.MID;
            case 03 -> Ring.OUTER;
            default -> Ring.UNKNOWN;
        };
    }

    private static EX5.Ring ringFromCombatant(XivCombatant cbt) {
        int id = (int) cbt.getbNpcId();
        return switch (id) {
            case 15765 -> Ring.INNER;
            case 15766 -> Ring.MID;
            case 15767 -> Ring.OUTER;
            default -> Ring.UNKNOWN;
        };
    }

    //For use with midRingTranslation
    private static int indexOffsetFromCombatant(XivCombatant cbt) {
        ArenaSector facing = ArenaPos.combatantFacing(cbt);
        return switch(facing) {
            case NORTH -> 0;
            case NORTHEAST -> 1;
            case EAST -> 2;
            case SOUTHEAST -> 3;
            case SOUTH -> 4;
            case SOUTHWEST -> 5;
            case WEST -> 6;
            case NORTHWEST -> 7;
            default -> 0;
        };
    }

    private static ArenaSector arenaSectorFromIndex(int index) {
        return ArenaSector.NORTH.plusEighths(index);
    }

    private enum Ring {
        INNER,
        MID,
        OUTER,
        UNKNOWN
    }

    private enum Rotation {
        CLOCKWISE,
        COUNTERCLOCKWISE,
        UNKNOWN
    }

    //index 0 is north, going CW
    //indicates what happens if the fire goes through that direction
    //E/W impossible
    //N, NE, E, SE, S, SW, W, NW
    //-1 makes it move CCW, 1 makes it move CW
    static final int[] midRingTranslation = {0, 0, 0, 1, 0, -1, 0, 0};

    List<XivCombatant> marksOfPurgatory;
    XivCombatant rubicante;
    List<XivCombatant> circlesOfHell;
    XivCombatant innerCircle;
    XivCombatant middleCircle;
    XivCombatant outerCircle;
    //Slots:
    //00 = Arena fiery or not
    //01 = Inner circle
    //02 = Middle ring
    //03 = Outer ring
    //
    //00 flags:
    //00020001 = Fiery
    //00080004 = Not fiery
    //
    //01/02/03 flags:
    //00020001 = Arrows rotating CW
    //00080004 = Clear CW arrows
    //00200010 = Arrows rotating CCW
    //00400004 = Clear CCW arrows
    //NPC IDs:
    //15759 = triangle
    //15760 = square
    //15765 = inner circle of hell
    //15766 = middle circle of hell
    //15767 = outer circle of hell
    @AutoFeed
    public SequentialTrigger<BaseEvent> hopeAbandonYeSq = SqtTemplates.multiInvocation(75_000, AbilityUsedEvent.class,
            aue -> aue.abilityIdMatches(0x7F27), //TODO: have this entire thing happen twice per "hope abandon ye"
            this::hopeAbandonYe1,
            this::hopeAbandonYe2,
            this::hopeAbandonYe3
            );

    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe1Purgation1Triangle = ModifiableCallout.durationBasedCall("HAY 1: Purgation 1 Cone", "{safe1} or {safe2}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe1Purgation1Square = ModifiableCallout.durationBasedCall("HAY 1: Purgation 1 Square", "{safe}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe1Purgation2 = ModifiableCallout.durationBasedCall("HAY 1: Purgation 2", "{safe}");

    public void hopeAbandonYe1(AbilityUsedEvent e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Hope Abandon Ye 1: Start, purgation 1");
        //Map effects are before he starts casting
        List<MapEffectEvent> mapEffects = s.waitEvents(2, MapEffectEvent.class, EX5::ringMapEffect);
        Optional<MapEffectEvent> inner = mapEffects.stream().filter(mee -> mee.getIndex() == 01).findFirst();
        Optional<MapEffectEvent> outer = mapEffects.stream().filter(mee -> mee.getIndex() == 03).findFirst();
        if(inner.isPresent() && outer.isPresent()) {

            //Ordeal of Purgation cast start
            log.info("Hope Abandone Ye 1: Waiting for cast 1 to start");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
            refreshHopeAbandonYeActors(s);

            //First pattern is one line, in direction he is facing
            int rubiFacingIndex = indexOffsetFromCombatant(rubicante);
            int midRotationIndex = indexOffsetFromCombatant(middleCircle);
            int innerSpin = rotationFromMapEffect(inner.get()) == Rotation.CLOCKWISE ? 1 : -1;
            int outerSpin = rotationFromMapEffect(outer.get()) == Rotation.CLOCKWISE ? 1 : -1;
            int flameDestination = Math.floorMod((midRingTranslation[Math.floorMod(midRotationIndex - innerSpin - rubiFacingIndex, 8)]) + rubiFacingIndex + innerSpin, 8);
            log.info("Flame 1 Destination: {} {}", flameDestination, arenaSectorFromIndex(flameDestination));
            //Thanks Sinbad in the ACT discord for this solution
            //inner == outer: rectangle
            //inner != outer: triangle
            if(innerSpin == outerSpin) {
                s.accept(hopeAbandonYe1Purgation1Square.getModified(Map.of(
                        "safe", arenaSectorFromIndex(flameDestination).opposite())));
            } else {
                s.accept(hopeAbandonYe1Purgation1Triangle.getModified(Map.of(
                        "safe1", arenaSectorFromIndex(flameDestination).plusEighths(-1),
                        "safe2", arenaSectorFromIndex(flameDestination).plusEighths(1))));
            }

            //Second Ordeal of Purgation
            s.waitMs(1_000);
            log.info("Hope Abandone Ye 1: Waiting for cast 2 to start");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
            refreshHopeAbandonYeActors(s);
            //This pattern is just behind the perpendicular lines, he always faces the line that is most CCW, so just get 5 rotations from his location
            int flameDestination2 = Math.floorMod(indexOffsetFromCombatant(rubicante) + 5, 8);
            log.info("Flame 2 Safe spot: {} {}", flameDestination2, arenaSectorFromIndex(flameDestination2));
            s.accept(hopeAbandonYe1Purgation2.getModified(Map.of("safe", arenaSectorFromIndex(flameDestination2))));

            //gimmeHopeAbandonYePostMechDebug(s);
        }
    }

    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe2Purgation1 = ModifiableCallout.durationBasedCall("HAY 2: Purgation 1", "{safe}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe2Purgation2 = ModifiableCallout.durationBasedCall("HAY 2: Purgation 2", "Between {safe}");

    private void hopeAbandonYe2(AbilityUsedEvent e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Hope Abandon Ye 2: Start, pattern 1");
        //Map effects before the cast begins
        MapEffectEvent middlemapeffect = s.waitEvent(MapEffectEvent.class, EX5::ringMapEffect);
        log.info("Hope Abandon Ye 2: Waiting for cast 1 to start");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
        refreshHopeAbandonYeActors(s);

        //first pattern is gonna be cw safe from the destination of the ccw most line
        int rubiFacingIndex = indexOffsetFromCombatant(rubicante);
        int midRotationIndex = indexOffsetFromCombatant(middleCircle);
        int midSpin = rotationFromMapEffect(middlemapeffect) == Rotation.CLOCKWISE ? 1 : -1;
        int flameDestination = Math.floorMod(rubiFacingIndex + midRingTranslation[Math.floorMod(midRotationIndex + midSpin - rubiFacingIndex, 8)], 8);
        int flame1point5Destination = Math.floorMod(rubiFacingIndex + 2 + midRingTranslation[Math.floorMod(midRotationIndex + midSpin - (rubiFacingIndex + 2), 8)], 8);
        log.info("Flame 1 destination: {} {}", flameDestination, arenaSectorFromIndex(flameDestination));
        log.info("Flame 1.5 destination: {} {}", flame1point5Destination, arenaSectorFromIndex(flame1point5Destination));
        if(flame1point5Destination - flameDestination == 1) { //triangles right next to eachother, safe 2 cw from it
            s.accept(hopeAbandonYe2Purgation1.getModified(Map.of("safe", arenaSectorFromIndex(flameDestination).plusEighths(2))));
        } else { //split, safe between them
            s.accept(hopeAbandonYe2Purgation1.getModified(Map.of("safe", arenaSectorFromIndex(flameDestination).plusEighths(1))));
        }

        //Second ordeal of purgation
        s.waitMs(1_000);
        log.info("Hope Abandon Ye 2: Waiting for map effect");
        //Map effects before the cast begins
        MapEffectEvent middlemapeffect2 = s.waitEvent(MapEffectEvent.class, EX5::ringMapEffect);
        log.info("Hope Abandon Ye 2: waiting for second cast");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
        refreshHopeAbandonYeActors(s);

        //second pattern is double squares, leaving one eighth safe, mid rotates TODO: investigate, southwest called when it was northwest, east called correctly
        List<ArenaSector> safe = new ArrayList<>(ArenaSector.all);
        int midRotationIndex2 = indexOffsetFromCombatant(middleCircle);
        int midSpin2 = rotationFromMapEffect(middlemapeffect2) == Rotation.CLOCKWISE ? 1 : -1;
        int fuse1Index = Math.floorMod(indexOffsetFromCombatant(rubicante) - 2, 8);
        int fuse1Destination = Math.floorMod(fuse1Index + midRingTranslation[Math.floorMod(midRotationIndex2 + midSpin2 - fuse1Index, 8)], 8);
        ArenaSector fuse1Sector = arenaSectorFromIndex(fuse1Destination);
        int fuse2Index = Math.floorMod(indexOffsetFromCombatant(rubicante) + 2, 8);
        int fuse2Destination = Math.floorMod(fuse2Index + midRingTranslation[Math.floorMod(midRotationIndex2 + midSpin2 - fuse2Index, 8)], 8);
        ArenaSector fuse2Sector = arenaSectorFromIndex(fuse2Destination);
        log.info("Hope Abandon Ye 2 DEBUG: midrotatioindex: {}, midspin: {}, fuse1index: {}, fuse2index: {}", midRotationIndex2, midSpin2, fuse1Index, fuse2Index);
        log.info("Hope Abandon Ye 2: squares are at {} and {}", fuse1Sector, fuse2Sector);

        safe.remove(fuse1Sector);
        safe.remove(fuse1Sector.plusEighths(1));
        safe.remove(fuse1Sector.plusEighths(-1));
        safe.remove(fuse2Sector);
        safe.remove(fuse2Sector.plusEighths(1));
        safe.remove(fuse2Sector.plusEighths(-1));

        s.accept(hopeAbandonYe2Purgation2.getModified(Map.of("safe", safe)));
    }

    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe3Purgation1 = ModifiableCallout.durationBasedCall("HAY 3: Purgation 1", "{safe}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe3Purgation2minus1 = ModifiableCallout.durationBasedCall("HAY 3: Purgation 2 CCW safe", "Counterclockwise from {safe}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe3Purgation2plus3 = ModifiableCallout.durationBasedCall("Hay 3: Purgation 2 CW safe", "Clockwise from {}");

    public void hopeAbandonYe3(AbilityUsedEvent e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Hope Abandon Ye 3: Start, purgation 1");
        List<MapEffectEvent> mapEffects = s.waitEvents(2, MapEffectEvent.class, EX5::ringMapEffect);
        Optional<MapEffectEvent> inner = mapEffects.stream().filter(mee -> mee.getIndex() == 01).findFirst();
        Optional<MapEffectEvent> middle = mapEffects.stream().filter(mee -> mee.getIndex() == 02).findFirst();
        if(inner.isPresent() && middle.isPresent()) {
            //Ordeal of Purgation cast start
            log.info("Hope Abandone Ye 3: Waiting for cast 1 to start");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
            refreshHopeAbandonYeActors(s);

            int rubiFacingIndex = indexOffsetFromCombatant(rubicante);
            int midRotationIndex = indexOffsetFromCombatant(middleCircle);
            int innerSpin = rotationFromMapEffect(inner.get()) == Rotation.CLOCKWISE ? 1 : -1;
            int middleSpin = rotationFromMapEffect(middle.get()) == Rotation.CLOCKWISE ? 1 : -1;
            int flameDestination = Math.floorMod(rubiFacingIndex + innerSpin + midRingTranslation[Math.floorMod(midRotationIndex + middleSpin - rubiFacingIndex - innerSpin, 8)], 8);
            ArenaSector coneSector = arenaSectorFromIndex(flameDestination);
            s.accept(hopeAbandonYe3Purgation1.getModified(Map.of("safe", coneSector.plusEighths(2))));

            log.info("Hope Abandon Ye 3: Waiting for map effects");
            MapEffectEvent mapEffect = s.waitEvent(MapEffectEvent.class, EX5::ringMapEffect);

            log.info("Hope Abandone Ye 3: Waiting for cast 2 to start");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
            refreshHopeAbandonYeActors(s);

            //solution by:
            //https://www.youtube.com/watch?v=TNzz215p_N4&ab_channel=Kaouri
            //find middle translation to the left or right of the perpendicular lines
            // (rubifacing + 1), then check -2 and +2 from here on midRingTranslation (rubifacing then -1 or +3)
            // if its -2, call "ccw of [-2]"
            // if its +2, call "cw of [2]"

            int rubiFacingIndex2 = indexOffsetFromCombatant(rubicante);
            int midRotationIndex2 = indexOffsetFromCombatant(middleCircle);
            int minus1midTranslation = midRingTranslation[midRotationIndex2 - rubiFacingIndex2 - 1];
            int plus3midTranslation = midRingTranslation[midRotationIndex2 - rubiFacingIndex2 + 3];
            ArenaSector rubiFacing = ArenaPos.combatantFacing(rubicante);
            if(minus1midTranslation == 0) {
                s.accept(hopeAbandonYe3Purgation2minus1.getModified(Map.of("safe", rubiFacing.plusEighths(-1))));
            } else if(plus3midTranslation == 0) {
                s.accept(hopeAbandonYe3Purgation2plus3.getModified(Map.of("safe", rubiFacing.plusEighths(3))));
            }
        }
    }

    public void refreshHopeAbandonYeActors(SequentialTriggerController<BaseEvent> s) {
        s.waitMs(500); //he could still be turning
        s.refreshCombatants(100);
        log.info("refreshHopeAbandonYeActors: Starting, finding boss");
        Optional<XivCombatant> maybeRubicante;
        do {
            maybeRubicante = getState().getCombatants().values().stream().filter(cbt -> {
                long id = cbt.getbNpcId();
                return id == 15756;
            }).findFirst();
            if(maybeRubicante.isPresent()) {
                rubicante = maybeRubicante.get();
                break;
            } else {
                s.refreshCombatants(200);
            }
        } while (true);

        log.info("refreshHopeAbandonYeActors: Finding Circles of Purgatory (outside)");
        do {
            marksOfPurgatory = getState().getCombatants().values().stream().filter(cbt -> {
                long id = cbt.getbNpcId();
                return id == 15759 || id == 15760;
            }).toList();
            if(marksOfPurgatory.size() < 16) {
                s.refreshCombatants(200);
            } else {
                break;
            }
        } while (true);

        log.info("refreshHopeAbandonYeActors: Finding floor circles");
        do {
            circlesOfHell = getState().getCombatants().values().stream().filter( cbt -> {
                long id = cbt.getbNpcId();
                return id == 15765 || id == 15766 || id == 15767;
            }).toList();
            if(circlesOfHell.size() < 3) {
                s.refreshCombatants(200);
            } else {
                break;
            }
        } while (true);

        Optional<XivCombatant> maybeInner;
        do {
            maybeInner = circlesOfHell.stream().filter(cbt -> {
                long id = cbt.getbNpcId();
                return id == 15765;
            }).findFirst();
            if(maybeInner.isPresent()) {
                innerCircle = maybeInner.get();
                break;
            }
        } while (true);

        Optional<XivCombatant> maybeMiddle;
        do {
            maybeMiddle = circlesOfHell.stream().filter(cbt -> {
                long id = cbt.getbNpcId();
                return id == 15766;
            }).findFirst();
            if(maybeMiddle.isPresent()) {
                middleCircle = maybeMiddle.get();
                break;
            }
        } while (true);

        Optional<XivCombatant> maybeOuter;
        do {
            maybeOuter = circlesOfHell.stream().filter(cbt -> {
                long id = cbt.getbNpcId();
                return id == 15767;
            }).findFirst();
            if(maybeOuter.isPresent()) {
                outerCircle = maybeOuter.get();
                break;
            }
        } while (true);

        log.info("refreshHopeAbandonYeActors: Done.");
    }

    public void gimmeHopeAbandonYePostMechDebug(SequentialTriggerController<BaseEvent> s) {
        //POST-MECH DEBUG
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x80E9));
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7CEF, 0x7CF0));
        List<XivCombatant> circlesOfHellNEW;
        do {
            circlesOfHellNEW = getState().getCombatants().values().stream().filter( cbt -> {
                long id = cbt.getbNpcId();
                return id == 15765 || id == 15766 || id == 15767;
            }).toList();
            if(circlesOfHellNEW.size() < 3) {
                s.refreshCombatants(200);
            } else {
                break;
            }
        } while (true);
        for (XivCombatant cbt : circlesOfHellNEW) {
            EX5.Ring ring = ringFromCombatant(cbt);
            ArenaSector facing = ArenaPos.combatantFacing(cbt);
            log.info("AND NOW {} is LOOKING: {}", ring, facing);
        }
    }
}
