package com.expo.recruitment.repository;

import com.expo.recruitment.dto.ClientDashboardRecruitmentResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClientDashboardRecruitmentMapper {

    List<ClientDashboardRecruitmentResponse> findByHostClientId(Long hostClientId);
}
