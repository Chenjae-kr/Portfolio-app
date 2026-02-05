package com.portfolio.api;

import com.portfolio.common.response.ApiResponse;
import com.portfolio.common.util.AssetClass;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.service.InstrumentService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/instruments")
@RequiredArgsConstructor
public class InstrumentController {
    
    private final InstrumentService instrumentService;
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<InstrumentDto>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AssetClass assetClass,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Instrument> instruments;
        
        if (assetClass != null) {
            instruments = instrumentService.searchByAssetClass(q != null ? q : "", assetClass, pageable);
        } else {
            instruments = instrumentService.search(q, pageable);
        }
        
        Page<InstrumentDto> dtoPage = instruments.map(InstrumentDto::from);
        
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("totalElements", dtoPage.getTotalElements());
        meta.put("totalPages", dtoPage.getTotalPages());
        meta.put("currentPage", dtoPage.getNumber());
        meta.put("size", dtoPage.getSize());
        
        return ResponseEntity.ok(new ApiResponse<>(dtoPage, meta, null));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InstrumentDto>> getById(@PathVariable String id) {
        Instrument instrument = instrumentService.getById(id);
        InstrumentDto dto = InstrumentDto.from(instrument);
        
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(new ApiResponse<>(dto, meta, null));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<InstrumentDto>>> getAll(
            @RequestParam(required = false) AssetClass assetClass
    ) {
        List<Instrument> instruments;
        
        if (assetClass != null) {
            instruments = instrumentService.getActiveByAssetClass(assetClass);
        } else {
            instruments = instrumentService.getAll();
        }
        
        List<InstrumentDto> dtos = instruments.stream()
                .map(InstrumentDto::from)
                .toList();
        
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("count", dtos.size());
        
        return ResponseEntity.ok(new ApiResponse<>(dtos, meta, null));
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentDto {
        private String id;
        private String instrumentType;
        private String name;
        private String ticker;
        private String currency;
        private String country;
        private String assetClass;
        private String sector;
        private String industry;
        private String provider;
        private String status;
        
        public static InstrumentDto from(Instrument instrument) {
            return new InstrumentDto(
                    instrument.getId(),
                    instrument.getInstrumentType().name(),
                    instrument.getName(),
                    instrument.getTicker(),
                    instrument.getCurrency(),
                    instrument.getCountry(),
                    instrument.getAssetClass().name(),
                    instrument.getSector(),
                    instrument.getIndustry(),
                    instrument.getProvider(),
                    instrument.getStatus().name()
            );
        }
    }
}
