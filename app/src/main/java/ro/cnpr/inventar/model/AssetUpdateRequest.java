package ro.cnpr.inventar.model;

import java.math.BigDecimal;

public class AssetUpdateRequest {

    private String type;
    private String nrInventar;
    private String codObiect;
    private String denumireObiect;
    private String caracteristiciObiect;
    private BigDecimal valoareInventar;
    private String gestionarActual;
    private String compartimentGestionar;
    private String gestionarPrecedent;
    private String compartimentPrecedent;
    private String custodie;
    private String compartimentCustodie;
    private String propusCasare;
    private String locatia;
    private String camera;
    private Boolean identified;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNrInventar() {
        return nrInventar;
    }

    public void setNrInventar(String nrInventar) {
        this.nrInventar = nrInventar;
    }

    public String getCodObiect() {
        return codObiect;
    }

    public void setCodObiect(String codObiect) {
        this.codObiect = codObiect;
    }

    public String getDenumireObiect() {
        return denumireObiect;
    }

    public void setDenumireObiect(String denumireObiect) {
        this.denumireObiect = denumireObiect;
    }

    public String getCaracteristiciObiect() {
        return caracteristiciObiect;
    }

    public void setCaracteristiciObiect(String caracteristiciObiect) {
        this.caracteristiciObiect = caracteristiciObiect;
    }

    public BigDecimal getValoareInventar() {
        return valoareInventar;
    }

    public void setValoareInventar(BigDecimal valoareInventar) {
        this.valoareInventar = valoareInventar;
    }

    public String getGestionarActual() {
        return gestionarActual;
    }

    public void setGestionarActual(String gestionarActual) {
        this.gestionarActual = gestionarActual;
    }

    public String getCompartimentGestionar() {
        return compartimentGestionar;
    }

    public void setCompartimentGestionar(String compartimentGestionar) {
        this.compartimentGestionar = compartimentGestionar;
    }

    public String getGestionarPrecedent() {
        return gestionarPrecedent;
    }

    public void setGestionarPrecedent(String gestionarPrecedent) {
        this.gestionarPrecedent = gestionarPrecedent;
    }

    public String getCompartimentPrecedent() {
        return compartimentPrecedent;
    }

    public void setCompartimentPrecedent(String compartimentPrecedent) {
        this.compartimentPrecedent = compartimentPrecedent;
    }

    public String getCustodie() {
        return custodie;
    }

    public void setCustodie(String custodie) {
        this.custodie = custodie;
    }

    public String getCompartimentCustodie() {
        return compartimentCustodie;
    }

    public void setCompartimentCustodie(String compartimentCustodie) {
        this.compartimentCustodie = compartimentCustodie;
    }

    public String getPropusCasare() {
        return propusCasare;
    }

    public void setPropusCasare(String propusCasare) {
        this.propusCasare = propusCasare;
    }

    public String getLocatia() {
        return locatia;
    }

    public void setLocatia(String locatia) {
        this.locatia = locatia;
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public Boolean getIdentified() {
        return identified;
    }

    public void setIdentified(Boolean identified) {
        this.identified = identified;
    }
}
