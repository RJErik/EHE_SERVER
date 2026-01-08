package ehe_server.service.intf.home;

import ehe_server.dto.HomeStockResponse;

import java.util.List;

public interface HomeWorstStockServiceInterface {
    List<HomeStockResponse> getHomeWorstStock();
}
