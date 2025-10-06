package com.example.ehe_server.service.intf.home;

import com.example.ehe_server.dto.HomeWorstStockResponse;

import java.util.List;

public interface HomeWorstStockInterface {
    List<HomeWorstStockResponse> getHomeWorstStock();
}
