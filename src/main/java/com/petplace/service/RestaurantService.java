package com.petplace.service;
import com.petplace.dto.request.*;
import com.petplace.entity.*;
import com.petplace.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor @Transactional
public class RestaurantService {
    private final RestaurantRepository repo;

    public List<Restaurant> findNearby(double lat, double lng, double radius) { return repo.findNearby(lat, lng, radius); }
    public List<Restaurant> search(String keyword) { return repo.findByNameContainingIgnoreCase(keyword); }

    public List<Restaurant> filter(RestaurantFilterRequest req) {
        return repo.findAll().stream()
            .filter(r -> req.getRegion() == null || (r.getRegion() != null && r.getRegion().name().equals(req.getRegion())))
            .filter(r -> req.getHasParking() == null || r.isHasParking() == req.getHasParking())
            .filter(r -> req.getHasRestroom() == null || r.isHasRestroom() == req.getHasRestroom())
            .filter(r -> req.getAllowSmall() == null || r.isAllowSmall() == req.getAllowSmall())
            .filter(r -> req.getAllowMedium() == null || r.isAllowMedium() == req.getAllowMedium())
            .filter(r -> req.getAllowLarge() == null || r.isAllowLarge() == req.getAllowLarge())
            .filter(r -> req.getHasFence() == null || r.isHasFence() == req.getHasFence())
            .filter(r -> req.getHasSnack() == null || r.isHasSnack() == req.getHasSnack())
            .filter(r -> req.getHasIndoor() == null || r.isHasIndoor() == req.getHasIndoor())
            .filter(r -> req.getHasOutdoor() == null || r.isHasOutdoor() == req.getHasOutdoor())
            .collect(Collectors.toList());
    }

    public Restaurant getDetail(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("가게 없음")); }

    public Restaurant register(Long ownerId, RestaurantRequest req) {
        Restaurant r = new Restaurant(); r.setOwner(new User(ownerId)); r.setName(req.getName());
        r.setCategory(req.getCategory()); r.setRegion(req.getRegion()); r.setAddress(req.getAddress());
        r.setLatitude(req.getLatitude()); r.setLongitude(req.getLongitude()); r.setPhone(req.getPhone());
        r.setBusinessNo(req.getBusinessNo()); r.setHasFence(req.isHasFence());
        r.setHasArtificialGrass(req.isHasArtificialGrass()); r.setHasNaturalGrass(req.isHasNaturalGrass());
        r.setHasSnack(req.isHasSnack()); r.setHasParking(req.isHasParking()); r.setHasRestroom(req.isHasRestroom());
        r.setHasIndoor(req.isHasIndoor()); r.setHasOutdoor(req.isHasOutdoor());
        r.setAllowSmall(req.isAllowSmall()); r.setAllowMedium(req.isAllowMedium()); r.setAllowLarge(req.isAllowLarge());
        return repo.save(r);
    }

    public Restaurant update(Long id, RestaurantRequest req) {
        Restaurant r = getDetail(id); r.setName(req.getName()); r.setCategory(req.getCategory());
        r.setRegion(req.getRegion()); r.setAddress(req.getAddress()); r.setHasFence(req.isHasFence());
        r.setHasArtificialGrass(req.isHasArtificialGrass()); r.setHasNaturalGrass(req.isHasNaturalGrass());
        r.setHasSnack(req.isHasSnack()); r.setHasParking(req.isHasParking()); r.setHasRestroom(req.isHasRestroom());
        r.setHasIndoor(req.isHasIndoor()); r.setHasOutdoor(req.isHasOutdoor());
        r.setAllowSmall(req.isAllowSmall()); r.setAllowMedium(req.isAllowMedium()); r.setAllowLarge(req.isAllowLarge());
        return repo.save(r);
    }
}
