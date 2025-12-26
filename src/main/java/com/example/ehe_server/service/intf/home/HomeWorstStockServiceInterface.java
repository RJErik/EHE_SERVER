package com.example.ehe_server.service.intf.home;

import com.example.ehe_server.dto.HomeStockResponse;

import java.util.List;

public interface HomeWorstStockServiceInterface {
    List<HomeStockResponse> getHomeWorstStock();
}
