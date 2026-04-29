package com.divudi.bean.inward;

import java.io.Serializable;

public class RoomGanttBar implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String roomName;
    private final String offsetPct;
    private final String widthPct;
    private final boolean wide;
    private final String barColor;

    public RoomGanttBar(String roomName, boolean guardian, boolean active,
                        String offsetPct, String widthPct, boolean wide) {
        this.roomName = roomName;
        this.offsetPct = offsetPct;
        this.widthPct = widthPct;
        this.wide = wide;
        if (guardian) {
            this.barColor = "linear-gradient(90deg,#6f42c1,#a56dff)";
        } else if (active) {
            this.barColor = "linear-gradient(90deg,#198754,#20c997)";
        } else {
            this.barColor = "linear-gradient(90deg,#0d6efd,#6ea8fe)";
        }
    }

    public String getRoomName()  { return roomName; }
    public String getOffsetPct() { return offsetPct; }
    public String getWidthPct()  { return widthPct; }
    public boolean isWide()      { return wide; }
    public String getBarColor()  { return barColor; }
}
