package com.mobichord.ftps.service;

public interface IFtpsService  extends ProtocolService{
    default String getType() {
        return "FTPS";
    }
}