package com.divudi.core.data.inward;

/**
 * The unit a {@code TimedItemFee.durationHours} value is expressed in, i.e. how
 * a timed service (or a room facility charge) turns elapsed time into a billed
 * block count.
 * <p>
 * {@code durationHours} keeps its historical name for database compatibility;
 * with a unit attached it is really "duration value", and the unit says whether
 * that value counts minutes, hours or days. {@link #ONE_TIME} ignores the value
 * entirely and charges the fee exactly once.
 * <p>
 * A null unit on an existing row means {@link #HOUR} — that is what every row
 * created before this enum existed was implicitly using.
 */
public enum TimedItemDurationUnit {

    ONE_TIME,
    MINUTE,
    HOUR,
    DAY;

    /** Minutes in one unit. Meaningless for {@link #ONE_TIME}, which is not time-based. */
    public double getMinutesPerUnit() {
        switch (this) {
            case MINUTE:
                return 1.0;
            case DAY:
                return 60.0 * 24.0;
            case HOUR:
            default:
                return 60.0;
        }
    }

    public String getLabel() {
        switch (this) {
            case ONE_TIME:
                return "One Time Fee";
            case MINUTE:
                return "Per Minute";
            case DAY:
                return "Per Day";
            case HOUR:
            default:
                return "Per Hour";
        }
    }
}
