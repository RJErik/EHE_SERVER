package ehe_server.service.intf.alert.websocket;

import ehe_server.service.alert.websocket.AlertSubscription;

public interface AlertInitialCheckServiceInterface {

    void performInitialAlertCheckAsync(AlertSubscription subscription);
}