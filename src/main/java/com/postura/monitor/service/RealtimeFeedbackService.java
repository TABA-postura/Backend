package com.postura.monitor.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeFeedbackService {

    @Transactional
    public void updatePostureCache(Long userId, List<String> postureStates) {}
}
