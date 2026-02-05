package com.portfolio.api;

import com.portfolio.TestConfig;
import com.portfolio.common.util.AssetClass;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.repository.InstrumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class InstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @BeforeEach
    void setUp() {
        instrumentRepository.deleteAll();
    }

    @Test
    @DisplayName("종목 검색 API")
    void searchInstruments() throws Exception {
        // given
        Instrument instrument = Instrument.builder()
                .name("삼성전자")
                .ticker("005930")
                .instrumentType(Instrument.InstrumentType.STOCK)
                .assetClass(AssetClass.EQUITY)
                .currency("KRW")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();
        instrumentRepository.save(instrument);

        // when & then
        mockMvc.perform(get("/v1/instruments/search")
                        .param("q", "삼성"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].name", containsString("삼성")));
    }

    @Test
    @DisplayName("종목 상세 조회 API")
    void getInstrumentById() throws Exception {
        // given
        Instrument instrument = Instrument.builder()
                .name("Apple Inc.")
                .ticker("AAPL")
                .instrumentType(Instrument.InstrumentType.STOCK)
                .assetClass(AssetClass.EQUITY)
                .currency("USD")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();
        Instrument saved = instrumentRepository.save(instrument);

        // when & then
        mockMvc.perform(get("/v1/instruments/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.name").value("Apple Inc."))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("자산 클래스별 종목 조회 API")
    void getInstrumentsByAssetClass() throws Exception {
        // given
        Instrument stock = Instrument.builder()
                .name("Stock")
                .ticker("STK")
                .instrumentType(Instrument.InstrumentType.STOCK)
                .assetClass(AssetClass.EQUITY)
                .currency("KRW")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();

        Instrument bond = Instrument.builder()
                .name("Bond")
                .ticker("BND")
                .instrumentType(Instrument.InstrumentType.BOND)
                .assetClass(AssetClass.BOND)
                .currency("KRW")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();

        instrumentRepository.save(stock);
        instrumentRepository.save(bond);

        // when & then
        mockMvc.perform(get("/v1/instruments")
                        .param("assetClass", "EQUITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].assetClass").value("EQUITY"));
    }
}
