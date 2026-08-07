package com.expo.booth.repository;

import com.expo.booth.dto.ClientDashboardBoothResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClientDashboardBoothMapper {

    List<ClientDashboardBoothResponse> findByClientUserId(Long clientUserId);
}
