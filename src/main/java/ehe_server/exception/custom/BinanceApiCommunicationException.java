package ehe_server.exception.custom;

public class BinanceApiCommunicationException extends ExternalServiceException {
    public BinanceApiCommunicationException() {
        super("error.message.binanceApiCommunication", "error.logDetail.binanceApiCommunication");
    }
}