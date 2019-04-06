package com.sapphire.microphone.exceptions;


import com.sapphire.microphone.model.RoleRequest;

public class SameRoleException extends Exception {
    private final RoleRequest roleRequest;

    public SameRoleException(final RoleRequest roleRequest) {
        super("same role: " + roleRequest.toString());
        this.roleRequest = roleRequest;
    }

    public RoleRequest getRoleRequest() {
        return roleRequest;
    }
}
