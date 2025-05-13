package main.java.plinko.model.records;

import java.net.InetAddress;

//Data which identifies an endpoint goes here
//ie. address, port, etc...
public record EndpointDataRec (
    String address,
    int port
) {
}
