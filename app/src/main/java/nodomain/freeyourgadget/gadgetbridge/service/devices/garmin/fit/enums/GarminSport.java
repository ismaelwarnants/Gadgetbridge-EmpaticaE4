package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

// Taken from CHANGELOG.fit of a Venu 3,
// Garmin API doc (https://developer.garmin.com/connect-iq/api-docs/Toybox/Activity.html)
// and FIT files
public enum GarminSport {
    GENERIC(0, 0, ActivityKind.ACTIVITY),
    NAVIGATE(0, 50, ActivityKind.NAVIGATE),
    TRACK_ME(0, 51, ActivityKind.TRACK_ME),
    MAP(0, 52, ActivityKind.MAP),
    EXPEDITION(0, 66, ActivityKind.EXPEDITION),
    ADVENTURE_RACE(0, 82, ActivityKind.ADVENTURE_RACE),
    ANCHOR(0, 89, ActivityKind.ANCHOR),
    RUN(1, 0, ActivityKind.RUNNING),
    TREADMILL(1, 1, ActivityKind.TREADMILL),
    TRAIL_RUN(1, 3, ActivityKind.TRAIL_RUN),
    TRACK_RUN(1, 4, ActivityKind.TRACK_RUN),
    INDOOR_TRACK(1, 45, ActivityKind.INDOOR_TRACK),
    VIRTUAL_RUN(1, 58, ActivityKind.VIRTUAL_RUN),
    OBSTACLE_RACING(1, 59, ActivityKind.OBSTACLE_RACE),
    ULTRA_RUN(1, 67, ActivityKind.ULTRA_RUN),
    BIKE(2, 0, ActivityKind.CYCLING),
    BIKE_INDOOR(2, 6, ActivityKind.INDOOR_CYCLING),
    ROAD_BIKE(2, 7, ActivityKind.ROAD_BIKE),
    MTB(2, 8, ActivityKind.MOUNTAIN_BIKE),
    CYCLOCROSS(2, 11, ActivityKind.CYCLO_CROSS),
    HANDCYCLING(2, 12, ActivityKind.HANDCYCLING),
    E_BIKE(2, 28, ActivityKind.E_BIKE),
    BMX(2, 29, ActivityKind.BMX),
    GRAVEL_BIKE(2, 46, ActivityKind.GRAVEL_BIKE),
    E_MTB(2, 47, ActivityKind.E_MOUNTAIN_BIKE),
    BIKE_COMMUTE(2, 48, ActivityKind.BIKE_COMMUTE),
    BIKE_TOUR(2, 49, ActivityKind.BIKE_TOUR),
    HANDCYCLING_INDOOR(2, 88, ActivityKind.HANDCYCLING_INDOOR),
    TRANSITION(3, 0, ActivityKind.TRANSITION),
    FITNESS_EQUIPMENT(4, 0, ActivityKind.FITNESS_EQUIPMENT),
    ELLIPTICAL(4, 15, ActivityKind.ELLIPTICAL_TRAINER),
    STAIR_STEPPER(4, 16, ActivityKind.STAIR_STEPPER),
    PILATES(4, 44, ActivityKind.PILATES),
    SWIMMING(5, 0, ActivityKind.SWIMMING),
    POOL_SWIM(5, 17, ActivityKind.POOL_SWIM),
    OPEN_WATER(5, 18, ActivityKind.SWIMMING_OPENWATER),
    BASKETBALL(6, 0, ActivityKind.BASKETBALL),
    SOCCER(7, 0, ActivityKind.SOCCER),
    TENNIS(8, 0, ActivityKind.TENNIS),
    PLATFORM_TENNIS(8, 93, ActivityKind.PLATFORM_TENNIS),
    TABLE_TENNIS(8, 97, ActivityKind.TABLE_TENNIS),
    AMERICAN_FOOTBALL(9, 0, ActivityKind.AMERICAN_FOOTBALL),
    TRAINING(10, 0, ActivityKind.TRAINING),
    STRENGTH(10, 20, ActivityKind.STRENGTH_TRAINING),
    CARDIO(10, 26, ActivityKind.CARDIO),
    YOGA(10, 43, ActivityKind.YOGA),
    BREATHWORK(10, 62, ActivityKind.BREATHWORK),
    WALK(11, 0, ActivityKind.WALKING),
    WALK_INDOOR(11, 27, ActivityKind.INDOOR_WALKING),
    XC_CLASSIC_SKI(12, 0, ActivityKind.XC_CLASSIC_SKI),
    XC_SKATE_SKI(12, 42, ActivityKind.XC_SKATE_SKI),
    SKI(13, 0, ActivityKind.SKIING),
    BACKCOUNTRY_SKI(13, 37, ActivityKind.BACKCOUNTRY_SKIING),
    SNOWBOARD(14, 0, ActivityKind.SNOWBOARDING),
    BACKCOUNTRY_SNOWBOARD(14, 37, ActivityKind.BACKCOUNTRY_SNOWBOARDING),
    ROWING(15, 0, ActivityKind.ROWING),
    ROW_INDOOR(15, 14, ActivityKind.ROWING_MACHINE),
    MOUNTAINEERING(16, 0, ActivityKind.MOUNTAINEERING),
    HIKE(17, 0, ActivityKind.HIKING),
    MULTISPORT(18, 0, ActivityKind.MULTISPORT),
    PADDLING(19, 0, ActivityKind.PADDLING),
    FLYING(20, 0, ActivityKind.FLYING),
    E_BIKING(21, 0, ActivityKind.E_BIKE),
    MOTORCYCLING(22, 0, ActivityKind.MOTORCYCLING),
    ATV(22, 35, ActivityKind.ATV),
    MOTOCROSS(22, 35, ActivityKind.MOTOCROSS),
    BOATING(23, 0, ActivityKind.BOATING),
    DRIVING(24, 0, ActivityKind.DRIVING),
    GOLF(25, 0, ActivityKind.GOLF),
    HANG_GLIDING(26, 0, ActivityKind.HANG_GLIDING),
    HORSEBACK_RIDING(27,0, ActivityKind.HORSE_RIDING),
    HUNTING(28, 0, ActivityKind.HUNTING),
    FISHING(29, 0, ActivityKind.FISHING),
    INLINE_SKATING(30, 0, ActivityKind.INLINE_SKATING),
    ROCK_CLIMBING(31, 0, ActivityKind.ROCK_CLIMBING),
    CLIMB_INDOOR(31, 68, ActivityKind.CLIMB_INDOOR),
    BOULDERING(31, 69, ActivityKind.BOULDERING),
    SAIL(32, 0, ActivityKind.SAILING),
    SAIL_RACE(32, 65, ActivityKind.SAIL_RACE),
    SAIL_EXPEDITION(32, 66, ActivityKind.SAIL_EXPEDITION),
    ICE_SKATING(33, 0, ActivityKind.ICE_SKATING),
    SKY_DIVING(34, 0, ActivityKind.SKY_DIVING),
    SNOWSHOE(35, 0, ActivityKind.SNOWSHOE),
    SNOWMOBILING(36, 0, ActivityKind.SNOWMOBILING),
    STAND_UP_PADDLEBOARDING(37, 0, ActivityKind.STAND_UP_PADDLEBOARDING),
    SURFING(38, 0, ActivityKind.SURFING),
    WAKEBOARDING(39, 0, ActivityKind.WAKEBOARDING),
    WATER_SKIING(40, 0, ActivityKind.WATER_SKIING),
    KAYAKING(41, 0, ActivityKind.KAYAKING),
    RAFTING(42, 0, ActivityKind.RAFTING),
    WINDSURFING(43, 0, ActivityKind.WINDSURFING),
    KITESURFING(44, 0, ActivityKind.KITESURFING),
    TACTICAL(45, 0, ActivityKind.TACTICAL),
    JUMPMASTER(46, 0, ActivityKind.JUMPMASTER),
    BOXING(47, 0, ActivityKind.BOXING),
    FLOOR_CLIMBING(48, 0, ActivityKind.FLOOR_CLIMBING),
    BASEBALL(49, 0, ActivityKind.BASEBALL),
    SOFTBALL(50, 0, ActivityKind.SOFTBALL),
    SOFTBALL_SLOW_PITCH(51, 0, ActivityKind.SOFTBALL_SLOW_PITCH),
    SINGLE_GAS_DIVING(53, 53, ActivityKind.SCUBA_DIVING),
    MULTI_GAS_DIVING(53, 54, ActivityKind.SCUBA_DIVING),
    GAUGE_DIVING(53, 55, ActivityKind.DIVING),
    APNEA_DIVING(53, 56, ActivityKind.FREE_DIVING),
    APNEA_HUNTING(53, 57, ActivityKind.FREE_DIVING),
    SHOOTING(56, 0, ActivityKind.SHOOTING),
    AUTO_RACING(57, 0, ActivityKind.AUTO_RACING),
    WINTER_SPORT(58, 0, ActivityKind.WINTER_SPORT),
    GRINDING(59, 0, ActivityKind.GRINDING),
    HEALTH_SNAPSHOT(60, 0, ActivityKind.HEALTH_SNAPSHOT),
    MARINE(61, 0, ActivityKind.MARINE),
    HIIT(62, 0, ActivityKind.HIIT),
    VIDEO_GAMING(63, 0, ActivityKind.VIDEO_GAMING),
    GAMING(63, 77, ActivityKind.BOARD_GAME),
    RACKET(64, 0, ActivityKind.RACKET),
    PICKLEBALL(64, 84, ActivityKind.PICKLEBALL),
    PADEL(64, 85, ActivityKind.PADEL),
    SQUASH(64, 94, ActivityKind.SQUASH),
    BADMINTON(64, 95, ActivityKind.BADMINTON),
    RACQUETBALL(64, 96, ActivityKind.RACQUETBALL),
    PUSH_WALK_SPEED(65, 0, ActivityKind.PUSH_WALK_SPEED),
    INDOOR_PUSH_WALK_SPEED(65, 86, ActivityKind.INDOOR_PUSH_WALK_SPEED),
    PUSH_RUN_SPEED(66, 0, ActivityKind.PUSH_RUN_SPEED),
    INDOOR_PUSH_RUN_SPEED(66, 87, ActivityKind.INDOOR_PUSH_RUN_SPEED),
    MEDITATION(67, 0, ActivityKind.MEDITATION),
    PARA_SPORT(68, 0, ActivityKind.PARA_SPORT),
    DISC_GOLF(69, 0, ActivityKind.DISC_GOLF),
    ULTIMATE_DISC(69, 92, ActivityKind.ULTIMATE_DISC),
    TEAM_SPORT(70, 0, ActivityKind.TEAM_SPORT),
    CRICKET(71, 0, ActivityKind.CRICKET),
    RUGBY(72, 0, ActivityKind.RUGBY),
    HOCKEY(73, 0, ActivityKind.HOCKEY),
    ICE_HOCKEY(73, 91, ActivityKind.ICE_HOCKEY),
    FIELD_HOCKEY(73, 90, ActivityKind.HOCKEY),
    LACROSSE(74, 0, ActivityKind.LACROSSE),
    VOLLEYBALL(75, 0, ActivityKind.VOLLEYBALL),
    WATER_TUBING(76, 0, ActivityKind.WATER_TUBING),
    WAKESURFING(77, 0, ActivityKind.WAKESURFING),
    ARCHERY(79, 0, ActivityKind.ARCHERY),
    MIXED_MARTIAL_ARTS(80, 0, ActivityKind.MIXED_MARTIAL_ARTS), // aka MMA
    OVERLAND(81, 98, ActivityKind.OVERLANDING),
    SNORKELING(82, 0, ActivityKind.SNORKELING),
    DANCE(83, 0, ActivityKind.DANCE),
    JUMP_ROPE(84, 0, ActivityKind.JUMP_ROPING),
    TROLLING_MOTOR(99, 0, ActivityKind.TROLLING_MOTOR),
    ;

    private final int type;
    private final int subtype;
    private final ActivityKind activityKind;

    GarminSport(final int type, final int subtype, final ActivityKind activityKind) {
        this.type = type;
        this.subtype = subtype;
        this.activityKind = activityKind;
    }

    public int getType() {
        return type;
    }

    public int getSubtype() {
        return subtype;
    }

    public ActivityKind getActivityKind() {
        return activityKind;
    }

    public static Optional<GarminSport> fromCodes(final int type, final int subtype) {
        for (final GarminSport value : GarminSport.values()) {
            if (value.getType() == type && value.getSubtype() == subtype) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }
}
