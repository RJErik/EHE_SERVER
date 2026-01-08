package ehe_server.exception.custom;

public class SubscriptionNotFoundException extends ResourceNotFoundException {
    public SubscriptionNotFoundException(String subscriptionId) {
        super("error.message.subscriptionNotFound", "error.logDetail.subscriptionNotFound", subscriptionId);
    }
}