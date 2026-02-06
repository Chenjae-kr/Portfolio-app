package com.portfolio.pricing.service;

import com.portfolio.TestConfig;
import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.util.AssetClass;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.repository.InstrumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class InstrumentServiceTest {

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Test
    @DisplayName("종목 생성 및 조회")
    void createAndGetInstrument() {
        // given
        Instrument instrument = Instrument.builder()
                .name("Apple Inc.")
                .ticker("AAPL")
                .instrumentType(Instrument.InstrumentType.STOCK)
                .assetClass(AssetClass.EQUITY)
                .currency("USD")
                .country("US")
                .sector("Technology")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();

        // when
        Instrument created = instrumentService.create(instrument);

        // then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Apple Inc.");
        assertThat(created.getTicker()).isEqualTo("AAPL");

        // 조회 테스트
        Instrument found = instrumentService.getById(created.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Apple Inc.");
    }

    @Test
    @DisplayName("존재하지 않는 종목 조회 시 예외 발생")
    void getInstrumentNotFound() {
        // when & then
        assertThatThrownBy(() -> instrumentService.getById("non-existent-id"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("종목 검색 - 이름으로")
    void searchByName() {
        // given
        Instrument apple = Instrument.builder()
                .name("Apple Inc.")
                .ticker("AAPL")
                .instrumentType(Instrument.InstrumentType.STOCK)
                .assetClass(AssetClass.EQUITY)
                .currency("USD")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();
        instrumentRepository.save(apple);

        // when
        Page<Instrument> results = instrumentService.search("Apple", PageRequest.of(0, 10));

        // then
        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent().get(0).getName()).contains("Apple");
    }

    @Test
    @DisplayName("자산 클래스별 종목 조회")
    void getByAssetClass() {
        // given
        Instrument stock = Instrument.builder()
                .name("Stock A")
                .ticker("STKA")
                .instrumentType(Instrument.InstrumentType.STOCK)
                .assetClass(AssetClass.EQUITY)
                .currency("KRW")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();

        Instrument bond = Instrument.builder()
                .name("Bond B")
                .ticker("BNDB")
                .instrumentType(Instrument.InstrumentType.BOND)
                .assetClass(AssetClass.BOND)
                .currency("KRW")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();

        instrumentRepository.save(stock);
        instrumentRepository.save(bond);

        // when
        List<Instrument> equities = instrumentService.getActiveByAssetClass(AssetClass.EQUITY);

        // then
        assertThat(equities).hasSize(1);
        assertThat(equities.get(0).getAssetClass()).isEqualTo(AssetClass.EQUITY);
    }

    @Test
    @DisplayName("종목 업데이트")
    void updateInstrument() {
        // given
        Instrument instrument = Instrument.builder()
                .name("Test Corp")
                .ticker("TEST")
                .instrumentType(Instrument.InstrumentType.STOCK)
                .assetClass(AssetClass.EQUITY)
                .currency("KRW")
                .status(Instrument.InstrumentStatus.ACTIVE)
                .build();
        Instrument saved = instrumentRepository.save(instrument);

        // when
        Instrument updates = new Instrument();
        updates.setName("Test Corporation");
        updates.setSector("IT");
        Instrument updated = instrumentService.update(saved.getId(), updates);

        // then
        assertThat(updated.getName()).isEqualTo("Test Corporation");
        assertThat(updated.getSector()).isEqualTo("IT");
        assertThat(updated.getTicker()).isEqualTo("TEST"); // 변경 안 됨
    }
}
