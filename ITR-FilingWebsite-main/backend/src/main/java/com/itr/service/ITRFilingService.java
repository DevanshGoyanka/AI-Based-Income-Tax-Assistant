package com.itr.service;

import com.itr.dto.FilingRequest;
import com.itr.dto.FilingResponse;
import com.itr.entity.Client;
import com.itr.entity.ITRFiling;
import com.itr.repository.ClientRepository;
import com.itr.repository.ITRFilingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ITRFilingService {

    private final ITRFilingRepository filingRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<FilingResponse> getAllFilings(Long userId, String status, String year) {
        List<ITRFiling> filings;
        
        if (status != null && !status.isEmpty()) {
            filings = filingRepository.findByUserIdAndStatus(userId, status);
        } else {
            filings = filingRepository.findByUserId(userId);
        }
        
        if (year != null && !year.isEmpty()) {
            filings = filings.stream()
                    .filter(f -> f.getAssessmentYear().equals(year))
                    .collect(Collectors.toList());
        }
        
        return filings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FilingResponse createFiling(FilingRequest request, Long userId) {
        Client client = clientRepository.findByIdAndUserId(request.getClientId(), userId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        ITRFiling filing = ITRFiling.builder()
                .client(client)
                .assessmentYear(request.getAssessmentYear())
                .itrType(request.getItrType())
                .status("draft")
                .build();

        filing = filingRepository.save(filing);
        log.info("Created ITR filing {} for client {}", filing.getId(), client.getId());
        
        return toResponse(filing);
    }

    @Transactional
    public FilingResponse updateFiling(Long filingId, FilingRequest request, Long userId) {
        ITRFiling filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Filing not found"));

        if (!filing.getClient().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to filing");
        }

        if (request.getStatus() != null) {
            filing.setStatus(request.getStatus());
        }
        if (request.getAcknowledgementNumber() != null) {
            filing.setAcknowledgementNumber(request.getAcknowledgementNumber());
        }
        if (request.getFilingDate() != null) {
            filing.setFilingDate(request.getFilingDate());
        }

        filing = filingRepository.save(filing);
        return toResponse(filing);
    }

    @Transactional
    public void deleteFiling(Long filingId, Long userId) {
        ITRFiling filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Filing not found"));

        if (!filing.getClient().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to filing");
        }

        filingRepository.delete(filing);
        log.info("Deleted ITR filing {}", filingId);
    }

    private FilingResponse toResponse(ITRFiling filing) {
        return FilingResponse.builder()
                .id(filing.getId())
                .clientId(filing.getClient().getId())
                .clientName(filing.getClient().getName())
                .clientPan(filing.getClient().getPan())
                .assessmentYear(filing.getAssessmentYear())
                .itrType(filing.getItrType())
                .status(filing.getStatus())
                .filingDate(filing.getFilingDate())
                .acknowledgementNumber(filing.getAcknowledgementNumber())
                .totalIncome(filing.getTotalIncome())
                .taxPayable(filing.getTaxPayable())
                .refundAmount(filing.getRefundAmount())
                .createdAt(filing.getCreatedAt())
                .updatedAt(filing.getUpdatedAt())
                .build();
    }
}
