package com.example.ehe_server.service.intf.home;

import com.example.ehe_server.dto.HomeLatestTransactionsResponse;

import java.util.List;

public interface HomeLatestTransactionsInterface {
    List<HomeLatestTransactionsResponse> getLatestTransactions();
}
