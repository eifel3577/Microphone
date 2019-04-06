package com.sapphire.microphone.model;


import android.content.Context;
import com.sapphire.microphone.C;
import com.sapphire.microphone.R;

public class RoleRequest {
    private String remoteRole = C.UNKNOWN;
    private String localRole = C.UNKNOWN;

    public String getRemoteRole() {
        return remoteRole;
    }

    public void setRemoteRole(String remoteRole) {
        this.remoteRole = remoteRole;
    }

    public String getLocalRole() {
        return localRole;
    }

    public void setLocalRole(String localRole) {
        this.localRole = localRole;
    }

    public String getLocalRoleString(final Context context) {
        if (localRole.equals(C.UNKNOWN))
            return getUnknownRoleString(context);
        return context.getString(localRole.equals(C.MIC) ? R.string.MICROPHONE : R.string.CAMERA);
    }

    public String getRemoteRoleString(final Context context) {
        if (remoteRole.equals(C.UNKNOWN))
            return getUnknownRoleString(context);
        return context.getString(remoteRole.equals(C.MIC) ? R.string.MICROPHONE : R.string.CAMERA);
    }

    private String getUnknownRoleString(final Context context) {
        return context.getString(R.string.UNKNOWN);
    }

    public boolean isEqualRoles() {
        return remoteRole.equals(localRole);
    }

    @Override
    public String toString() {
        return "RoleRequest{" +
                "remoteRole='" + remoteRole + '\'' +
                ", localRole='" + localRole + '\'' +
                '}';
    }
}
