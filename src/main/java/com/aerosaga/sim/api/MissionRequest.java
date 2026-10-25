package com.aerosaga.sim.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class MissionRequest {

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double dropLat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double dropLng;

    public Double getDropLat() { return dropLat; }
    public void setDropLat(Double dropLat) { this.dropLat = dropLat; }

    public Double getDropLng() { return dropLng; }
    public void setDropLng(Double dropLng) { this.dropLng = dropLng; }
}