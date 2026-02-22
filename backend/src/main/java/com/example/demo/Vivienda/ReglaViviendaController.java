package com.example.demo.Vivienda;

import com.example.demo.Vivienda.DTOs.ReglaViviendaDTO;
import com.example.demo.Vivienda.DTOs.UpdateReglaVivienda;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/viviendas/{viviendaId}/reglas")
public class ReglaViviendaController {

    private final ReglaViviendaService reglaViviendaService;

    public ReglaViviendaController(ReglaViviendaService reglaViviendaService) {
        this.reglaViviendaService = reglaViviendaService;
    }

    @GetMapping
    public ResponseEntity<ReglaViviendaDTO> get(@PathVariable Integer viviendaId) {
        return ResponseEntity.ok(ReglaViviendaDTO.fromEntity(reglaViviendaService.getReglas(viviendaId)));
    }

    @PutMapping
    public ResponseEntity<ReglaViviendaDTO> upsert(
            @PathVariable Integer viviendaId,
            @Valid @RequestBody UpdateReglaVivienda request
    ) {
        ReglaViviendaEntity regla = reglaViviendaService.upsertReglas(
                viviendaId,
                request.permiteMascotas(),
                request.permiteFumadores(),
                request.fiestasPermitidas()
        );

        return ResponseEntity.ok(ReglaViviendaDTO.fromEntity(regla));
    }
}




