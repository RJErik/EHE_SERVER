package ehe_server.service.intf.home;

import ehe_server.dto.HomeLatestTransactionsResponse;

import java.util.List;

public interface HomeLatestTransactionsServiceInterface {
    List<HomeLatestTransactionsResponse> getLatestTransactions();
}
