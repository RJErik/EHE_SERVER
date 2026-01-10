package ehe_server.exception.custom;

public class InvalidPortfolioName extends ValidationException {
    public InvalidPortfolioName(String PortfolioName) {
        super("error.message.invalidPortfolioNameFormat", "error.logDetail.invalidPortfolioNameFormat", PortfolioName);
    }

  public InvalidPortfolioName() {
    super("error.message.invalidPortfolioNameFormat", "error.logDetail.invalidPortfolioNameFormat");
  }
}