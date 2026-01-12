package ehe_server.exception.custom;

public class AlpacaApiCommunicationException extends ExternalServiceException {
    public AlpacaApiCommunicationException() {
        super("error.message.alpacaApiCommunication", "error.logDetail.alpacaApiCommunication");
    }
}