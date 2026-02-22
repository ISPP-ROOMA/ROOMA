package com.example.demo.Vivienda;

import com.example.demo.Vivienda.DTOs.CreateVivienda;
import com.example.demo.Vivienda.DTOs.ViviendaDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/viviendas")
public class ViviendaController {

    private final ViviendaService viviendaService;

    public ViviendaController(ViviendaService viviendaService) {
        this.viviendaService = viviendaService;
    }

    @GetMapping
    public ResponseEntity<List<ViviendaDTO>> getAll() {
        return ResponseEntity.ok(ViviendaDTO.fromEntityList(viviendaService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViviendaDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ViviendaDTO.fromEntity(viviendaService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ViviendaDTO> create(@Valid @RequestBody CreateVivienda request) {
        ViviendaEntity vivienda = new ViviendaEntity();
        vivienda.setTitulo(request.titulo());
        vivienda.setDescripcion(request.descripcion());
        vivienda.setPrecio(request.precio());
        vivienda.setGastos(request.gastos());
        vivienda.setUbicacion(request.ubicacion());
        vivienda.setEstado(request.estado());

        ViviendaEntity created = viviendaService.create(vivienda);
        return new ResponseEntity<>(ViviendaDTO.fromEntity(created), HttpStatus.CREATED);
    }
}




