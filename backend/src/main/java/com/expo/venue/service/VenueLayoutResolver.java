package com.expo.venue.service;

import com.expo.venue.repository.VenueHallRepository;
import com.expo.venue.repository.VenueZoneRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 홀·구역의 배치도 파일 ID 를 조회한다. */
@Component
public class VenueLayoutResolver {

    private final VenueHallRepository venueHallRepository;
    private final VenueZoneRepository venueZoneRepository;

    public VenueLayoutResolver(
            VenueHallRepository venueHallRepository, VenueZoneRepository venueZoneRepository) {
        this.venueHallRepository = venueHallRepository;
        this.venueZoneRepository = venueZoneRepository;
    }

    public Map<Long, Long> hallLayoutFileIdsByHallId(Collection<Long> hallIds) {
        Map<Long, Long> result = new HashMap<>();
        venueHallRepository
                .findAllById(hallIds)
                .forEach(hall -> result.put(hall.getId(), hall.getLayoutFileId()));
        return result;
    }

    public Map<Long, Long> zoneLayoutFileIdsByZoneId(Collection<Long> zoneIds) {
        Map<Long, Long> result = new HashMap<>();
        venueZoneRepository
                .findAllById(zoneIds)
                .forEach(zone -> result.put(zone.getId(), zone.getLayoutFileId()));
        return result;
    }
}
