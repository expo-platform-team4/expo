package com.expo.member.repository;

import com.expo.member.dto.ClientDashboardProfileResponse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClientDashboardProfileMapper {

    ClientDashboardProfileResponse findByClientUserId(Long clientUserId);
}
