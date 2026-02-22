package com.example.demo.Apartment;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Exceptions.ResourceNotFoundException;

@Service
public class ApartmentService {

    private final ApartmentRepository apartmentsRepository;

    public ApartmentService(ApartmentRepository apartmentsRepository) {
        this.apartmentsRepository = apartmentsRepository;
    }

    @Transactional
    public ApartmentEntity save(ApartmentEntity newApartment) {
        return apartmentsRepository.save(newApartment);
    }

    @Transactional(readOnly = true)
    public List<ApartmentEntity> findAll() {
        return apartmentsRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ApartmentEntity findById(Integer id) {
        return apartmentsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found"));
    }

    @Transactional
    public ApartmentEntity update(Integer id, ApartmentEntity apartments) {
        ApartmentEntity existingApartment = findById(id);

        existingApartment.setTitle(apartments.getTitle());
        existingApartment.setDescription(apartments.getDescription());
        existingApartment.setPrice(apartments.getPrice());
        existingApartment.setBills(apartments.getBills());
        existingApartment.setUbication(apartments.getUbication());
        existingApartment.setState(apartments.getState());

        return apartmentsRepository.save(existingApartment);
    }

    @Transactional
    public void deleteById(Integer id) {
        if (!apartmentsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        apartmentsRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ApartmentEntity> search(String ubication, Double minPrice, Double maxPrice, String state) {
        return apartmentsRepository.search(ubication, minPrice, maxPrice, state);
    }

}
