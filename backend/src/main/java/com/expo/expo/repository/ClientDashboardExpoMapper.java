package com.expo.expo.repository;

import com.expo.expo.dto.ClientDashboardExpoResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClientDashboardExpoMapper {

    List<ClientDashboardExpoResponse> findByClientUserId(Long clientUserId);
}
