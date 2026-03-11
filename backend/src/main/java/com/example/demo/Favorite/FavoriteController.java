package com.example.demo.Favorite;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Favorite.DTOs.FavoriteIdsResponseDTO;
import com.example.demo.Favorite.DTOs.FavoriteSummaryDTO;
import com.example.demo.Favorite.DTOs.FavoriteToggleResponseDTO;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<List<FavoriteSummaryDTO>> getCurrentUserFavorites() {
        return ResponseEntity.ok(favoriteService.getCurrentUserFavorites());
    }

    @PutMapping("/{apartmentId}")
    public ResponseEntity<FavoriteToggleResponseDTO> addFavorite(@PathVariable Integer apartmentId) {
        return ResponseEntity.ok(favoriteService.addFavorite(apartmentId));
    }

    @DeleteMapping("/{apartmentId}")
    public ResponseEntity<FavoriteToggleResponseDTO> removeFavorite(@PathVariable Integer apartmentId) {
        return ResponseEntity.ok(favoriteService.removeFavorite(apartmentId));
    }

    @GetMapping("/ids")
    public ResponseEntity<FavoriteIdsResponseDTO> getFavoriteApartmentIds(
            @RequestParam(name = "apartmentIds", required = false) List<Integer> apartmentIds) {
        List<Integer> ids = favoriteService.getFavoriteApartmentIds(apartmentIds);
        return ResponseEntity.ok(new FavoriteIdsResponseDTO(ids));
    }
}
