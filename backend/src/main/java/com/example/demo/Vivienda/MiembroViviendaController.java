package com.example.demo.Vivienda;

import com.example.demo.Vivienda.DTOs.CreateMiembroVivienda;
import com.example.demo.Vivienda.DTOs.MiembroViviendaDTO;
import com.example.demo.Vivienda.DTOs.UpdateMiembroVivienda;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/viviendas/{viviendaId}/miembros")
public class MiembroViviendaController {

    private final MiembroViviendaService miembroViviendaService;

    public MiembroViviendaController(MiembroViviendaService miembroViviendaService) {
        this.miembroViviendaService = miembroViviendaService;
    }

    @GetMapping
    public ResponseEntity<List<MiembroViviendaDTO>> list(@PathVariable Integer viviendaId) {
        return ResponseEntity.ok(
                MiembroViviendaDTO.fromEntityList(miembroViviendaService.listMiembros(viviendaId))
        );
    }

    @PostMapping
    public ResponseEntity<MiembroViviendaDTO> add(
            @PathVariable Integer viviendaId,
            @Valid @RequestBody CreateMiembroVivienda request
    ) {
        MiembroViviendaEntity miembro = miembroViviendaService.addMiembro(
                viviendaId,
                request.usuarioId(),
                request.rol(),
                request.fechaIngreso()
        );

        return new ResponseEntity<>(MiembroViviendaDTO.fromEntity(miembro), HttpStatus.CREATED);
    }

    @PutMapping("/{miembroId}")
    public ResponseEntity<MiembroViviendaDTO> updateRol(
            @PathVariable Integer viviendaId,
            @PathVariable Integer miembroId,
            @Valid @RequestBody UpdateMiembroVivienda request
    ) {
        MiembroViviendaEntity miembro = miembroViviendaService.updateRol(
                viviendaId,
                miembroId,
                request.rol()
        );

        return ResponseEntity.ok(MiembroViviendaDTO.fromEntity(miembro));
    }

    @DeleteMapping("/{miembroId}")
    public ResponseEntity<Void> remove(
            @PathVariable Integer viviendaId,
            @PathVariable Integer miembroId
    ) {
        miembroViviendaService.removeMiembro(viviendaId, miembroId);
        return ResponseEntity.noContent().build();
    }
}




