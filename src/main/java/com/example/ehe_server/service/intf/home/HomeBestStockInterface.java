package com.example.ehe_server.service.intf.home;

import com.example.ehe_server.dto.HomeBestStockResponse;

import java.util.List;

public interface HomeBestStockInterface {
    List<HomeBestStockResponse> getHomeBestStock();
}
