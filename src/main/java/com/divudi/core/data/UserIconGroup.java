package com.divudi.core.data;

import com.divudi.core.entity.UserIcon;
import java.io.Serializable;
import java.util.List;

/**
 * Groups a list of user icons under a common label for display on the home
 * page.
 *
 * @author Dr M H B Ariyaratne
 */
public class UserIconGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    private IconGroup iconGroup;
    private String groupName;
    private List<UserIcon> userIcons;

    public UserIconGroup() {
    }

    public UserIconGroup(IconGroup iconGroup, String groupName, List<UserIcon> userIcons) {
        this.iconGroup = iconGroup;
        this.groupName = groupName;
        this.userIcons = userIcons;
    }

    public IconGroup getIconGroup() {
        return iconGroup;
    }

    public void setIconGroup(IconGroup iconGroup) {
        this.iconGroup = iconGroup;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<UserIcon> getUserIcons() {
        return userIcons;
    }

    public void setUserIcons(List<UserIcon> userIcons) {
        this.userIcons = userIcons;
    }
}
