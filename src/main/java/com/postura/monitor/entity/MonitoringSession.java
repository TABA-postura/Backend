package com.postura.monitor.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "monitoring_session")
public class MonitoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
