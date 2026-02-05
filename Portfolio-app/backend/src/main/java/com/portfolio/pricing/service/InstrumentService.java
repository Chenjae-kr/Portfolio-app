package com.portfolio.pricing.service;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.exception.ErrorCode;
import com.portfolio.common.util.AssetClass;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstrumentService {
    
    private final InstrumentRepository instrumentRepository;
    
    public Instrument getById(String id) {
        return instrumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUMENT_NOT_FOUND));
    }
    
    public List<Instrument> getByIds(List<String> ids) {
        return instrumentRepository.findByIdIn(ids);
    }
    
    public List<Instrument> getAll() {
        return instrumentRepository.findAll();
    }
    
    public Page<Instrument> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return instrumentRepository.findAll(pageable);
        }
        return instrumentRepository.searchByNameOrTicker(keyword, pageable);
    }
    
    public Page<Instrument> searchByAssetClass(String keyword, AssetClass assetClass, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return instrumentRepository.findAll(pageable);
        }
        return instrumentRepository.searchByNameOrTickerAndAssetClass(keyword, assetClass, pageable);
    }
    
    public List<Instrument> getActiveByAssetClass(AssetClass assetClass) {
        return instrumentRepository.findByAssetClassAndStatus(
                assetClass, Instrument.InstrumentStatus.ACTIVE);
    }
    
    @Transactional
    public Instrument create(Instrument instrument) {
        return instrumentRepository.save(instrument);
    }
    
    @Transactional
    public Instrument update(String id, Instrument updates) {
        Instrument instrument = getById(id);
        
        if (updates.getName() != null) instrument.setName(updates.getName());
        if (updates.getTicker() != null) instrument.setTicker(updates.getTicker());
        if (updates.getCurrency() != null) instrument.setCurrency(updates.getCurrency());
        if (updates.getCountry() != null) instrument.setCountry(updates.getCountry());
        if (updates.getSector() != null) instrument.setSector(updates.getSector());
        if (updates.getIndustry() != null) instrument.setIndustry(updates.getIndustry());
        if (updates.getProvider() != null) instrument.setProvider(updates.getProvider());
        if (updates.getExpenseRatio() != null) instrument.setExpenseRatio(updates.getExpenseRatio());
        if (updates.getBenchmarkIndex() != null) instrument.setBenchmarkIndex(updates.getBenchmarkIndex());
        if (updates.getStatus() != null) instrument.setStatus(updates.getStatus());
        
        return instrument;
    }
}
